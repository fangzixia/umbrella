package com.runa.smarter.umbrella.runtime.agent.fleet;

import com.runa.smarter.umbrella.graph.GraphNode;
import com.runa.smarter.umbrella.graph.GraphRegistry;
import com.runa.smarter.umbrella.graph.GraphView;
import com.runa.smarter.umbrella.graph.model.NodeId;
import com.runa.smarter.umbrella.runtime.agent.AgentSessions;
import com.runa.smarter.umbrella.runtime.agent.harness.GraphHarnessAgentFactory;
import com.runa.smarter.umbrella.runtime.agent.harness.UmbrellaHarnessRuntime;
import com.runa.smarter.umbrella.tools.AgentToolkitFactory;
import io.agentscope.core.ReActAgent;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** 图变更后同步 Harness 总控与全图瘦 ReAct 节点 Agent 群（节点群可后台异步构建）。 */
@Component
public class AgentFleetLifecycle {

    private static final Logger log = LoggerFactory.getLogger(AgentFleetLifecycle.class);
    private static final int PROGRESS_LOG_EVERY = 100;

    private final GraphRegistry graphRegistry;
    private final AgentToolkitFactory toolkitFactory;
    private final ObjectProvider<DashScopeChatModel> models;
    private final UmbrellaHarnessRuntime harnessRuntime;
    private final GraphHarnessAgentFactory agentFactory;

    private final Object syncControl = new Object();
    private volatile HarnessAgent masterAgent;
    private final Map<String, ReActAgent> nodeAgents = new ConcurrentHashMap<>();
    private volatile boolean fleetSyncRunning;
    private volatile boolean fleetSyncPending;

    public AgentFleetLifecycle(
            GraphRegistry graphRegistry,
            AgentToolkitFactory toolkitFactory,
            ObjectProvider<DashScopeChatModel> models,
            UmbrellaHarnessRuntime harnessRuntime,
            GraphHarnessAgentFactory agentFactory) {
        this.graphRegistry = graphRegistry;
        this.toolkitFactory = toolkitFactory;
        this.models = models;
        this.harnessRuntime = harnessRuntime;
        this.agentFactory = agentFactory;
    }

    public synchronized void startMasterIfNeeded() {
        DashScopeChatModel model = models.getIfAvailable();
        if (model == null) {
            log.info("DashScope 未配置，跳过总控 Agent 启动");
            return;
        }
        if (masterAgent != null) {
            return;
        }
        masterAgent = agentFactory.createMaster(model);
        harnessRuntime.registerAgent(AgentSessions.MASTER_AGENT_ID, masterAgent);
        harnessRuntime.gateway().bindMainAgent(masterAgent);
        log.info("总控 Harness Agent 已注册");
    }

    /** 在后台虚拟线程中构建/对齐全图节点 Agent，不阻塞调用方。 */
    public void scheduleFleetSyncWithGraph() {
        synchronized (syncControl) {
            if (fleetSyncRunning) {
                fleetSyncPending = true;
                log.debug("节点 Agent 群同步已在进行，已标记补跑");
                return;
            }
            fleetSyncRunning = true;
        }
        Thread.ofVirtual().name("umbrella-fleet-sync").start(this::runFleetSyncLoop);
    }

    public ReActAgent ensureNodeAgent(String nodeId) {
        DashScopeChatModel model = models.getIfAvailable();
        if (model == null) {
            throw new IllegalStateException("DashScope 未配置");
        }
        if (!graphRegistry.initialized()) {
            throw new IllegalStateException("图谱未初始化");
        }
        GraphView graph = graphRegistry.graph();
        GraphNode node = graph.findNode(NodeId.of(nodeId))
                .orElseThrow(() -> new IllegalArgumentException("节点不存在: " + nodeId));
        return registerNodeAgentIfAbsent(node, graph, model);
    }

    public boolean hasNodeAgent(String nodeId) {
        return nodeAgents.containsKey(nodeId);
    }

    public boolean isFleetSyncRunning() {
        return fleetSyncRunning;
    }

    public int registeredNodeAgentCount() {
        return nodeAgents.size();
    }

    public boolean isFleetFullySynced() {
        return graphRegistry.initialized()
                && !fleetSyncRunning
                && nodeAgents.size() >= graphRegistry.nodeCount();
    }

    public String refreshGraphAndFleet() {
        graphRegistry.refresh();
        scheduleFleetSyncWithGraph();
        return "图谱已刷新: 名称="
                + graphRegistry.graph().name()
                + ", 节点="
                + graphRegistry.nodeCount()
                + ", 边="
                + graphRegistry.edgeCount()
                + ", 节点 Agent 后台同步中 (当前 "
                + nodeAgents.size()
                + "/"
                + graphRegistry.nodeCount()
                + ")";
    }

    public HarnessAgent masterAgent() {
        return masterAgent;
    }

    public boolean masterRunning() {
        return masterAgent != null;
    }

    public UmbrellaHarnessRuntime harnessRuntime() {
        return harnessRuntime;
    }

    private void runFleetSyncLoop() {
        try {
            do {
                fleetSyncPending = false;
                runFleetSyncOnce();
            } while (fleetSyncPending);
        } finally {
            synchronized (syncControl) {
                fleetSyncRunning = false;
            }
        }
    }

    private void runFleetSyncOnce() {
        DashScopeChatModel model = models.getIfAvailable();
        if (model == null) {
            log.info("DashScope 未配置，跳过节点 Agent 群同步");
            return;
        }
        if (!graphRegistry.initialized()) {
            stopAllNodeAgents();
            return;
        }
        GraphView graph = graphRegistry.graph();
        Set<String> activeIds = graph.nodes().stream()
                .map(node -> node.id().value())
                .collect(Collectors.toSet());
        pruneRemovedNodes(activeIds);

        int target = activeIds.size();
        int processed = 0;
        log.info("开始后台同步节点 ReAct Agent 群，目标 count={}", target);
        for (GraphNode node : graph.nodes()) {
            registerNodeAgentIfAbsent(node, graph, model);
            processed++;
            if (processed % PROGRESS_LOG_EVERY == 0 || processed == target) {
                log.info("节点 ReAct Agent 同步进度: {}/{}", nodeAgents.size(), target);
            }
        }
        log.info("节点 ReAct Agent 群同步完成: count={}", nodeAgents.size());
    }

    private ReActAgent registerNodeAgentIfAbsent(GraphNode node, GraphView graph, DashScopeChatModel model) {
        String nodeId = node.id().value();
        return nodeAgents.computeIfAbsent(nodeId, id -> {
            AgentToolkitFactory.NodeAgentToolkit nodeToolkit = toolkitFactory.createNodeFleetToolkit(node);
            return agentFactory.createNode(node, graph, model, nodeToolkit);
        });
    }

    private void pruneRemovedNodes(Set<String> activeIds) {
        nodeAgents.keySet().removeIf(nodeId -> {
            if (!activeIds.contains(nodeId)) {
                stopNodeAgent(nodeId);
                return true;
            }
            return false;
        });
    }

    private void stopAllNodeAgents() {
        nodeAgents.keySet().forEach(this::stopNodeAgent);
        nodeAgents.clear();
    }

    private void stopNodeAgent(String nodeId) {
        ReActAgent agent = nodeAgents.remove(nodeId);
        if (agent != null) {
            agent.close();
        }
    }
}
