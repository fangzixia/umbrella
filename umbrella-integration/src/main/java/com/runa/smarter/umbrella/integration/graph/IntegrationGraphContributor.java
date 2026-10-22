package com.runa.smarter.umbrella.integration.graph;

import com.runa.smarter.umbrella.graph.GraphEditor;
import com.runa.smarter.umbrella.graph.spi.GraphContributor;
import java.util.Optional;

/** integration 模块的 {@link GraphContributor} SPI 实现，注册 {@code META-INF/services}。 */
public final class IntegrationGraphContributor implements GraphContributor {

    public static final String GRAPH_NAME = "wds 一网图";

    @Override
    public Optional<String> graphName() {
        return Optional.of(GRAPH_NAME);
    }

    @Override
    public void contribute(GraphEditor graph) {
        IntegrationGraphContributorBridge.contributeFromSpi(graph);
    }

}
