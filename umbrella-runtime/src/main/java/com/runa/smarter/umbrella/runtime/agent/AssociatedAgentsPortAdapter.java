package com.runa.smarter.umbrella.runtime.agent;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;
import com.runa.smarter.umbrella.graph.GraphRegistry;
import com.runa.smarter.umbrella.runtime.agent.harness.UmbrellaCallContextHolder;
import com.runa.smarter.umbrella.runtime.agent.policy.AgentAssociationPolicy;
import com.runa.smarter.umbrella.runtime.agent.policy.AssociatedAgent;
import com.runa.smarter.umbrella.tools.messaging.AgentToolRuntimeBridge;
import com.runa.smarter.umbrella.tools.messaging.AssociatedAgentsPort;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.hook.RuntimeContextAware;
import org.springframework.stereotype.Component;

@Component
public class AssociatedAgentsPortAdapter implements AssociatedAgentsPort, RuntimeContextAware {

    private final GraphRegistry graphRegistry;
    private final AgentAssociationPolicy associationPolicy;
    private volatile RuntimeContext runtimeContext;

    public AssociatedAgentsPortAdapter(GraphRegistry graphRegistry, AgentAssociationPolicy associationPolicy) {
        this.graphRegistry = graphRegistry;
        this.associationPolicy = associationPolicy;
    }

    @Override
    public void setRuntimeContext(RuntimeContext runtimeContext) {
        this.runtimeContext = runtimeContext;
        UmbrellaCallContextHolder.bind(runtimeContext);
    }

    @Override
    public String listAssociatedAgents(String nodeIdOptional) {
        if (!graphRegistry.initialized()) {
            return BusinessErrorMessage.format(
                    "内存图谱尚未初始化。",
                    "请配置 integration.graph.url 或调用 refresh_graph。");
        }
        AgentAddress from = resolveFrom(nodeIdOptional);
        if (from == null) {
            return BusinessErrorMessage.format(
                    "无法确定当前调用方身份。",
                    "请在 Agent 处理消息时使用；节点可传入 node_id。");
        }
        var associated = associationPolicy.listAssociated(from, graphRegistry.graph());
        if (associated.isEmpty()) {
            return "当前身份 " + from.mailboxKey() + " 无可联系的节点 Agent。";
        }
        StringBuilder builder = new StringBuilder("调用方: ")
                .append(from.mailboxKey())
                .append("\n可 send_to_agent 的目标:\n");
        for (AssociatedAgent agent : associated) {
            builder.append("- ")
                    .append(agent.nodeId())
                    .append(" | ")
                    .append(agent.displayName())
                    .append(" | ")
                    .append(agent.reason())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    private AgentAddress resolveFrom(String nodeIdOptional) {
        RuntimeContext toolContext = AgentToolRuntimeBridge.current();
        UmbrellaAgentCallContext callContext = toolContext != null
                ? toolContext.get(UmbrellaAgentCallContext.class)
                : null;
        if (callContext == null) {
            callContext = UmbrellaCallContextHolder.current();
        }
        if (callContext == null && runtimeContext != null) {
            callContext = runtimeContext.get(UmbrellaAgentCallContext.class);
        }
        if (callContext != null) {
            return callContext.from();
        }
        if (nodeIdOptional != null && !nodeIdOptional.isBlank()) {
            return AgentAddress.node(nodeIdOptional.strip());
        }
        return null;
    }
}
