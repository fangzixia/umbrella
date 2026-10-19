package com.runa.smarter.umbrella.graph;

import java.util.Collections;
import java.util.Map;

/** 调用 {@link NodeCapability#invoke(CapabilityInvokeRequest)} 时的入参（不可变字符串键值）。 */
public record CapabilityInvokeRequest(Map<String, String> arguments) {

    public CapabilityInvokeRequest {
        arguments = arguments == null || arguments.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(Map.copyOf(arguments));
    }

    public static CapabilityInvokeRequest empty() {
        return new CapabilityInvokeRequest(Map.of());
    }

    public static CapabilityInvokeRequest of(Map<String, String> arguments) {
        return new CapabilityInvokeRequest(arguments);
    }

}
