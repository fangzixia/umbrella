package com.runa.smarter.umbrella.tools.messaging;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.hook.RuntimeContextAware;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

/** 总控与节点 Harness Agent 共用的 Agent 间消息工具。 */
@Component
public class AgentMessagingTools implements RuntimeContextAware {

    private final AgentBusPort agentBusPort;

    public AgentMessagingTools(AgentBusPort agentBusPort) {
        this.agentBusPort = agentBusPort;
    }

    @Override
    public void setRuntimeContext(RuntimeContext runtimeContext) {
        AgentToolRuntimeBridge.bind(runtimeContext);
    }

    @Tool(
            name = "send_to_agent",
            description = "向指定 nodeId 的常驻节点 Agent 发送 instruction 并等待其回复文本。"
                    + " 请先 list_associated_agents 确认目标在可联系范围内。")
    public String sendToAgent(
            @ToolParam(name = "node_id", description = "目标图谱节点 id") String nodeId,
            @ToolParam(name = "instruction", description = "发给目标 Agent 的任务或问题") String instruction) {
        if (nodeId == null || nodeId.isBlank()) {
            return "未提供 node_id。";
        }
        if (instruction == null || instruction.isBlank()) {
            return "未提供 instruction。";
        }
        return agentBusPort.sendToAgent(nodeId.strip(), instruction.strip());
    }
}
