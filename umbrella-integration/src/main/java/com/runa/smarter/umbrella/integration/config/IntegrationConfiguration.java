package com.runa.smarter.umbrella.integration.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** 集成模块 Spring 配置：属性绑定与 WDS 用 {@link RestClient}。 */
@Configuration
@EnableConfigurationProperties(IntegrationProperties.class)
public class IntegrationConfiguration {

    @Bean
    RestClient integrationRestClient() {
        return RestClient.builder().build();
    }

}
