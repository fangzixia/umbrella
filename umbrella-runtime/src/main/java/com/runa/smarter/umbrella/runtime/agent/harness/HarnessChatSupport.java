package com.runa.smarter.umbrella.runtime.agent.harness;

import com.runa.smarter.umbrella.runtime.agent.AgentAddress;
import com.runa.smarter.umbrella.runtime.agent.SessionTurn;
import com.runa.smarter.umbrella.runtime.agent.UmbrellaAgentCallContext;
import com.runa.smarter.umbrella.tools.messaging.AgentToolRuntimeBridge;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.harness.agent.HarnessAgent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import reactor.core.publisher.Mono;

public final class HarnessChatSupport {

    private HarnessChatSupport() {
    }

    public static RuntimeContext runtimeContext(String sessionId, AgentAddress from, int depth) {
        return RuntimeContext.builder()
                .sessionId(sessionId)
                .userId("umbrella-http")
                .put(UmbrellaAgentCallContext.class, new UmbrellaAgentCallContext(from, depth))
                .build();
    }

    public static List<Msg> messagesFromHistory(List<SessionTurn> history, String userMessage) {
        List<Msg> messages = new ArrayList<>();
        if (history != null) {
            for (SessionTurn turn : history) {
                if (turn == null || turn.content() == null || turn.content().isBlank()) {
                    continue;
                }
                if ("user".equals(turn.role())) {
                    messages.add(new UserMessage(turn.content()));
                } else if ("assistant".equals(turn.role())) {
                    messages.add(new AssistantMessage(turn.content()));
                }
            }
        }
        messages.add(new UserMessage(userMessage));
        return messages;
    }

    public static Mono<String> callText(
            HarnessAgent agent, List<Msg> messages, RuntimeContext context, Duration timeout) {
        return invokeCallText(
                activeCallRegistryKey(agent.getAgentId(), agent.getName()),
                context,
                agent.call(messages, context),
                timeout);
    }

    public static Mono<String> callText(
            ReActAgent agent,
            String registryKey,
            List<Msg> messages,
            RuntimeContext context,
            Duration timeout) {
        return invokeCallText(registryKey, context, agent.call(messages, context), timeout);
    }

    private static Mono<String> invokeCallText(
            String registryKey, RuntimeContext context, Mono<Msg> call, Duration timeout) {
        UmbrellaActiveCallRegistry.register(registryKey, context);
        UmbrellaCallContextHolder.bind(context);
        AgentToolRuntimeBridge.bind(context);
        return call.map(Msg::getTextContent)
                .map(text -> text == null || text.isBlank() ? "" : text.strip())
                .timeout(timeout)
                .doFinally(signal -> {
                    UmbrellaActiveCallRegistry.unregister(registryKey);
                    UmbrellaCallContextHolder.clear();
                    AgentToolRuntimeBridge.clear();
                });
    }

    private static String activeCallRegistryKey(String agentId, String name) {
        if (agentId == null || agentId.isBlank()) {
            agentId = name;
        }
        return agentId;
    }
}
