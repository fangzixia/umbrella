package com.runa.smarter.umbrella.graph.model;

/** 节点唯一标识（非空字符串）。 */
public record NodeId(String value) {

    public NodeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("node id must not be blank");
        }
    }

    public static NodeId of(String value) {
        return new NodeId(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
