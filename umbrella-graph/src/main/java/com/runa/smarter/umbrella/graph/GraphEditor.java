package com.runa.smarter.umbrella.graph;

import com.runa.smarter.umbrella.graph.model.NodeId;
import com.runa.smarter.umbrella.graph.model.EdgeId;

/**
 * 可变图：在 {@link GraphView} 之上提供节点、边的增删改。
 * 删节点时会级联删除以该节点为端点的边。
 */
public interface GraphEditor extends GraphView {

    void addNode(GraphNode node);

    void updateNode(GraphNode node);

    void removeNode(NodeId id);

    void addEdge(GraphEdge edge);

    void updateEdge(GraphEdge edge);

    void removeEdge(EdgeId id);

}
