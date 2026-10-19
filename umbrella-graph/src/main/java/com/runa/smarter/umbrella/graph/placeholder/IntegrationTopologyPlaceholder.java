package com.runa.smarter.umbrella.graph.placeholder;

import com.runa.smarter.umbrella.graph.GraphEditor;
import com.runa.smarter.umbrella.graph.model.EdgeId;
import com.runa.smarter.umbrella.graph.model.GraphProperties;
import com.runa.smarter.umbrella.graph.model.Graphs;
import com.runa.smarter.umbrella.graph.model.NodeId;
import com.runa.smarter.umbrella.graph.model.SimpleGraphEdge;
import com.runa.smarter.umbrella.graph.model.SimpleGraphNode;
import com.runa.smarter.umbrella.graph.model.SimpleNodeCapability;
import java.util.List;

/**
 * 集成层占位拓扑：应用、数据库、Redis 节点及依赖边（演示用，非真实探测）。
 * 供 integration 的 {@link com.runa.smarter.umbrella.graph.spi.GraphContributor} 委托调用。
 */
public final class IntegrationTopologyPlaceholder {

    private IntegrationTopologyPlaceholder() {
    }

    /** 新建空图并填充占位拓扑。 */
    public static GraphEditor create() {
        GraphEditor graph = Graphs.empty();
        populate(graph);
        return graph;
    }

    /** 在已有可变图上追加占位节点与边。 */
    public static void populate(GraphEditor graph) {
        graph.addNode(SimpleGraphNode.of(
                NodeId.of("umbrella-app"),
                "application",
                "Umbrella App",
                GraphProperties.EMPTY,
                List.of(SimpleNodeCapability.of("serve", "HTTP/API placeholder"))));
        graph.addNode(SimpleGraphNode.of(
                NodeId.of("integration-database"),
                "integration",
                "Database",
                GraphProperties.EMPTY,
                List.of(SimpleNodeCapability.of(
                        "ping",
                        "Database health placeholder",
                        request -> "database:placeholder"))));
        graph.addNode(SimpleGraphNode.of(
                NodeId.of("integration-redis"),
                "integration",
                "Redis",
                GraphProperties.EMPTY,
                List.of(SimpleNodeCapability.of(
                        "ping",
                        "Redis health placeholder",
                        request -> "redis:placeholder"))));
        graph.addEdge(SimpleGraphEdge.of(
                EdgeId.of("app-uses-database"),
                NodeId.of("umbrella-app"),
                NodeId.of("integration-database"),
                "depends_on"));
        graph.addEdge(SimpleGraphEdge.of(
                EdgeId.of("app-uses-redis"),
                NodeId.of("umbrella-app"),
                NodeId.of("integration-redis"),
                "depends_on"));
    }

}
