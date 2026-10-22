package com.runa.smarter.umbrella.tools.capability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.runa.smarter.umbrella.graph.CapabilityInvokeRequest;
import com.runa.smarter.umbrella.graph.model.GraphProperties;
import com.runa.smarter.umbrella.graph.model.NodeId;
import com.runa.smarter.umbrella.graph.model.SimpleGraphNode;
import com.runa.smarter.umbrella.graph.model.SimpleNodeCapability;
import java.util.List;
import org.junit.jupiter.api.Test;

class NodeCapabilityToolsTest {

    @Test
    void detectsInvokableCapabilities() {
        var node = SimpleGraphNode.of(
                NodeId.of("n1"),
                "service",
                "Node",
                GraphProperties.EMPTY,
                List.of(
                        SimpleNodeCapability.of("read", "仅描述"),
                        SimpleNodeCapability.of("run", "执行", request -> "done")));

        var tools = new NodeCapabilityTools(node);
        assertTrue(tools.hasInvokableCapability());
        assertTrue(tools.invokeNodeCapability("run", null).contains("done"));
        assertTrue(tools.invokeNodeCapability("read", null).contains("不可执行"));
    }

    @Test
    void emptyCapabilitiesAreNotInvokable() {
        var node = SimpleGraphNode.of(NodeId.of("n2"), "service", "Empty");
        assertFalse(new NodeCapabilityTools(node).hasInvokableCapability());
    }

}
