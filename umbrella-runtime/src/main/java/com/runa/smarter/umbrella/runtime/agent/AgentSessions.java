package com.runa.smarter.umbrella.runtime.agent;

/** Harness {@code RuntimeContext.sessionId} 约定。 */
public final class AgentSessions {

    public static final String MASTER = "umbrella-master";
    public static final String MASTER_AGENT_ID = "umbrella-master";

    private AgentSessions() {
    }

    public static String forNode(String nodeId) {
        return "node-" + nodeId;
    }
}
