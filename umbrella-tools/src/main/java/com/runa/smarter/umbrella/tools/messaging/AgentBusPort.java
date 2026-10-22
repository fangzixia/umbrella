package com.runa.smarter.umbrella.tools.messaging;

/** 由 runtime 实现：从当前 Worker 上下文向目标节点 Agent 发送 request-reply。 */
public interface AgentBusPort {

    String sendToAgent(String nodeId, String instruction);
}
