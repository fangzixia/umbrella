package com.runa.smarter.umbrella.graph.model;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;

/** 边唯一标识（非空字符串）。 */
public record EdgeId(String value) {

    public EdgeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(BusinessErrorMessage.format(
                    "边 id 为空或仅包含空白字符。",
                    "请为每条边生成唯一的非空 id（例如 edge:源:目标）。"));
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
