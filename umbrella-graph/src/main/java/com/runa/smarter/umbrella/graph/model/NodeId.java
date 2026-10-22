package com.runa.smarter.umbrella.graph.model;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;

/** 节点唯一标识（非空字符串）。 */
public record NodeId(String value) {

    public NodeId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(BusinessErrorMessage.format(
                    "节点 id 为空或仅包含空白字符。",
                    "请传入上游系统（如 WDS）提供的非空节点标识。"));
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
