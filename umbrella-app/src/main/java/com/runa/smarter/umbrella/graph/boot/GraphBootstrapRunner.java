package com.runa.smarter.umbrella.graph.boot;

import com.runa.smarter.umbrella.graph.GraphRegistry;
import com.runa.smarter.umbrella.runtime.agent.fleet.AgentFleetLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

/** 应用启动时若配置了 WDS 地址，则通过 {@link GraphRegistry#init()} 拉取并初始化集成图谱。 */
@Component
@DependsOn("integrationGraphContributorBridge")
@ConditionalOnProperty(prefix = "integration.graph", name = "url", matchIfMissing = false)
public class GraphBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GraphBootstrapRunner.class);

    private final GraphRegistry graphRegistry;
    private final AgentFleetLifecycle fleetLifecycle;

    public GraphBootstrapRunner(GraphRegistry graphRegistry, AgentFleetLifecycle fleetLifecycle) {
        this.graphRegistry = graphRegistry;
        this.fleetLifecycle = fleetLifecycle;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("正在从 WDS 初始化集成图谱…");
        graphRegistry.init();
        log.info("集成图谱初始化完成: 节点={}, 边={}", graphRegistry.nodeCount(), graphRegistry.edgeCount());
        fleetLifecycle.startMasterIfNeeded();
        fleetLifecycle.scheduleFleetSyncWithGraph();
    }

}
