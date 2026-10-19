package com.runa.smarter.umbrella.integration.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.runa.smarter.umbrella.graph.CapabilityInvokeRequest;
import com.runa.smarter.umbrella.graph.model.Graphs;
import com.runa.smarter.umbrella.graph.model.NodeId;
import com.runa.smarter.umbrella.graph.spi.GraphContributors;
import org.junit.jupiter.api.Test;

class IntegrationGraphContributorTest {

    @Test
    void spiInitBuildsPlaceholderTopology() {
        var graph = Graphs.empty();
        GraphContributors.loadAndApply(graph);

        assertEquals(3, graph.nodes().size());
        assertEquals(2, graph.edges().size());
        var database = graph.findNode(NodeId.of("integration-database")).orElseThrow();
        Object result = database.capabilities().getFirst().invoke(CapabilityInvokeRequest.empty());
        assertEquals("database:placeholder", result);
    }

}
