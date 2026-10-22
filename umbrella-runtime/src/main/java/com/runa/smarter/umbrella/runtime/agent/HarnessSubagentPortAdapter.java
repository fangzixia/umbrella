package com.runa.smarter.umbrella.runtime.agent;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;
import com.runa.smarter.umbrella.graph.GraphRegistry;
import com.runa.smarter.umbrella.graph.GraphView;
import com.runa.smarter.umbrella.runtime.agent.fleet.AgentFleetLifecycle;
import com.runa.smarter.umbrella.runtime.agent.harness.HarnessChatSupport;
import com.runa.smarter.umbrella.runtime.agent.harness.UmbrellaActiveCallRegistry;
import com.runa.smarter.umbrella.runtime.agent.harness.UmbrellaCallContextHolder;
import com.runa.smarter.umbrella.runtime.agent.policy.AgentAssociationPolicy;
import com.runa.smarter.umbrella.tools.messaging.AgentBusPort;
import com.runa.smarter.umbrella.tools.messaging.AgentToolRuntimeBridge;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.hook.RuntimeContextAware;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.ReActAgent;
import java.time.Duration;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class HarnessSubagentPortAdapter implements AgentBusPort, RuntimeContextAware {

    private static final Duration REPLY_TIMEOUT = Duration.ofSeconds(85);

    private final GraphRegistry graphRegistry;
    private final AgentAssociationPolicy associationPolicy;
    private final AgentFleetLifecycle fleetLifecycle;
    private volatile RuntimeContext runtimeContext;

    public HarnessSubagentPortAdapter(
            GraphRegistry graphRegistry,
            AgentAssociationPolicy associationPolicy,
            @Lazy AgentFleetLifecycle fleetLifecycle) {
        this.graphRegistry = graphRegistry;
        this.associationPolicy = associationPolicy;
        this.fleetLifecycle = fleetLifecycle;
    }

    @Override
    public void setRuntimeContext(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
        UmbrellaCallContextHolder.bind(runtimeContext);
    }

    @Override
    public String sendToAgent(String nodeId, String instruction) {
        UmbrellaAgentCallContext callContext = resolveCallContext();
        if (callContext == null) {
            return BusinessErrorMessage.format(
                    "当前不在 Agent 调用上下文中，无法 send_to_agent。",
                    "请由总控或节点 Harness Agent 在处理消息时调用此工具。");
        }
        if (!graphRegistry.initialized()) {
            return BusinessErrorMessage.format("内存图谱尚未初始化。", "请刷新图谱。");
        }
        GraphView graph = graphRegistry.graph();
        AgentAddress from = callContext.from();
        AgentAddress to = AgentAddress.node(nodeId);
        int nextDepth = callContext.depth() + 1;
        if (nextDepth > associationPolicy.maxDepth()) {
            return BusinessErrorMessage.format(
                    "Agent 转发深度超过上限 " + associationPolicy.maxDepth() + "。",
                    "请减少链式 send_to_agent 调用。");
        }
        if (!associationPolicy.allows(from, to, graph)) {
            return associationPolicy.explainDenial(from, to, graph);
        }
        try {
            ReActAgent nodeAgent = fleetLifecycle.ensureNodeAgent(nodeId);
            List<Msg> messages = List.of(new UserMessage(instruction));
            String parentSession = from instanceof AgentAddress.Master
                    ? AgentSessions.MASTER
                    : AgentSessions.forNode(((AgentAddress.Node) from).nodeId());
            RuntimeContext subContext = HarnessChatSupport.runtimeContext(
                    AgentSessions.forNode(nodeId), AgentAddress.node(nodeId), nextDepth);
            String reply;
            try {
                reply = HarnessChatSupport.callText(nodeAgent, nodeId, messages, subContext, REPLY_TIMEOUT)
                        .block(REPLY_TIMEOUT.plusSeconds(5));
            } finally {
                UmbrellaCallContextHolder.bind(HarnessChatSupport.runtimeContext(
                        parentSession, callContext.from(), callContext.depth()));
                AgentToolRuntimeBridge.bind(HarnessChatSupport.runtimeContext(
                        parentSession, callContext.from(), callContext.depth()));
            }
            if (reply == null || reply.isBlank()) {
                return BusinessErrorMessage.format(
                        "目标 Agent 未返回文本。",
                        "请缩短 instruction 或稍后重试。");
            }
            return "[nodeId=" + nodeId + "]\n" + reply;
        } catch (Exception error) {
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            return BusinessErrorMessage.format(
                    "send_to_agent 失败: " + cause.getMessage(),
                    "请检查 nodeId、关联策略与目标 Agent 是否在线。");
        }
    }

    private UmbrellaAgentCallContext resolveCallContext() {
        RuntimeContext masterContext = UmbrellaActiveCallRegistry.contextFor(AgentSessions.MASTER_AGENT_ID);
        if (masterContext == null) {
            masterContext = UmbrellaActiveCallRegistry.contextFor(AgentSessions.MASTER);
        }
        if (masterContext != null) {
            UmbrellaAgentCallContext fromMaster = masterContext.get(UmbrellaAgentCallContext.class);
            return fromMaster != null
                    ? fromMaster
                    : new UmbrellaAgentCallContext(AgentAddress.master(), 0);
        }
        RuntimeContext toolContext = AgentToolRuntimeBridge.current();
        if (toolContext != null) {
            UmbrellaAgentCallContext fromTool = toolContext.get(UmbrellaAgentCallContext.class);
            if (fromTool != null) {
                return fromTool;
            }
        }
        UmbrellaAgentCallContext holder = UmbrellaCallContextHolder.current();
        if (holder != null) {
            return holder;
        }
        if (runtimeContext != null) {
            return runtimeContext.get(UmbrellaAgentCallContext.class);
        }
        if (UmbrellaActiveCallRegistry.hasActiveCalls()) {
            return new UmbrellaAgentCallContext(AgentAddress.master(), 0);
        }
        return null;
    }
}
