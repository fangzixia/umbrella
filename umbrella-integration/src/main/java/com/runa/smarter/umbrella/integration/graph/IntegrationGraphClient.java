package com.runa.smarter.umbrella.integration.graph;

import com.runa.smarter.umbrella.integration.config.IntegrationProperties;
import com.runa.smarter.umbrella.integration.graph.wds.NetRelationTreeNode;
import com.runa.smarter.umbrella.integration.graph.wds.WdsApiResponse;
import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** 调用 WDS {@code getFirstNetRelationTree} 拉取一网关系树。 */
@Service
public class IntegrationGraphClient {

    private static final int SUCCESS_CODE = 20000;

    private static final ParameterizedTypeReference<WdsApiResponse<List<NetRelationTreeNode>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final IntegrationProperties properties;
    private final RestClient restClient;

    public IntegrationGraphClient(IntegrationProperties properties, RestClient integrationRestClient) {
        this.properties = properties;
        this.restClient = integrationRestClient;
    }

    public List<NetRelationTreeNode> fetchTree() {
        var graph = properties.getGraph();
        String url = graph.getUrl();
        if (url.isBlank()) {
            throw IntegrationGraphException.because(
                    "未配置 integration.graph.url，无法请求 WDS 导航树。",
                    "在 application.yml（或 application-dev.yml）中设置 integration.graph.url 为 WDS 接口地址。");
        }
        try {
            var request = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON);
            if (graph.isAuth()) {
                String bearer = properties.getBearer();
                if (bearer.isBlank()) {
                    throw IntegrationGraphException.because(
                            "integration.graph.auth 为 true，但未配置 integration.bearer。",
                            "在配置文件中填写有效的 Bearer Token，或将 integration.graph.auth 设为 false（仅当 WDS 允许匿名访问时）。");
                }
                request = request.header("Authorization", "Bearer " + bearer);
            }
            WdsApiResponse<List<NetRelationTreeNode>> response = request
                    .body(FirstNetRelationTreeRequest.DEFAULT)
                    .retrieve()
                    .body(RESPONSE_TYPE);
            validateResponse(response);
            return response.data() == null ? List.of() : response.data();
        } catch (IntegrationGraphException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw IntegrationGraphException.because(
                    "调用 WDS 导航树接口失败：" + ex.getMessage() + "。",
                    "请检查 integration.graph.url 是否可达、网络与防火墙策略，以及鉴权配置是否正确后重试。",
                    ex);
        }
    }

    private static void validateResponse(WdsApiResponse<List<NetRelationTreeNode>> response) {
        if (response == null) {
            throw IntegrationGraphException.because(
                    "WDS 返回了空响应体（HTTP 可能成功但无 JSON 内容）。",
                    "请确认 WDS 接口正常、返回 application/json，并查看网侧服务日志。");
        }
        if (!response.success() || response.code() != SUCCESS_CODE) {
            String message = response.message() == null ? "未知错误" : response.message();
            throw IntegrationGraphException.because(
                    "WDS 业务返回失败：code=" + response.code() + "，message=" + message + "。",
                    "请核对 WDS 服务状态、请求体参数与账号权限；必要时联系网侧管理员确认接口与数据范围。");
        }
    }

}
