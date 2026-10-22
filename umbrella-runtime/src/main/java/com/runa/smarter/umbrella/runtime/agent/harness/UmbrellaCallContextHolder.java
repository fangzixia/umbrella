package com.runa.smarter.umbrella.runtime.agent.harness;

import com.runa.smarter.umbrella.runtime.agent.UmbrellaAgentCallContext;
import io.agentscope.core.agent.RuntimeContext;

/** 工具执行期间与 {@code agent.call} 同生命周期的调用方上下文（配合 RuntimeContext）。 */
public final class UmbrellaCallContextHolder {

    private static final ThreadLocal<UmbrellaAgentCallContext> CURRENT = new ThreadLocal<>();

    private UmbrellaCallContextHolder() {
    }

    public static void bind(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            CURRENT.remove();
            return;
        }
        UmbrellaAgentCallContext ctx = runtimeContext.get(UmbrellaAgentCallContext.class);
        if (ctx == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(ctx);
        }
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static UmbrellaAgentCallContext current() {
        return CURRENT.get();
    }
}
