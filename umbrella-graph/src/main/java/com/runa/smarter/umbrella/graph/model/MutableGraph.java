package com.runa.smarter.umbrella.graph.model;

import com.runa.smarter.umbrella.graph.GraphDefinitionException;
import com.runa.smarter.umbrella.graph.GraphEdge;
import com.runa.smarter.umbrella.graph.GraphEditor;
import com.runa.smarter.umbrella.graph.GraphNode;
import com.runa.smarter.umbrella.graph.GraphView;
import com.runa.smarter.umbrella.graph.NodeCapability;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** {@link GraphEditor} 的默认实现；内存中维护节点、边及邻接索引。 */
public final class MutableGraph implements GraphEditor {

    private final Map<NodeId, GraphNode> nodesById = new HashMap<>();
    private final List<GraphEdge> edges = new ArrayList<>();
    private final Map<NodeId, List<GraphEdge>> edgesBySource = new HashMap<>();
    private final Map<NodeId, List<GraphEdge>> edgesByTarget = new HashMap<>();
    private final Set<EdgeId> edgeIds = new HashSet<>();

    public MutableGraph() {
    }

    MutableGraph(GraphView view) {
        if (view == null) {
            throw new GraphDefinitionException("view must not be null");
        }
        for (GraphNode node : view.nodes()) {
            addNode(node);
        }
        for (GraphEdge edge : view.edges()) {
            addEdge(edge);
        }
    }

    @Override
    public void addNode(GraphNode node) {
        if (node == null) {
            throw new GraphDefinitionException("node must not be null");
        }
        if (nodesById.containsKey(node.id())) {
            throw new GraphDefinitionException("duplicate node id: " + node.id());
        }
        nodesById.put(node.id(), node);
    }

    @Override
    public void updateNode(GraphNode node) {
        if (node == null) {
            throw new GraphDefinitionException("node must not be null");
        }
        if (!nodesById.containsKey(node.id())) {
            throw new GraphDefinitionException("node not found: " + node.id());
        }
        nodesById.put(node.id(), node);
    }

    @Override
    public void removeNode(NodeId id) {
        if (id == null) {
            throw new GraphDefinitionException("node id must not be null");
        }
        if (!nodesById.containsKey(id)) {
            throw new GraphDefinitionException("node not found: " + id);
        }
        Set<EdgeId> incidentEdgeIds = new HashSet<>();
        for (GraphEdge edge : edgesFrom(id)) {
            incidentEdgeIds.add(edge.id());
        }
        for (GraphEdge edge : edgesTo(id)) {
            incidentEdgeIds.add(edge.id());
        }
        for (EdgeId edgeId : incidentEdgeIds) {
            removeEdge(edgeId);
        }
        nodesById.remove(id);
        edgesBySource.remove(id);
        edgesByTarget.remove(id);
    }

    @Override
    public void addEdge(GraphEdge edge) {
        if (edge == null) {
            throw new GraphDefinitionException("edge must not be null");
        }
        if (!edgeIds.add(edge.id())) {
            throw new GraphDefinitionException("duplicate edge id: " + edge.id());
        }
        if (!nodesById.containsKey(edge.source())) {
            throw new GraphDefinitionException(
                    "edge %s references missing source node %s".formatted(edge.id(), edge.source()));
        }
        if (!nodesById.containsKey(edge.target())) {
            throw new GraphDefinitionException(
                    "edge %s references missing target node %s".formatted(edge.id(), edge.target()));
        }
        edges.add(edge);
        indexEdge(edge);
    }

    @Override
    public void updateEdge(GraphEdge edge) {
        if (edge == null) {
            throw new GraphDefinitionException("edge must not be null");
        }
        if (!edgeIds.contains(edge.id())) {
            throw new GraphDefinitionException("edge not found: " + edge.id());
        }
        if (!nodesById.containsKey(edge.source())) {
            throw new GraphDefinitionException(
                    "edge %s references missing source node %s".formatted(edge.id(), edge.source()));
        }
        if (!nodesById.containsKey(edge.target())) {
            throw new GraphDefinitionException(
                    "edge %s references missing target node %s".formatted(edge.id(), edge.target()));
        }
        GraphEdge existing = findEdgeById(edge.id())
                .orElseThrow(() -> new GraphDefinitionException("edge not found: " + edge.id()));
        deindexEdge(existing);
        int index = edges.indexOf(existing);
        edges.set(index, edge);
        indexEdge(edge);
    }

    @Override
    public void removeEdge(EdgeId id) {
        if (id == null) {
            throw new GraphDefinitionException("edge id must not be null");
        }
        GraphEdge edge = findEdgeById(id)
                .orElseThrow(() -> new GraphDefinitionException("edge not found: " + id));
        deindexEdge(edge);
        edges.remove(edge);
        edgeIds.remove(id);
    }

    @Override
    public Collection<GraphNode> nodes() {
        return List.copyOf(nodesById.values());
    }

    @Override
    public Collection<GraphEdge> edges() {
        return List.copyOf(edges);
    }

    @Override
    public Optional<GraphNode> findNode(NodeId id) {
        return Optional.ofNullable(nodesById.get(id));
    }

    @Override
    public List<GraphEdge> edgesFrom(NodeId source) {
        return List.copyOf(edgesBySource.getOrDefault(source, List.of()));
    }

    @Override
    public List<GraphEdge> edgesTo(NodeId target) {
        return List.copyOf(edgesByTarget.getOrDefault(target, List.of()));
    }

    @Override
    public List<NodeCapability> capabilities(NodeId nodeId) {
        return findNode(nodeId).map(GraphNode::capabilities).orElse(List.of());
    }

    private Optional<GraphEdge> findEdgeById(EdgeId id) {
        for (GraphEdge edge : edges) {
            if (edge.id().equals(id)) {
                return Optional.of(edge);
            }
        }
        return Optional.empty();
    }

    private void indexEdge(GraphEdge edge) {
        edgesBySource.computeIfAbsent(edge.source(), ignored -> new ArrayList<>()).add(edge);
        edgesByTarget.computeIfAbsent(edge.target(), ignored -> new ArrayList<>()).add(edge);
    }

    private void deindexEdge(GraphEdge edge) {
        removeFromAdjacency(edgesBySource, edge.source(), edge);
        removeFromAdjacency(edgesByTarget, edge.target(), edge);
    }

    private static void removeFromAdjacency(Map<NodeId, List<GraphEdge>> adjacency, NodeId key, GraphEdge edge) {
        List<GraphEdge> list = adjacency.get(key);
        if (list == null) {
            return;
        }
        Iterator<GraphEdge> iterator = list.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().id().equals(edge.id())) {
                iterator.remove();
                break;
            }
        }
        if (list.isEmpty()) {
            adjacency.remove(key);
        }
    }

}
