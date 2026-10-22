package com.runa.smarter.umbrella.tools.messaging;

/** 列出当前调用方可 send_to_agent 的目标节点。 */
public interface AssociatedAgentsPort {

    String listAssociatedAgents(String nodeIdOptional);
}
