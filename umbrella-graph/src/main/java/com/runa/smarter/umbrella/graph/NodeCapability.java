package com.runa.smarter.umbrella.graph;

/** 节点上的能力描述；{@link #invoke(CapabilityInvokeRequest)} 可后续包装为 Agent 工具。 */
public interface NodeCapability {

    String name();

    String description();

    Object invoke(CapabilityInvokeRequest request);

}
