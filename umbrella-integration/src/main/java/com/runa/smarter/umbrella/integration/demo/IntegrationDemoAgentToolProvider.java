package com.runa.smarter.umbrella.integration.demo;

import com.runa.smarter.umbrella.graph.spi.DemoAgentToolProvider;
import org.springframework.stereotype.Component;

/** 将 {@link DemoIntegrationTools} 暴露给 runtime 的 {@link DemoAgentToolProvider} 实现。 */
@Component
public class IntegrationDemoAgentToolProvider implements DemoAgentToolProvider {

    private final DemoIntegrationTools demoIntegrationTools;

    public IntegrationDemoAgentToolProvider(DemoIntegrationTools demoIntegrationTools) {
        this.demoIntegrationTools = demoIntegrationTools;
    }

    @Override
    public Object demoTools() {
        return demoIntegrationTools;
    }

}
