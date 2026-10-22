package com.runa.smarter.umbrella.runtime.agent.policy;

import com.runa.smarter.umbrella.graph.GraphView;
import com.runa.smarter.umbrella.runtime.agent.AgentAddress;
import java.util.List;

/** 判定 {@code from} 是否可向 {@code to} 发送 Agent 消息，并列出可关联节点。 */
public interface AgentAssociationPolicy {

    int maxDepth();

    boolean allows(AgentAddress from, AgentAddress to, GraphView graph);

    String explainDenial(AgentAddress from, AgentAddress to, GraphView graph);

    List<AssociatedAgent> listAssociated(AgentAddress from, GraphView graph);
}
