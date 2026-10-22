package com.runa.smarter.umbrella.runtime.agent.harness;

import com.runa.smarter.umbrella.tools.messaging.AgentToolRuntimeBridge;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.hook.PostActingEvent;
import io.agentscope.core.hook.PreActingEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** 工具执行前绑定 {@link RuntimeContext}，避免 Reactor 线程切换丢失上下文。 */
@Component
public class UmbrellaToolContextHook implements Hook {

    @Override
    public <T extends HookEvent> Mono<T> onEvent(T event) {
        if (event instanceof PreActingEvent preActing) {
            if (preActing.getAgent() instanceof AgentBase agent) {
                RuntimeContext context = UmbrellaActiveCallRegistry.contextFor(agent.getAgentId());
                if (context == null) {
                    context = agent.getRuntimeContext();
                }
                if (context != null) {
                    AgentToolRuntimeBridge.bind(context);
                    UmbrellaCallContextHolder.bind(context);
                }
            }
        } else if (event instanceof PostActingEvent) {
            AgentToolRuntimeBridge.clear();
        }
        return Mono.just(event);
    }
}
