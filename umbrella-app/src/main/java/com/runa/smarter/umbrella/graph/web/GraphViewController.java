package com.runa.smarter.umbrella.graph.web;

import com.runa.smarter.umbrella.graph.GraphRegistry;
import com.runa.smarter.umbrella.graph.GraphView;
import com.runa.smarter.umbrella.runtime.agent.MasterAgentChatService;
import com.runa.smarter.umbrella.runtime.agent.NodeAgentChatService;
import com.runa.smarter.umbrella.runtime.agent.fleet.AgentFleetLifecycle;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 为前端图谱查看器提供只读数据；能力仅作描述展示，不在此接口执行。 */
@RestController
public class GraphViewController {

    private final GraphRegistry registry;
    private final NodeAgentChatService agents;
    private final MasterAgentChatService masterChat;
    private final AgentFleetLifecycle fleetLifecycle;

    public GraphViewController(
            GraphRegistry registry,
            NodeAgentChatService agents,
            MasterAgentChatService masterChat,
            AgentFleetLifecycle fleetLifecycle) {
        this.registry = registry;
        this.agents = agents;
        this.masterChat = masterChat;
        this.fleetLifecycle = fleetLifecycle;
    }

    @GetMapping("/api/graph/view")
    public Snapshot view(@RequestParam(required = false) String nodeId) {
        GraphView graph = registry.graph();
        int targetCount = registry.nodeCount();
        Boolean nodeAgentReady = null;
        if (nodeId != null && !nodeId.isBlank()) {
            nodeAgentReady = fleetLifecycle.hasNodeAgent(nodeId.strip());
        }
        return new Snapshot(
                graph.name(),
                registry.initialized(),
                agents.available(),
                masterChat.available(),
                fleetLifecycle.isFleetSyncRunning(),
                fleetLifecycle.registeredNodeAgentCount(),
                targetCount,
                fleetLifecycle.isFleetFullySynced(),
                nodeAgentReady,
                graph.nodes().stream().map(node -> new Node(
                        node.id().value(), node.type(), node.displayName(), node.properties().entries(),
                        node.capabilities().stream().map(capability ->
                                new Capability(capability.name(), capability.description())).toList())).toList(),
                graph.edges().stream().map(edge -> new Edge(edge.id().value(), edge.source().value(),
                        edge.target().value(), edge.relationType())).toList());
    }

    /** 图谱快照：名称、初始化状态、对话与节点 Agent 群状态、节点与边列表。 */
    public record Snapshot(
            String name,
            boolean initialized,
            boolean chatAvailable,
            boolean masterChatAvailable,
            boolean fleetSyncRunning,
            int fleetRegisteredCount,
            int fleetTargetCount,
            boolean fleetFullySynced,
            Boolean nodeAgentReady,
            List<Node> nodes,
            List<Edge> edges) {}

    public record Node(String id, String type, String displayName, Map<String, String> properties,
                       List<Capability> capabilities) {}

    public record Capability(String name, String description) {}

    public record Edge(String id, String source, String target, String relationType) {}
}
