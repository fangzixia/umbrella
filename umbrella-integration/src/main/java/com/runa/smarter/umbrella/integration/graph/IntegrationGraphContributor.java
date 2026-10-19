package com.runa.smarter.umbrella.integration.graph;

import com.runa.smarter.umbrella.graph.GraphEditor;
import com.runa.smarter.umbrella.graph.placeholder.IntegrationTopologyPlaceholder;
import com.runa.smarter.umbrella.graph.spi.GraphContributor;

/** integration 模块的 {@link GraphContributor} SPI 实现，注册 {@code META-INF/services}。 */
public final class IntegrationGraphContributor implements GraphContributor {

    @Override
    public void contribute(GraphEditor graph) {
        IntegrationTopologyPlaceholder.populate(graph);
    }

}
