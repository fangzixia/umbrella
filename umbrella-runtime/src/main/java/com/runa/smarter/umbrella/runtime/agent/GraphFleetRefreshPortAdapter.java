package com.runa.smarter.umbrella.runtime.agent;

import com.runa.smarter.umbrella.runtime.agent.fleet.AgentFleetLifecycle;
import com.runa.smarter.umbrella.tools.messaging.GraphFleetRefreshPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class GraphFleetRefreshPortAdapter implements GraphFleetRefreshPort {

    private final AgentFleetLifecycle fleetLifecycle;

    public GraphFleetRefreshPortAdapter(@Lazy AgentFleetLifecycle fleetLifecycle) {
        this.fleetLifecycle = fleetLifecycle;
    }

    @Override
    public String refreshGraphAndFleet() {
        return fleetLifecycle.refreshGraphAndFleet();
    }
}
