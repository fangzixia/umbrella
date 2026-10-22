package com.runa.smarter.umbrella.graph.spi;

import com.runa.smarter.umbrella.graph.GraphDefinitionException;
import com.runa.smarter.umbrella.graph.GraphEditor;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;

/** 通过 {@link ServiceLoader} 加载并应用所有 {@link GraphContributor}。 */
public final class GraphContributors {

    private GraphContributors() {
    }

    /**
     * 从 classpath 上所有 {@link GraphContributor} 的 {@link GraphContributor#graphName()} 解析唯一图名；
     * 无声明时使用 {@code fallback}，多个不同名称时抛出 {@link GraphDefinitionException}。
     */
    public static String resolveGraphName(String fallback) {
        Set<String> names = new HashSet<>();
        ServiceLoader.load(GraphContributor.class).forEach(contributor ->
                contributor.graphName().ifPresent(names::add));
        if (names.isEmpty()) {
            return fallback;
        }
        if (names.size() > 1) {
            throw GraphDefinitionException.because(
                    "classpath 上多个 GraphContributor 声明了不同的图名: " + names + "。",
                    "请在各模块 SPI 中统一 graphName()，或仅保留一个声明图名的贡献者。");
        }
        return names.iterator().next();
    }

    public static void loadAndApply(GraphEditor graph) {
        ServiceLoader.load(GraphContributor.class).forEach(contributor -> contributor.contribute(graph));
    }

}
