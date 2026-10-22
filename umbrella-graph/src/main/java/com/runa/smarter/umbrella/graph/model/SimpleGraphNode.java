package com.runa.smarter.umbrella.graph.model;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;
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
            throw new IllegalArgumentException(BusinessErrorMessage.format(
                    "节点 type 为空或仅包含空白字符。",
                    "请为节点指定类型（如 service、agent 或 wds-type-*）。"));
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException(BusinessErrorMessage.format(
                    "节点 displayName 为空或仅包含空白字符。",
                    "请从业务数据填充节点显示名称（如 WDS 的 name 字段）。"));
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
