package com.runa.smarter.umbrella.runtime.agent;

/** 写入 {@link io.agentscope.core.agent.RuntimeContext}，供跨 Agent 工具识别调用方与转发深度。 */
public record UmbrellaAgentCallContext(AgentAddress from, int depth) {

    public UmbrellaAgentCallContext nextHop() {
        return new UmbrellaAgentCallContext(from, depth + 1);
    }
}
