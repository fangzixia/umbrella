package com.runa.smarter.umbrella.graph.spi;

import com.runa.smarter.umbrella.graph.GraphEditor;
import java.util.ServiceLoader;

/** 通过 {@link ServiceLoader} 加载并应用所有 {@link GraphContributor}。 */
public final class GraphContributors {

    private GraphContributors() {
    }

    public static void loadAndApply(GraphEditor graph) {
        ServiceLoader.load(GraphContributor.class).forEach(contributor -> contributor.contribute(graph));
    }

}
