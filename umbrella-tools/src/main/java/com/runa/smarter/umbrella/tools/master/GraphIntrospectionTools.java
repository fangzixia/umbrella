package com.runa.smarter.umbrella.tools.master;

import com.runa.smarter.umbrella.graph.BusinessErrorMessage;
import com.runa.smarter.umbrella.graph.GraphNode;
import com.runa.smarter.umbrella.graph.GraphRegistry;
import com.runa.smarter.umbrella.graph.GraphView;
import com.runa.smarter.umbrella.graph.model.NodeId;
import com.runa.smarter.umbrella.tools.messaging.AgentToolRuntimeBridge;
import com.runa.smarter.umbrella.tools.messaging.AssociatedAgentsPort;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.hook.RuntimeContextAware;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** 只读图谱 introspection 与可关联 Agent 列表。 */
@Component
public class GraphIntrospectionTools implements RuntimeContextAware {

    private final GraphRegistry registry;
    private final AssociatedAgentsPort associatedAgentsPort;

    public GraphIntrospectionTools(GraphRegistry registry, AssociatedAgentsPort associatedAgentsPort) {
        this.registry = registry;
        this.associatedAgentsPort = associatedAgentsPort;
    }

    @Override
    public void setRuntimeContext(RuntimeContext runtimeContext) {
        AgentToolRuntimeBridge.bind(runtimeContext);
    }

    @Tool(
            name = "list_graph_nodes",
            description = "列出当前内存图中的节点摘要。",
            readOnly = true,
            concurrencySafe = true)
    public String listGraphNodes(
            @ToolParam(name = "type_filter", description = "可选，精确匹配节点 type，空则不过滤") String typeFilter) {
        if (!registry.initialized()) {
            return notInitializedMessage();
        }
        GraphView graph = registry.graph();
        String filter = typeFilter == null || typeFilter.isBlank() ? null : typeFilter.strip();
        StringBuilder builder = new StringBuilder("图名: ")
                .append(graph.name())
                .append("\n节点列表:\n");
        for (GraphNode node : graph.nodes()) {
            if (filter != null && !node.type().equals(filter)) {
                continue;
            }
            builder.append("- ")
                    .append(node.id().value())
                    .append(" | ")
                    .append(node.displayName())
                    .append(" | type=")
                    .append(node.type())
                    .append(" | 能力数=")
                    .append(node.capabilities().size())
                    .append('\n');
        }
        return builder.toString().trim();
    }

    @Tool(
            name = "get_node_summary",
            description = "返回单个节点的详情与关联边。",
            readOnly = true,
            concurrencySafe = true)
    public String getNodeSummary(
            @ToolParam(name = "node_id", description = "图谱节点 id") String nodeId) {
        if (nodeId == null || nodeId.isBlank()) {
            return BusinessErrorMessage.format("未提供 node_id。", "请传入有效 node_id。");
        }
        if (!registry.initialized()) {
            return notInitializedMessage();
        }
        GraphView graph = registry.graph();
        GraphNode node = graph.findNode(NodeId.of(nodeId.strip()))
                .orElse(null);
        if (node == null) {
            return BusinessErrorMessage.format(
                    "当前内存图谱中找不到节点 id「" + nodeId + "」。",
                    "请刷新图谱后重试。");
        }
        var relations = graph.edges().stream()
                .filter(edge -> edge.source().equals(node.id()) || edge.target().equals(node.id()))
                .map(edge -> edge.source().value() + " --" + edge.relationType() + "--> " + edge.target().value())
                .toList();
        return "nodeId: " + node.id().value()
                + "\n名称: " + node.displayName()
                + "\n类型: " + node.type()
                + "\n属性: " + node.properties().entries()
                + "\n能力: "
                + node.capabilities().stream()
                        .map(c -> c.name() + (c.invokable() ? "（可执行）" : "（仅描述）") + ": " + c.description())
                        .toList()
                + "\n关联: " + relations;
    }

