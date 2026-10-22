package com.runa.smarter.umbrella.tools.custom;

import com.runa.smarter.umbrella.integration.IntegrationGateway;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.springframework.stereotype.Component;

/** AgentScope 演示工具：经 {@link IntegrationGateway} 探测数据库/Redis 与演示缓存。 */
@Component
public class DemoIntegrationTools {

    private final IntegrationGateway integrationGateway;

    public DemoIntegrationTools(IntegrationGateway integrationGateway) {
        this.integrationGateway = integrationGateway;
    }

    @Tool(
            name = "check_integration_health",
            description = "检查数据库与 Redis 集成是否可用。",
            readOnly = true,
            concurrencySafe = true)
    public String checkIntegrationHealth() {
        var health = integrationGateway.checkHealth();
        return health.message();
    }

    @Tool(
            name = "demo_cache_put",
            description = "通过集成层将演示数据写入 Redis。")
    public String demoCachePut(
            @ToolParam(name = "key", description = "缓存键") String key,
            @ToolParam(name = "value", description = "要写入的值") String value) {
        integrationGateway.putDemoCache(key, value);
        return "已写入演示缓存，键: " + key;
    }

    @Tool(
            name = "demo_cache_get",
            description = "通过集成层从 Redis 读取演示缓存。",
            readOnly = true,
            concurrencySafe = true)
    public String demoCacheGet(@ToolParam(name = "key", description = "缓存键") String key) {
        return integrationGateway.getDemoCache(key)
                .map(value -> "键 " + key + " 的值: " + value)
                .orElse("未找到键: " + key);
    }

}
