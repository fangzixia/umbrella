package com.runa.smarter.umbrella.graph.model;

/** 边唯一标识（非空字符串）。 */
public record EdgeId(String value) {

    public EdgeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("edge id must not be blank");
        }
    }

    public static EdgeId of(String value) {
        return new EdgeId(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
