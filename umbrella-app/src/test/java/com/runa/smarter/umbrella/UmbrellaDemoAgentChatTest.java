package com.runa.smarter.umbrella;

import com.runa.smarter.umbrella.runtime.agent.UmbrellaDemoAgent;
import io.agentscope.core.message.Msg;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UmbrellaDemoAgentChatTest {

    @Autowired
    private UmbrellaDemoAgent demoAgent;

    @DynamicPropertySource
    static void dashScopeFromEnvironment(DynamicPropertyRegistry registry) {
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        registry.add("agentscope.dashscope.enabled", () -> "true");
        registry.add("agentscope.dashscope.api-key", () -> apiKey);
    }

    @Test
    void chat_repliesWithMessage() {
        Msg reply = demoAgent.chat("demo-user", "demo-session", "请调用 check_integration_health 并简要说明结果")
                .block();

        assertNotNull(reply);
    }

}
