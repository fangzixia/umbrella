package com.runa.smarter.umbrella.graph.model;

import java.util.Collections;
import java.util.Map;

/** 节点或边上的扩展属性（不可变字符串 Map）。 */
public record GraphProperties(Map<String, String> entries) {

    public static final GraphProperties EMPTY = new GraphProperties(Map.of());

    public GraphProperties {
        entries = entries == null || entries.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(Map.copyOf(entries));
    }

    public static GraphProperties of(Map<String, String> entries) {
        return entries == null || entries.isEmpty() ? EMPTY : new GraphProperties(entries);
    }

    public static GraphProperties of(String key, String value) {
        return new GraphProperties(Map.of(key, value));
    }

}
