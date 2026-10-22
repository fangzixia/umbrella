package com.runa.smarter.umbrella.tools;

import com.runa.smarter.umbrella.graph.GraphNode;
import com.runa.smarter.umbrella.tools.capability.NodeCapabilityTools;
import com.runa.smarter.umbrella.tools.custom.CustomAgentTools;
import com.runa.smarter.umbrella.tools.master.GraphIntrospectionTools;
import com.runa.smarter.umbrella.tools.master.MasterGraphRefreshTools;
import com.runa.smarter.umbrella.tools.messaging.AgentMessagingTools;
import io.agentscope.core.tool.Toolkit;
import org.springframework.stereotype.Service;

/** 为 runtime 组装 AgentScope {@link Toolkit}。 */
@Service
public class AgentToolkitFactory {

    private final CustomAgentTools customAgentTools;
    private final GraphIntrospectionTools graphIntrospectionTools;
    private final AgentMessagingTools agentMessagingTools;
    private final MasterGraphRefreshTools masterGraphRefreshTools;

    public AgentToolkitFactory(
            CustomAgentTools customAgentTools,
            GraphIntrospectionTools graphIntrospectionTools,
            AgentMessagingTools agentMessagingTools,
            MasterGraphRefreshTools masterGraphRefreshTools) {
        this.customAgentTools = customAgentTools;
        this.graphIntrospectionTools = graphIntrospectionTools;
        this.agentMessagingTools = agentMessagingTools;
        this.masterGraphRefreshTools = masterGraphRefreshTools;
    }

    /** 演示 Agent：注册 {@code tools.custom} 包中声明的自定义工具。 */
    public Toolkit createDemoToolkit() {
        Toolkit toolkit = new Toolkit();
        customAgentTools.registerAll(toolkit);
        return toolkit;
    }

    /** 图谱节点 Harness Agent：能力 + 图关联 + send_to_agent。 */
    public NodeAgentToolkit createNodeFleetToolkit(GraphNode node) {
        NodeCapabilityTools capabilityTools = new NodeCapabilityTools(node);
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(graphIntrospectionTools);
        toolkit.registerTool(agentMessagingTools);
        if (!node.capabilities().isEmpty()) {
            toolkit.registerTool(capabilityTools);
        }
        return new NodeAgentToolkit(toolkit, capabilityTools.hasInvokableCapability(), true);
    }

    /** 总控 Worker。 */
    public Toolkit createMasterToolkit() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(graphIntrospectionTools);
        toolkit.registerTool(agentMessagingTools);
        toolkit.registerTool(masterGraphRefreshTools);
        return toolkit;
    }

    /** 节点 / Worker 使用的工具包及是否包含可执行能力。 */
    public record NodeAgentToolkit(Toolkit toolkit, boolean hasInvokableCapabilities, boolean toolsEnabled) {
    }
}
