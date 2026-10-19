package com.runa.smarter.umbrella.graph;

import com.runa.smarter.umbrella.graph.model.NodeId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** 图的只读视图：查询节点、边与能力。 */
public interface GraphView {

    /** 全部节点（副本，不可通过返回值修改内部图）。 */
    Collection<GraphNode> nodes();

    /** 全部边（副本）。 */
    Collection<GraphEdge> edges();

    Optional<GraphNode> findNode(NodeId id);

    /** 以 {@code source} 为起点的出边。 */
    List<GraphEdge> edgesFrom(NodeId source);

    /** 以 {@code target} 为终点的入边。 */
    List<GraphEdge> edgesTo(NodeId target);

    /** 指定节点上的能力列表；节点不存在时为空列表。 */
    List<NodeCapability> capabilities(NodeId nodeId);

}
