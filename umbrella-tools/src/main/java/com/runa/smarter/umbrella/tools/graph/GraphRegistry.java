package com.runa.smarter.umbrella.tools.graph;

import com.runa.smarter.umbrella.graph.GraphEditor;
import com.runa.smarter.umbrella.graph.model.Graphs;
import com.runa.smarter.umbrella.graph.spi.GraphContributors;
import org.springframework.stereotype.Service;

/**
 * 应用内唯一推荐的图 init 入口：手动调用 {@link #init()}，通过 SPI 加载 integration 等模块的 {@link com.runa.smarter.umbrella.graph.spi.GraphContributor}。
 * 不会在 Spring 启动时自动 init。
 */
@Service
public class GraphRegistry {

    private GraphEditor graph = Graphs.empty();
    private boolean initialized;

    /** 重置为空图并应用 classpath 上所有 GraphContributor。 */
    public void init() {
        graph = Graphs.empty();
        GraphContributors.loadAndApply(graph);
        initialized = true;
    }

    public GraphEditor graph() {
        return graph;
    }

    public boolean initialized() {
        return initialized;
    }

}
