package com.runa.smarter.umbrella.runtime.agent;

/** Agent 寻址：总控或图谱节点。 */
public sealed interface AgentAddress permits AgentAddress.Master, AgentAddress.Node {

    String mailboxKey();

    static AgentAddress master() {
        return Master.INSTANCE;
    }

    static AgentAddress node(String nodeId) {
        return new Node(nodeId);
    }

    enum Master implements AgentAddress {
        INSTANCE;

        @Override
        public String mailboxKey() {
            return "MASTER";
        }
    }

    record Node(String nodeId) implements AgentAddress {
        @Override
        public String mailboxKey() {
            return "NODE:" + nodeId;
        }
    }
}
