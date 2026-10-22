package com.runa.smarter.umbrella.graph.web;

import com.runa.smarter.umbrella.graph.GraphRegistry;
import com.runa.smarter.umbrella.runtime.agent.fleet.AgentFleetLifecycle;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 图谱管理：查询摘要、触发从 SPI 重新拉取并刷新内存图。 */
@RestController
@RequestMapping("/api/graph")
public class GraphAdminController {

    private final GraphRegistry graphRegistry;
    private final AgentFleetLifecycle fleetLifecycle;

    public GraphAdminController(GraphRegistry graphRegistry, AgentFleetLifecycle fleetLifecycle) {
        this.graphRegistry = graphRegistry;
        this.fleetLifecycle = fleetLifecycle;
    }

    @GetMapping
    public GraphSummary summary() {
        return new GraphSummary(
                graphRegistry.graph().name(),
                graphRegistry.initialized(),
                graphRegistry.nodeCount(),
                graphRegistry.edgeCount());
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public GraphSummary refresh() {
        graphRegistry.refresh();
        fleetLifecycle.scheduleFleetSyncWithGraph();
        return summary();
    }

    public record GraphSummary(String name, boolean initialized, int nodeCount, int edgeCount) {
    }

}
