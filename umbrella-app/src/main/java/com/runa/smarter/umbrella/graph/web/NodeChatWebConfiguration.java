package com.runa.smarter.umbrella.graph.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 节点对话异步请求超时：略长于 Agent 90 秒，以便返回 JSON 错误而非容器默认超时。 */
@Configuration(proxyBeanMethods = false)
public class NodeChatWebConfiguration implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(95_000);
    }
}
