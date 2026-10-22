package com.runa.smarter.umbrella.runtime.agent;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;
import com.runa.smarter.umbrella.tools.AgentToolkitFactory;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Path;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 演示用 Harness Agent：挂载 {@link AgentToolkitFactory#createDemoToolkit()} 中的自定义工具。
 * DashScope 未启用时 {@link #chat} 返回错误。
 */
@Component
public class UmbrellaDemoAgent {

    private static final String AGENT_NAME = "umbrella-assistant";
    private static final String AGENT_ID = "umbrella-assistant";
    private static final String SYSTEM_PROMPT = """
            你是 Umbrella 助手。用户询问基础设施或演示缓存时，\
            可使用 check_integration_health、demo_cache_put、demo_cache_get 工具。""";
    private static final Path WORKSPACE = Path.of(System.getProperty("user.home"), ".umbrella", "workspace", "./");

    private final AgentToolkitFactory toolkitFactory;
    private final ObjectProvider<DashScopeChatModel> dashScopeChatModel;

    public UmbrellaDemoAgent(AgentToolkitFactory toolkitFactory, ObjectProvider<DashScopeChatModel> dashScopeChatModel) {
        this.toolkitFactory = toolkitFactory;
        this.dashScopeChatModel = dashScopeChatModel;
    }

    public Mono<Msg> chat(String userId, String sessionId, String userText) {
        DashScopeChatModel model = dashScopeChatModel.getIfAvailable();
        if (model == null) {
            return Mono.error(new IllegalStateException(BusinessErrorMessage.format(
                    "DashScope 聊天模型 Bean 未注入（未启用或 api-key 无效）。",
                    "在 application.yml 中设置 agentscope.dashscope.enabled=true 并配置有效的 agentscope.dashscope.api-key。")));
        }

        HarnessAgent.Builder agentBuilder = HarnessAgent.builder()
                .name(AGENT_NAME)
                .agentId(AGENT_ID)
                .sysPrompt(SYSTEM_PROMPT)
                .model(model)
                .toolkit(toolkitFactory.createDemoToolkit())
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
