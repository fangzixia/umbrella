package com.runa.smarter.umbrella.graph;

import com.runa.smarter.umbrella.graph.model.Graphs;
import com.runa.smarter.umbrella.graph.spi.GraphContributors;
import org.springframework.stereotype.Service;

/**
 * 应用内图 init 入口：通过 {@link GraphContributors#loadAndApply} 加载 classpath 上所有
 * {@link com.runa.smarter.umbrella.graph.spi.GraphContributor} SPI；失败时不更新 {@link #initialized()}。
 */
@Service
public class GraphRegistry {

    private GraphEditor graph = Graphs.empty();
    private boolean initialized;

    /** 从 GraphContributor SPI 解析图名并初始化；无 SPI 名称时使用「关系图谱」。 */
    public void init() {
        initGraph(GraphContributors.resolveGraphName("关系图谱"));
    }

    /** 以指定名称创建图谱，并应用 classpath 上所有 GraphContributor SPI。 */
    public void initGraph(String name) {
        GraphEditor next = Graphs.empty(name);
        GraphContributors.loadAndApply(next);
        this.graph = next;
        this.initialized = true;
    }

    public void refresh() {
        initGraph(graph.name());
    }

    public GraphEditor graph() {
        return graph;
    }

    public boolean initialized() {
        return initialized;
    }

    public int nodeCount() {
        return graph.nodes().size();
    }

    public int edgeCount() {
        return graph.edges().size();
    }

}
