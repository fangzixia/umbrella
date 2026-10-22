package com.runa.smarter.umbrella.integration.graph;

import com.runa.smarter.umbrella.graph.GraphEditor;
import com.runa.smarter.umbrella.graph.model.EdgeId;
import com.runa.smarter.umbrella.graph.model.GraphProperties;
import com.runa.smarter.umbrella.graph.model.NodeId;
import com.runa.smarter.umbrella.graph.model.SimpleGraphEdge;
import com.runa.smarter.umbrella.graph.model.SimpleGraphNode;
import com.runa.smarter.umbrella.integration.graph.wds.NetRelationTreeNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 将 WDS 一网关系树映射为 Umbrella 图节点与 {@code contains} 边。 */
@Component
public class NavigationTreeGraphMapper {

    /** 父子节点在图中的关系类型。 */
    private static final String RELATION_CONTAINS = "contains";

    public void populate(GraphEditor graph, List<NetRelationTreeNode> roots) {
        if (roots == null || roots.isEmpty()) {
            return;
        }
        for (NetRelationTreeNode root : roots) {
            visitNode(graph, null, root);
        }
    }

    private void visitNode(GraphEditor graph, NetRelationTreeNode parent, NetRelationTreeNode node) {
        NodeId nodeId = NodeId.of(node.id());
        graph.addNode(SimpleGraphNode.of(
                nodeId,
                "wds-type-" + node.type(),
                node.name(),
                GraphProperties.of(propertiesFor(node)),
                List.of()));
        if (parent != null) {
            graph.addEdge(SimpleGraphEdge.of(
                    EdgeId.of("edge:" + parent.id() + ":" + node.id()),
                    NodeId.of(parent.id()),
                    nodeId,
                    RELATION_CONTAINS));
        }
        for (NetRelationTreeNode child : node.children()) {
            visitNode(graph, node, child);
        }
    }

    private static Map<String, String> propertiesFor(NetRelationTreeNode node) {
        Map<String, String> entries = new HashMap<>();
        entries.put("objectId", String.valueOf(node.objectId()));
        entries.put("permitted", String.valueOf(node.permitted()));
        entries.put("wdsType", String.valueOf(node.type()));
        return entries;
    }

}
