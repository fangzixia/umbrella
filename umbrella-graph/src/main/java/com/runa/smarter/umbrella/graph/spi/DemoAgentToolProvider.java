package com.runa.smarter.umbrella.graph.spi;

/**
 * Demo Agent 可选工具提供者 SPI 契约。
 * 由 integration 模块实现并注册 Spring Bean，runtime 通过 {@code ObjectProvider} 注入，避免编译期依赖 integration。
 */
public interface DemoAgentToolProvider {

    /** 返回带 AgentScope {@code @Tool} 注解的工具对象。 */
    Object demoTools();

}
