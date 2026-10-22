package com.runa.smarter.umbrella.graph;

import com.runa.smarter.umbrella.graph.model.NodeId;
import com.runa.smarter.umbrella.graph.model.EdgeId;

/**
 * 可变图：在 {@link GraphView} 之上提供节点、边的增删改。
 * 删节点时会级联删除以该节点为端点的边。
 */
public interface GraphEditor extends GraphView {

    /** 添加节点；id 重复时抛出 {@link com.runa.smarter.umbrella.graph.GraphDefinitionException}。 */
    void addNode(GraphNode node);

    /** 按 id 覆盖已有节点。 */
    void updateNode(GraphNode node);

    /** 删除节点并级联删除关联边。 */
    void removeNode(NodeId id);

    /** 添加边；端点节点必须已存在。 */
    void addEdge(GraphEdge edge);

    /** 按边 id 替换边定义。 */
    void updateEdge(GraphEdge edge);

    /** 按边 id 删除边。 */
    void removeEdge(EdgeId id);

}
