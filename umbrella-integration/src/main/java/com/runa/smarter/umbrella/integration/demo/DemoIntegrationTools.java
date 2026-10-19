package com.runa.smarter.umbrella.integration.demo;

import com.runa.smarter.umbrella.integration.IntegrationGateway;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

/** AgentScope 演示工具：经 {@link IntegrationGateway} 访问 DB/Redis。 */
@Component
public class DemoIntegrationTools {

    private final IntegrationGateway integrationGateway;

    public DemoIntegrationTools(IntegrationGateway integrationGateway) {
        this.integrationGateway = integrationGateway;
    }

    @Tool(
            name = "check_integration_health",
            description = "Check whether database and Redis integrations are reachable.",
            readOnly = true,
            concurrencySafe = true)
    public String checkIntegrationHealth() {
        var health = integrationGateway.checkHealth();
        return health.message();
    }

    @Tool(
            name = "demo_cache_put",
            description = "Store a demo value in Redis through the integration layer.")
    public String demoCachePut(
            @ToolParam(name = "key", description = "Cache key") String key,
            @ToolParam(name = "value", description = "Value to store") String value) {
        integrationGateway.putDemoCache(key, value);
        return "Stored demo cache entry for key: " + key;
    }

    @Tool(
            name = "demo_cache_get",
            description = "Read a demo value from Redis through the integration layer.",
            readOnly = true,
            concurrencySafe = true)
    public String demoCacheGet(@ToolParam(name = "key", description = "Cache key") String key) {
        return integrationGateway.getDemoCache(key)
                .map(value -> "Value for key " + key + ": " + value)
                .orElse("No value found for key: " + key);
    }

}
