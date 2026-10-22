package com.runa.smarter.umbrella.runtime.agent.harness;

import com.runa.smarter.umbrella.graph.GraphNode;
import com.runa.smarter.umbrella.graph.GraphView;
import com.runa.smarter.umbrella.runtime.agent.AgentSessions;
import com.runa.smarter.umbrella.runtime.agent.NodeAgentChatService;
import com.runa.smarter.umbrella.tools.AgentToolkitFactory;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.harness.agent.HarnessAgent;
import java.nio.file.Path;
import org.springframework.stereotype.Component;

@Component
public class GraphHarnessAgentFactory {

    private static final Path FLEET_WORKSPACE =
            Path.of(System.getProperty("user.home"), ".umbrella", "fleet-workspace");

    private static final String MASTER_SYS_PROMPT = """
            你是 Umbrella 总控 Agent，负责协调图谱中各常驻节点 Agent。请使用中文回答，除非用户要求其他语言。
            你具备全图认知工具，可通过 send_to_agent 向节点下达任务并等待回复；汇总时请标注信息来源 nodeId。
            无法获得未通过 send_to_agent 确认或未在图中的实时遥测；不得冒充某个节点 Agent。
            发送前请先 list_associated_agents 或 get_node_summary 确认目标；链式寻址时注意深度限制。
            """;

    private final AgentToolkitFactory toolkitFactory;
    private final UmbrellaHarnessRuntime harnessRuntime;
    private final UmbrellaToolContextHook toolContextHook;

    public GraphHarnessAgentFactory(
            AgentToolkitFactory toolkitFactory,
            UmbrellaHarnessRuntime harnessRuntime,
            UmbrellaToolContextHook toolContextHook) {
        this.toolkitFactory = toolkitFactory;
        this.harnessRuntime = harnessRuntime;
        this.toolContextHook = toolContextHook;
    }

    public HarnessAgent createMaster(DashScopeChatModel model) {
        Toolkit toolkit = toolkitFactory.createMasterToolkit();
        return HarnessAgent.builder()
                .name(AgentSessions.MASTER_AGENT_ID)
                .agentId(AgentSessions.MASTER_AGENT_ID)
                .sysPrompt(MASTER_SYS_PROMPT)
                .model(model)
                .toolkit(toolkit)
                .maxIters(8)
                .stateStore(harnessRuntime.stateStore())
                .defaultSessionId(AgentSessions.MASTER)
                .workspace(FLEET_WORKSPACE)
                .hook(toolContextHook)
                .build();
    }

    public ReActAgent createNode(
            GraphNode node,
            GraphView graph,
            DashScopeChatModel model,
            AgentToolkitFactory.NodeAgentToolkit nodeToolkit) {
        String nodeId = node.id().value();
        return ReActAgent.builder()
                .name(nodeId)
                .sysPrompt(NodeAgentChatService.contextFor(
                        graph,
                        node,
                        nodeToolkit.toolsEnabled(),
                        nodeToolkit.hasInvokableCapabilities()))
                .model(model)
                .toolkit(nodeToolkit.toolkit())
                .maxIters(3)
                .stateStore(harnessRuntime.stateStore())
                .defaultSessionId(AgentSessions.forNode(nodeId))
                .hook(toolContextHook)
                .build();
    }
}
