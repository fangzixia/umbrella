package com.runa.smarter.umbrella.runtime.agent.harness;

import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.gateway.HarnessGateway;
import org.springframework.stereotype.Component;

/** 进程内共享 Harness Gateway 与会话状态存储。 */
@Component
public class UmbrellaHarnessRuntime {

    private final AgentStateStore stateStore = new InMemoryAgentStateStore();
    private final HarnessGateway gateway = HarnessGateway.create();

    public AgentStateStore stateStore() {
        return stateStore;
    }

    public HarnessGateway gateway() {
        return gateway;
    }

    public void registerAgent(String agentId, HarnessAgent agent) {
        gateway.registerAgent(agentId, agent);
    }

    public HarnessAgent findAgent(String agentId) {
        return gateway.findAgent(agentId);
    }
}
