package com.runa.smarter.umbrella.integration.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code integration.*} 配置：WDS 图谱拉取、Bearer 等。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "integration")
public class IntegrationProperties {

    /** WDS 等接口使用的 Bearer Token。 */
    private String bearer = "";
    private Graph graph = new Graph();

    @Getter
    @Setter
    public static final class Graph {

        /** WDS 导航树接口地址。 */
        private String url = "";
        /** 为 true 时请求头携带 Authorization: Bearer。 */
        private boolean auth;

    }

}
