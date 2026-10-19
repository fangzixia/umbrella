package com.runa.smarter.umbrella.graph.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runa.smarter.umbrella.graph.GraphEditor;
import org.junit.jupiter.api.Test;

class MutableGraphTest {

    @Test
    void emptyAddRemoveAndCascadeEdges() {
        GraphEditor graph = Graphs.empty();
        graph.addNode(SimpleGraphNode.of(NodeId.of("a"), "service", "A"));
        graph.addNode(SimpleGraphNode.of(NodeId.of("b"), "service", "B"));
        graph.addEdge(SimpleGraphEdge.of(EdgeId.of("e1"), NodeId.of("a"), NodeId.of("b"), "depends_on"));

        assertTrue(graph.findNode(NodeId.of("a")).isPresent());
        assertEquals(1, graph.edges().size());

        graph.removeNode(NodeId.of("a"));
        assertTrue(graph.findNode(NodeId.of("a")).isEmpty());
        assertEquals(0, graph.edges().size());
    }

    @Test
    void copyFromGraphView() {
        GraphEditor graph = Graphs.empty();
        graph.addNode(SimpleGraphNode.of(NodeId.of("a"), "service", "A"));
        graph.addEdge(SimpleGraphEdge.of(EdgeId.of("e1"), NodeId.of("a"), NodeId.of("a"), "self"));

        GraphEditor copy = Graphs.from(graph);
        assertEquals(graph.nodes().size(), copy.nodes().size());
        assertEquals(1, copy.edgesFrom(NodeId.of("a")).size());
    }

}
