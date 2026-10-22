package com.runa.smarter.umbrella.tools.messaging;

import io.agentscope.core.agent.RuntimeContext;

/** AgentScope 工具执行线程上的 {@link RuntimeContext} 桥接。 */
public final class AgentToolRuntimeBridge {

    private static final ThreadLocal<RuntimeContext> CURRENT = new ThreadLocal<>();

    private AgentToolRuntimeBridge() {
    }

    public static void bind(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(runtimeContext);
        }
    }

    public static RuntimeContext current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
