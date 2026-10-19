package com.runa.smarter.umbrella.graph;

import com.runa.smarter.umbrella.graph.model.GraphProperties;
import com.runa.smarter.umbrella.graph.model.NodeId;
import java.util.List;

/** 图中的一个节点；{@code type} 为开放类型字符串（如 service、agent）。 */
public interface GraphNode {

    NodeId id();

    String type();

    String displayName();

    GraphProperties properties();

    List<NodeCapability> capabilities();

}
