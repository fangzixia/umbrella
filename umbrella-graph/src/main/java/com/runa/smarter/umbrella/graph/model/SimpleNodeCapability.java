package com.runa.smarter.umbrella.graph.model;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;
import com.runa.smarter.umbrella.graph.CapabilityInvokeRequest;
import com.runa.smarter.umbrella.graph.NodeCapability;
import java.util.function.Function;

/**
 * {@link NodeCapability} 的默认实现。
 * 仅描述时可省略 {@code invoker}；无 invoker 时 {@link #invoke} 抛 {@link UnsupportedOperationException}。
 */
public record SimpleNodeCapability(
        String name,
        String description,
        Function<CapabilityInvokeRequest, Object> invoker) implements NodeCapability {

    public SimpleNodeCapability(String name, String description) {
        this(name, description, null);
    }

    public SimpleNodeCapability {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(BusinessErrorMessage.format(
                    "能力 name 为空或仅包含空白字符。",
                    "请为 NodeCapability 指定唯一的非空名称。"));
        }
        if (description == null) {
            description = "";
        }
    }

    @Override
    public boolean invokable() {
        return invoker != null;
    }

    @Override
    public Object invoke(CapabilityInvokeRequest request) {
        if (invoker == null) {
            throw new UnsupportedOperationException("能力不可执行（仅描述）: " + name);
        }
        return invoker.apply(request);
    }

    public static SimpleNodeCapability of(String name, String description) {
        return new SimpleNodeCapability(name, description);
    }

    public static SimpleNodeCapability of(
            String name,
            String description,
            Function<CapabilityInvokeRequest, Object> invoker) {
        return new SimpleNodeCapability(name, description, invoker);
    }

}
