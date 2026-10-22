package com.runa.smarter.umbrella.runtime.agent.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runa.smarter.umbrella.graph.model.EdgeId;
import com.runa.smarter.umbrella.graph.model.GraphProperties;
import com.runa.smarter.umbrella.graph.model.Graphs;
import com.runa.smarter.umbrella.graph.model.NodeId;
import com.runa.smarter.umbrella.graph.model.SimpleGraphEdge;
import com.runa.smarter.umbrella.graph.model.SimpleGraphNode;
import com.runa.smarter.umbrella.runtime.agent.AgentAddress;
import java.util.List;
import org.junit.jupiter.api.Test;

class GraphEdgeAssociationPolicyTest {

    private final GraphEdgeAssociationPolicy policy = new GraphEdgeAssociationPolicy();

    @Test
    void masterCanReachAllNodes() {
        var graph = Graphs.empty("test");
        graph.addNode(SimpleGraphNode.of(NodeId.of("a"), "t", "A", GraphProperties.EMPTY, List.of()));
        graph.addNode(SimpleGraphNode.of(NodeId.of("b"), "t", "B", GraphProperties.EMPTY, List.of()));
        assertTrue(policy.allows(AgentAddress.master(), AgentAddress.node("b"), graph));
    }

    @Test
    void nodeOnlyAdjacent() {
        var graph = Graphs.empty("test");
        graph.addNode(SimpleGraphNode.of(NodeId.of("a"), "t", "A", GraphProperties.EMPTY, List.of()));
        graph.addNode(SimpleGraphNode.of(NodeId.of("b"), "t", "B", GraphProperties.EMPTY, List.of()));
        graph.addNode(SimpleGraphNode.of(NodeId.of("c"), "t", "C", GraphProperties.EMPTY, List.of()));
        graph.addEdge(SimpleGraphEdge.of(EdgeId.of("e1"), NodeId.of("a"), NodeId.of("b"), "link"));
        assertTrue(policy.allows(AgentAddress.node("a"), AgentAddress.node("b"), graph));
        assertFalse(policy.allows(AgentAddress.node("a"), AgentAddress.node("c"), graph));
    }
}
