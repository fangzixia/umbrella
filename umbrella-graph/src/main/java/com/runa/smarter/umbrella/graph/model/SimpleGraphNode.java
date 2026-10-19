package com.runa.smarter.umbrella.graph.model;

import com.runa.smarter.umbrella.graph.GraphNode;
import com.runa.smarter.umbrella.graph.NodeCapability;
import java.util.List;

/** {@link com.runa.smarter.umbrella.graph.GraphNode} 的默认 record 实现。 */
public record SimpleGraphNode(
        NodeId id,
        String type,
        String displayName,
        GraphProperties properties,
        List<NodeCapability> capabilities) implements GraphNode {

    public SimpleGraphNode {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("node type must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("node displayName must not be blank");
        }
        if (properties == null) {
            properties = GraphProperties.EMPTY;
        }
        if (capabilities == null || capabilities.isEmpty()) {
            capabilities = List.of();
        } else {
            capabilities = List.copyOf(capabilities);
        }
    }

    public static SimpleGraphNode of(
            NodeId id,
            String type,
            String displayName,
            GraphProperties properties,
            List<NodeCapability> capabilities) {
        return new SimpleGraphNode(id, type, displayName, properties, capabilities);
    }

    public static SimpleGraphNode of(NodeId id, String type, String displayName) {
        return new SimpleGraphNode(id, type, displayName, GraphProperties.EMPTY, List.of());
    }

}
