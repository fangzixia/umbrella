package com.runa.smarter.umbrella.graph.model;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;
import com.runa.smarter.umbrella.graph.GraphEdge;

/** {@link com.runa.smarter.umbrella.graph.GraphEdge} 的默认 record 实现。 */
public record SimpleGraphEdge(
        EdgeId id,
        NodeId source,
        NodeId target,
        String relationType,
        GraphProperties properties) implements GraphEdge {

    public SimpleGraphEdge {
        if (relationType == null || relationType.isBlank()) {
            throw new IllegalArgumentException(BusinessErrorMessage.format(
                    "边 relationType 为空或仅包含空白字符。",
                    "请为边指定关系类型（如 contains、depends_on）。"));
        }
        if (properties == null) {
            properties = GraphProperties.EMPTY;
        }
    }

    public static SimpleGraphEdge of(
            EdgeId id,
            NodeId source,
            NodeId target,
            String relationType,
            GraphProperties properties) {
        return new SimpleGraphEdge(id, source, target, relationType, properties);
    }

    public static SimpleGraphEdge of(EdgeId id, NodeId source, NodeId target, String relationType) {
        return new SimpleGraphEdge(id, source, target, relationType, GraphProperties.EMPTY);
    }

}
