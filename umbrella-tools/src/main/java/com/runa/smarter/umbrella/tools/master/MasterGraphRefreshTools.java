package com.runa.smarter.umbrella.tools.master;

import com.runa.smarter.umbrella.tools.messaging.GraphFleetRefreshPort;
import io.agentscope.core.tool.Tool;
import org.springframework.stereotype.Component;

/** 总控专用：刷新图谱并重建 Agent Worker 群。 */
@Component
public class MasterGraphRefreshTools {

    private final GraphFleetRefreshPort graphFleetRefreshPort;

    public MasterGraphRefreshTools(GraphFleetRefreshPort graphFleetRefreshPort) {
        this.graphFleetRefreshPort = graphFleetRefreshPort;
    }

    @Tool(
            name = "refresh_graph",
            description = "从 GraphContributor SPI 重新加载内存图谱，并重建全部常驻节点 Agent Worker。"
                    + " 可能导致旧 nodeId 失效，进行中对话会被中断。")
    public String refreshGraph() {
        return graphFleetRefreshPort.refreshGraphAndFleet();
    }
}
