package com.runa.smarter.umbrella.graph;

import com.runa.smarter.umbrella.graph.model.EdgeId;
import com.runa.smarter.umbrella.graph.model.GraphProperties;
import com.runa.smarter.umbrella.graph.model.NodeId;

/** 有向边：从 {@code source} 指向 {@code target}；{@code relationType} 为开放关系类型。 */
public interface GraphEdge {

    EdgeId id();

    NodeId source();

    NodeId target();

    String relationType();

    GraphProperties properties();

}
