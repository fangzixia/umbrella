package com.runa.smarter.umbrella.runtime.agent.harness;

import io.agentscope.core.agent.RuntimeContext;
import java.util.concurrent.ConcurrentHashMap;

/** 按 agentId 跟踪进行中的 {@link RuntimeContext}（跨 Reactor 线程）。 */
public final class UmbrellaActiveCallRegistry {

    private static final ConcurrentHashMap<String, RuntimeContext> ACTIVE = new ConcurrentHashMap<>();

    private UmbrellaActiveCallRegistry() {
    }

    public static void register(String agentId, RuntimeContext context) {
        if (agentId == null || context == null) {
            return;
        }
        ACTIVE.put(agentId, context);
    }

    public static void unregister(String agentId) {
        if (agentId != null) {
            ACTIVE.remove(agentId);
        }
    }

    public static RuntimeContext contextFor(String agentId) {
        return agentId == null ? null : ACTIVE.get(agentId);
    }

    public static boolean hasActiveCalls() {
        return !ACTIVE.isEmpty();
    }
}
