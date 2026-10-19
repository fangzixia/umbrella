package com.runa.smarter.umbrella.runtime.agent;

import com.runa.smarter.umbrella.graph.spi.DemoAgentToolProvider;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Path;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 演示用 Harness Agent：对话时按需注册 {@link DemoAgentToolProvider} 提供的集成工具。
 * DashScope 未启用时 {@link #chat} 返回错误。
 */
@Component
public class UmbrellaDemoAgent {

    private static final String AGENT_NAME = "umbrella-assistant";
    private static final String AGENT_ID = "umbrella-assistant";
    private static final String SYSTEM_PROMPT = """
            You are the Umbrella assistant. \
            Use check_integration_health, demo_cache_put, and demo_cache_get when users ask about infrastructure or demo cache.""";
    private static final Path WORKSPACE = Path.of(System.getProperty("user.home"), ".umbrella", "workspace", "./");

    private final ObjectProvider<DemoAgentToolProvider> demoAgentToolProvider;
    private final ObjectProvider<DashScopeChatModel> dashScopeChatModel;

    public UmbrellaDemoAgent(
            ObjectProvider<DemoAgentToolProvider> demoAgentToolProvider,
            ObjectProvider<DashScopeChatModel> dashScopeChatModel) {
        this.demoAgentToolProvider = demoAgentToolProvider;
        this.dashScopeChatModel = dashScopeChatModel;
    }

    public Mono<Msg> chat(String userId, String sessionId, String userText) {
        DashScopeChatModel model = dashScopeChatModel.getIfAvailable();
        if (model == null) {
            return Mono.error(new IllegalStateException(
                    "DashScope model is not available. Set agentscope.dashscope.enabled=true and agentscope.dashscope.api-key in application.yml."));
        }

        Toolkit toolkit = new Toolkit();
        demoAgentToolProvider.ifAvailable(provider -> toolkit.registerTool(provider.demoTools()));

        HarnessAgent.Builder agentBuilder = HarnessAgent.builder()
                .name(AGENT_NAME)
                .agentId(AGENT_ID)
                .sysPrompt(SYSTEM_PROMPT)
                .model(model)
                .toolkit(toolkit)
                .workspace(WORKSPACE);

        try (HarnessAgent agent = agentBuilder.build()) {
            RuntimeContext context = RuntimeContext.builder()
                    .userId(userId)
                    .sessionId(sessionId)
                    .build();
            return agent.call(new UserMessage(userText), context);
        }
    }

}
