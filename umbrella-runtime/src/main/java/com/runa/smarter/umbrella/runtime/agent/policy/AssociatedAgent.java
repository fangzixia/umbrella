package com.runa.smarter.umbrella.runtime.agent.policy;

/** 可关联 Agent 摘要，供工具与策略返回。 */
public record AssociatedAgent(String nodeId, String displayName, String reason) {
}
