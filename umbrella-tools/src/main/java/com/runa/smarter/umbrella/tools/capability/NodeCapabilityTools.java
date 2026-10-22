package com.runa.smarter.umbrella.tools.capability;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runa.smarter.umbrella.graph.BusinessErrorMessage;
import com.runa.smarter.umbrella.graph.CapabilityInvokeRequest;
import com.runa.smarter.umbrella.graph.GraphNode;
import com.runa.smarter.umbrella.graph.NodeCapability;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 将当前图谱节点上已声明的 {@link NodeCapability} 暴露为 AgentScope 工具。
 * 供节点 Agent 查询能力列表或执行带 invoker 的能力。
 */
public final class NodeCapabilityTools {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final String nodeId;
    private final List<NodeCapability> capabilities;

    public NodeCapabilityTools(GraphNode node) {
        this.nodeId = node.id().value();
        this.capabilities = List.copyOf(node.capabilities());
    }

    @Tool(
            name = "list_node_capabilities",
            description = "列出当前图谱节点已声明的能力，并标明每项是否可执行。",
            readOnly = true,
            concurrencySafe = true)
    public String listNodeCapabilities() {
        if (capabilities.isEmpty()) {
            return BusinessErrorMessage.format(
                    "节点「" + nodeId + "」未声明任何 NodeCapability。",
                    "请在构图时为该节点配置能力，或选择其他节点。");
        }
        StringBuilder builder = new StringBuilder("节点 ").append(nodeId).append(" 的能力列表：\n");
        for (NodeCapability capability : capabilities) {
            builder.append("- ")
                    .append(capability.name())
                    .append(capability.invokable() ? "（可执行）" : "（仅描述）")
                    .append("：")
                    .append(capability.description())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    @Tool(
            name = "invoke_node_capability",
            description = "执行当前节点上可 invoker 的能力；arguments_json 为可选的字符串键值 JSON 对象。")
    public String invokeNodeCapability(
            @ToolParam(name = "capability_name", description = "能力名称，须与 list_node_capabilities 结果一致") String capabilityName,
            @ToolParam(name = "arguments_json", description = "可选，JSON 对象，例如 {\"key\":\"value\"}") String argumentsJson) {
        if (capabilityName == null || capabilityName.isBlank()) {
            return BusinessErrorMessage.format(
                    "未提供 capability_name 参数。",
                    "请先调用 list_node_capabilities，再传入要执行的能力名称。");
        }
        Optional<NodeCapability> match = capabilities.stream()
                .filter(capability -> capability.name().equals(capabilityName.strip()))
                .findFirst();
        if (match.isEmpty()) {
            return BusinessErrorMessage.format(
                    "当前节点「" + nodeId + "」上不存在名为「" + capabilityName + "」的能力。",
                    "请用 list_node_capabilities 核对名称拼写，或确认该能力是否挂在当前选中节点上。");
        }
        NodeCapability capability = match.get();
        if (!capability.invokable()) {
            return "该能力不可执行（仅描述）: " + capability.name();
        }
        try {
            Object result = capability.invoke(new CapabilityInvokeRequest(parseArguments(argumentsJson)));
            return result == null ? "执行成功" : String.valueOf(result);
        } catch (IllegalArgumentException ex) {
            return ex.getMessage();
        } catch (UnsupportedOperationException ex) {
            return ex.getMessage();
        } catch (RuntimeException ex) {
            return BusinessErrorMessage.format(
                    "执行能力「" + capability.name() + "」时发生错误：" + ex.getMessage() + "。",
                    "请检查传入的 arguments_json 与能力实现要求；修正参数后重试，或联系能力提供方排查。");
        }
    }

    /** 当前节点是否至少有一项可执行能力。 */
    public boolean hasInvokableCapability() {
        return capabilities.stream().anyMatch(NodeCapability::invokable);
    }

    private static Map<String, String> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, String> parsed = JSON.readValue(argumentsJson.strip(), STRING_MAP);
            return Map.copyOf(parsed);
        } catch (Exception ex) {
            throw new IllegalArgumentException(BusinessErrorMessage.format(
                    "arguments_json 不是合法的字符串键值 JSON 对象：" + ex.getMessage() + "。",
                    "请传入形如 {\"key\":\"value\"} 的 JSON；不需要参数时可省略或传空。"));
        }
    }

}
