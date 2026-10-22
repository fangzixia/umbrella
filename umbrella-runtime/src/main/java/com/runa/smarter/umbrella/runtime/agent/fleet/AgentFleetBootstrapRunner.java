package com.runa.smarter.umbrella.runtime.agent.fleet;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** 应用就绪后启动总控 Harness Agent，并后台同步全图节点 Agent 群。 */
@Component
@Order(Integer.MAX_VALUE)
public class AgentFleetBootstrapRunner implements ApplicationRunner {

    private final AgentFleetLifecycle fleetLifecycle;

    public AgentFleetBootstrapRunner(AgentFleetLifecycle fleetLifecycle) {
        this.fleetLifecycle = fleetLifecycle;
    }

    @Override
    public void run(ApplicationArguments args) {
        fleetLifecycle.startMasterIfNeeded();
        fleetLifecycle.scheduleFleetSyncWithGraph();
    }
}
