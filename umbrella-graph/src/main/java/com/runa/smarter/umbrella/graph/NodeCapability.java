package com.runa.smarter.umbrella.graph;

/** 节点上的能力描述；{@link #invoke(CapabilityInvokeRequest)} 可后续包装为 Agent 工具。 */
public interface NodeCapability {

    String name();

    String description();

    /** 是否支持 {@link #invoke}；仅声明、无执行逻辑时为 {@code false}。 */
    default boolean invokable() {
        return true;
    }

    Object invoke(CapabilityInvokeRequest request);

}
