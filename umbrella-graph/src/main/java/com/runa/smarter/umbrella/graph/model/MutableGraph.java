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

    private final String name;

    private final Map<NodeId, GraphNode> nodesById = new HashMap<>();
    private final List<GraphEdge> edges = new ArrayList<>();
    private final Map<NodeId, List<GraphEdge>> edgesBySource = new HashMap<>();
    private final Map<NodeId, List<GraphEdge>> edgesByTarget = new HashMap<>();
    private final Set<EdgeId> edgeIds = new HashSet<>();

    public MutableGraph() {
        this("关系图谱");
    }

    public MutableGraph(String name) {
        if (name == null || name.isBlank()) {
            throw GraphDefinitionException.because(
                    "创建图谱时未提供有效名称（为空或仅空白）。",
                    "请传入非空的图谱名称，例如「关系图谱」或业务侧约定的图名。");
        }
        this.name = name.strip();
    }

    MutableGraph(GraphView view) {
        if (view == null) {
            throw GraphDefinitionException.because(
                    "从图视图复制时传入了 null。",
                    "请确保源 GraphView 已正确构造后再调用 Graphs.from(view)。");
        }
        this.name = view.name();
        for (GraphNode node : view.nodes()) {
            addNode(node);
        }
        for (GraphEdge edge : view.edges()) {
            addEdge(edge);
        }
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public void addNode(GraphNode node) {
        if (node == null) {
            throw GraphDefinitionException.because(
                    "添加节点时传入了 null。",
                    "请构造有效的 GraphNode（含 id、类型、显示名等）后再调用 addNode。");
        }
        if (nodesById.containsKey(node.id())) {
            throw GraphDefinitionException.because(
                    "节点 id「" + node.id() + "」在图中已存在。",
                    "请使用唯一的节点 id，或先 updateNode/removeNode 后再添加。");
        }
        nodesById.put(node.id(), node);
    }

    @Override
    public void updateNode(GraphNode node) {
        if (node == null) {
            throw GraphDefinitionException.because(
                    "更新节点时传入了 null。",
                    "请传入要更新的完整 GraphNode 实例。");
        }
        if (!nodesById.containsKey(node.id())) {
            throw GraphDefinitionException.because(
                    "要更新的节点 id「" + node.id() + "」不在当前图中。",
                    "请先 addNode 添加该节点，或检查 id 是否与图谱数据一致。");
        }
        nodesById.put(node.id(), node);
    }

    @Override
    public void removeNode(NodeId id) {
        if (id == null) {
            throw GraphDefinitionException.because(
                    "删除节点时节点 id 为 null。",
                    "请传入有效的 NodeId。");
        }
        if (!nodesById.containsKey(id)) {
            throw GraphDefinitionException.because(
                    "要删除的节点 id「" + id + "」不在当前图中。",
                    "请确认 id 正确，或先刷新/重新加载图谱后再操作。");
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
            throw GraphDefinitionException.because(
                    "添加边时传入了 null。",
                    "请构造有效的 GraphEdge（含 id、源、目标、关系类型）后再调用 addEdge。");
        }
        if (!edgeIds.add(edge.id())) {
            throw GraphDefinitionException.because(
                    "边 id「" + edge.id() + "」在图中已存在。",
                    "请使用唯一的边 id，或对已有边调用 updateEdge。");
        }
        if (!nodesById.containsKey(edge.source())) {
            throw GraphDefinitionException.because(
                    "边「" + edge.id() + "」的源节点「" + edge.source() + "」尚未加入图中。",
                    "请先 addNode 添加源节点与目标节点，再添加边。");
        }
        if (!nodesById.containsKey(edge.target())) {
            throw GraphDefinitionException.because(
                    "边「" + edge.id() + "」的目标节点「" + edge.target() + "」尚未加入图中。",
                    "请先 addNode 添加源节点与目标节点，再添加边。");
        }
        edges.add(edge);
        indexEdge(edge);
    }

    @Override
    public void updateEdge(GraphEdge edge) {
        if (edge == null) {
            throw GraphDefinitionException.because(
                    "更新边时传入了 null。",
                    "请传入要更新的完整 GraphEdge 实例。");
        }
        if (!edgeIds.contains(edge.id())) {
            throw GraphDefinitionException.because(
                    "要更新的边 id「" + edge.id() + "」不在当前图中。",
                    "请先 addEdge 添加该边，或检查边 id 是否与数据一致。");
        }
        if (!nodesById.containsKey(edge.source())) {
            throw GraphDefinitionException.because(
                    "边「" + edge.id() + "」的源节点「" + edge.source() + "」不在图中。",
                    "请先将源节点加入图，或修正边的 source 引用。");
        }
        if (!nodesById.containsKey(edge.target())) {
            throw GraphDefinitionException.because(
                    "边「" + edge.id() + "」的目标节点「" + edge.target() + "」不在图中。",
                    "请先将目标节点加入图，或修正边的 target 引用。");
        }
        GraphEdge existing = findEdgeById(edge.id())
                .orElseThrow(() -> GraphDefinitionException.because(
                        "边 id「" + edge.id() + "」在索引中不一致（内部数据异常）。",
                        "请避免并发修改同一 GraphEditor，或重新 init/refresh 图谱。"));
        deindexEdge(existing);
        int index = edges.indexOf(existing);
        edges.set(index, edge);
        indexEdge(edge);
    }

    @Override
    public void removeEdge(EdgeId id) {
        if (id == null) {
            throw GraphDefinitionException.because(
                    "删除边时边 id 为 null。",
                    "请传入有效的 EdgeId。");
        }
        GraphEdge edge = findEdgeById(id)
                .orElseThrow(() -> GraphDefinitionException.because(
                        "要删除的边 id「" + id + "」不在当前图中。",
                        "请确认边 id 正确。"));
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