    @Tool(
            name = "list_node_neighbors",
            description = "列出节点的图邻居及边类型。",
            readOnly = true,
            concurrencySafe = true)
    public String listNodeNeighbors(
            @ToolParam(name = "node_id", description = "图谱节点 id") String nodeId,
            @ToolParam(name = "direction", description = "in / out / both，默认 both") String direction) {
        if (nodeId == null || nodeId.isBlank()) {
            return BusinessErrorMessage.format("未提供 node_id。", "请传入有效 node_id。");
        }
        if (!registry.initialized()) {
            return notInitializedMessage();
        }
        GraphView graph = registry.graph();
        NodeId id = NodeId.of(nodeId.strip());
        if (graph.findNode(id).isEmpty()) {
            return BusinessErrorMessage.format(
                    "当前内存图谱中找不到节点 id「" + nodeId + "」。",
                    "请刷新图谱后重试。");
        }
        String dir = direction == null || direction.isBlank() ? "both" : direction.strip().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder("节点 ").append(nodeId).append(" 的邻居:\n");
        graph.edges().forEach(edge -> {
            if ("out".equals(dir) && edge.source().equals(id)) {
                builder.append("- → ")
                        .append(edge.target().value())
                        .append(" (")
                        .append(edge.relationType())
                        .append(")\n");
            } else if ("in".equals(dir) && edge.target().equals(id)) {
                builder.append("- ← ")
                        .append(edge.source().value())
                        .append(" (")
                        .append(edge.relationType())
                        .append(")\n");
            } else if ("both".equals(dir) && (edge.source().equals(id) || edge.target().equals(id))) {
                builder.append("- ")
                        .append(edge.source().value())
                        .append(" --")
                        .append(edge.relationType())
                        .append("--> ")
                        .append(edge.target().value())
                        .append('\n');
            }
        });
        return builder.toString().trim();
    }

    @Tool(
            name = "search_nodes_by_name",
            description = "按 displayName 子串搜索节点。",
            readOnly = true,
            concurrencySafe = true)
    public String searchNodesByName(
            @ToolParam(name = "query", description = "搜索子串") String query,
            @ToolParam(name = "limit", description = "最多返回条数，默认 20，上限 50") Integer limit) {
        if (query == null || query.isBlank()) {
            return BusinessErrorMessage.format("未提供 query。", "请传入非空搜索词。");
        }
        if (!registry.initialized()) {
            return notInitializedMessage();
        }
        int max = limit == null ? 20 : Math.min(50, Math.max(1, limit));
        String needle = query.strip().toLowerCase(Locale.ROOT);
        StringBuilder builder = new StringBuilder("匹配「").append(query).append("」的节点:\n");
        int count = 0;
        for (GraphNode node : registry.graph().nodes()) {
            if (!node.displayName().toLowerCase(Locale.ROOT).contains(needle)) {
                continue;
            }
            builder.append("- ")
                    .append(node.id().value())
                    .append(" | ")
                    .append(node.displayName())
                    .append(" | type=")
                    .append(node.type())
                    .append('\n');
            if (++count >= max) {
                break;
            }
        }
        if (count == 0) {
            return "未找到匹配节点。";
        }
        return builder.toString().trim();
    }

    @Tool(
            name = "list_associated_agents",
            description = "列出当前调用方可 send_to_agent 的目标 nodeId（含关联理由）。"
                    + " node_id 可选；节点 Agent 省略时默认为自身。",
            readOnly = true,
            concurrencySafe = true)
    public String listAssociatedAgents(
            @ToolParam(name = "node_id", description = "可选；省略时使用当前 Worker 身份") String nodeId) {
        return associatedAgentsPort.listAssociatedAgents(nodeId);
    }

    private static String notInitializedMessage() {
        return BusinessErrorMessage.format(
                "内存图谱尚未初始化。",
                "请配置 integration.graph.url 后重启，或调用 refresh_graph / 管理端刷新图谱。");
    }
}
