package com.runa.smarter.umbrella.runtime.agent;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;
import com.runa.smarter.umbrella.runtime.agent.fleet.AgentFleetLifecycle;
import com.runa.smarter.umbrella.runtime.agent.harness.HarnessChatSupport;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** 总控对话 HTTP 网关。 */
@Service
public class MasterAgentChatService {

    private static final Duration CHAT_TIMEOUT = Duration.ofSeconds(90);

    private final AgentFleetLifecycle fleetLifecycle;
    private final ObjectProvider<DashScopeChatModel> models;

    public MasterAgentChatService(
            AgentFleetLifecycle fleetLifecycle,
            ObjectProvider<DashScopeChatModel> models) {
        this.fleetLifecycle = fleetLifecycle;
        this.models = models;
    }

    public boolean available() {
        return models.getIfAvailable() != null && fleetLifecycle.masterRunning();
    }

    public Mono<String> chat(String message, List<NodeAgentChatService.Turn> history) {
        return Mono.defer(() -> {
            if (models.getIfAvailable() == null) {
                return Mono.error(new NodeAgentChatService.ModelUnavailableException());
            }
            fleetLifecycle.startMasterIfNeeded();
            HarnessAgent master = fleetLifecycle.masterAgent();
            if (master == null) {
                return Mono.error(new IllegalStateException(BusinessErrorMessage.format(
                        "总控 Agent 尚未就绪。",
                        "请确认 DashScope 已配置。")));
            }
            List<SessionTurn> session = history == null
                    ? List.of()
                    : history.stream()
                            .map(turn -> new SessionTurn(turn.role(), turn.content()))
                            .toList();
            RuntimeContext context = HarnessChatSupport.runtimeContext(
                    AgentSessions.MASTER, AgentAddress.master(), 0);
            return HarnessChatSupport.callText(
                    master,
                    HarnessChatSupport.messagesFromHistory(session, message),
                    context,
                    CHAT_TIMEOUT);
        });
    }
}
