package com.runa.smarter.umbrella.tools.custom;

import io.agentscope.core.tool.Toolkit;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * tools 模块内声明的自定义 AgentScope 工具（与图 SPI、节点能力无关）。
 * 新增工具：在本包添加 {@code @Component} + {@code @Tool}，并加入 {@link #toolObjects}。
 */
@Component
public class CustomAgentTools {

    private final List<Object> toolObjects;

    public CustomAgentTools(DemoIntegrationTools demoIntegrationTools) {
        this.toolObjects = List.of(demoIntegrationTools);
    }

    public void registerAll(Toolkit toolkit) {
        toolObjects.forEach(toolkit::registerTool);
    }

}
