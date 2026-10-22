package com.runa.smarter.umbrella.integration.graph;

import com.runa.smarter.umbrella.graph.GraphEditor;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/** SPI {@link IntegrationGraphContributor} 委托此类，以便使用 Spring 管理的 WDS 客户端与映射器。 */
@Component
public class IntegrationGraphContributorBridge {

    private static volatile IntegrationGraphContributorBridge instance;

    private final IntegrationGraphClient graphClient;
    private final NavigationTreeGraphMapper graphMapper;

    public IntegrationGraphContributorBridge(
            IntegrationGraphClient graphClient, NavigationTreeGraphMapper graphMapper) {
        this.graphClient = graphClient;
        this.graphMapper = graphMapper;
    }

    @PostConstruct
    void register() {
        instance = this;
    }

    public void contribute(GraphEditor graph) {
        graphMapper.populate(graph, graphClient.fetchTree());
    }

    static void contributeFromSpi(GraphEditor graph) {
        IntegrationGraphContributorBridge bridge = instance;
        if (bridge == null) {
            throw new IllegalStateException(com.runa.smarter.umbrella.graph.BusinessErrorMessage.format(
                    "GraphContributor SPI 在 Spring 容器完成 IntegrationGraphContributorBridge 初始化之前被调用。",
                    "请确保应用通过 Spring Boot 启动，且 GraphBootstrapRunner 依赖 integrationGraphContributorBridge；勿在静态块中手动调用 SPI。"));
        }
        bridge.contribute(graph);
    }

    /** 单测在无 Spring 容器时绑定桥接实例。 */
    static void bindForTest(IntegrationGraphContributorBridge bridge) {
        instance = bridge;
    }

    static void clearForTest() {
        instance = null;
    }

}
