package com.runa.smarter.umbrella.runtime.agent.policy;

import com.runa.smarter.umbrella.graph.GraphEdge;
import com.runa.smarter.umbrella.graph.GraphNode;
import com.runa.smarter.umbrella.graph.GraphView;
import com.runa.smarter.umbrella.graph.model.NodeId;
import com.runa.smarter.umbrella.runtime.agent.AgentAddress;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 总控可达全图节点；节点仅可达图上有边相连的其它节点。 */
@Component
public class GraphEdgeAssociationPolicy implements AgentAssociationPolicy {

    private static final int MAX_DEPTH = 2;

    @Override
    public int maxDepth() {
        return MAX_DEPTH;
    }

    @Override
    public boolean allows(AgentAddress from, AgentAddress to, GraphView graph) {
        if (to instanceof AgentAddress.Master) {
            return false;
        }
        if (!(to instanceof AgentAddress.Node target)) {
            return false;
        }
        if (graph.findNode(NodeId.of(target.nodeId())).isEmpty()) {
            return false;
        }
        if (from instanceof AgentAddress.Master) {
            return true;
        }
        if (from instanceof AgentAddress.Node self) {
            if (self.nodeId().equals(target.nodeId())) {
                return false;
            }
            return isAdjacent(graph, self.nodeId(), target.nodeId());
        }
        return false;
    }

    @Override
    public String explainDenial(AgentAddress from, AgentAddress to, GraphView graph) {
        if (to instanceof AgentAddress.Master) {
            return "节点 Agent 不可向总控发送 request-reply。";
        }
        if (to instanceof AgentAddress.Node target
                && graph.findNode(NodeId.of(target.nodeId())).isEmpty()) {
            return "目标 nodeId「" + target.nodeId() + "」不在当前图谱中。";
        }
        if (from instanceof AgentAddress.Node self && to instanceof AgentAddress.Node target) {
            if (self.nodeId().equals(target.nodeId())) {
                return "不可向自身 nodeId 发送 request-reply。";
            }
            if (!isAdjacent(graph, self.nodeId(), target.nodeId())) {
                return "节点「" + self.nodeId() + "」与「" + target.nodeId() + "」在图上不相邻。"
                        + " 请先调用 list_associated_agents 查看可联系对象。";
            }
        }
        return "不允许从 " + from.mailboxKey() + " 向 " + to.mailboxKey() + " 发送消息。";
    }

    @Override
    public List<AssociatedAgent> listAssociated(AgentAddress from, GraphView graph) {
        if (from instanceof AgentAddress.Master) {
            return graph.nodes().stream()
                    .map(node -> new AssociatedAgent(
                            node.id().value(),
                            node.displayName(),
                            "总控可编排图中全部节点"))
                    .toList();
        }
        if (from instanceof AgentAddress.Node self) {
            Set<String> neighborIds = new LinkedHashSet<>();
            List<AssociatedAgent> result = new ArrayList<>();
            for (GraphEdge edge : graph.edges()) {
                String selfId = self.nodeId();
                String other = null;
                String reason = null;
                if (edge.source().value().equals(selfId)) {
                    other = edge.target().value();
                    reason = "出边 " + edge.relationType() + " → " + other;
                } else if (edge.target().value().equals(selfId)) {
                    other = edge.source().value();
                    reason = "入边 " + edge.source().value() + " --" + edge.relationType() + "--> " + selfId;
                }
                if (other != null && neighborIds.add(other)) {
                    String displayName = graph.findNode(NodeId.of(other))
                            .map(GraphNode::displayName)
                            .orElse(other);
                    result.add(new AssociatedAgent(other, displayName, reason));
                }
            }
            return result;
        }
        return List.of();
    }

    private static boolean isAdjacent(GraphView graph, String a, String b) {
        return graph.edges().stream().anyMatch(edge ->
                (edge.source().value().equals(a) && edge.target().value().equals(b))
                        || (edge.source().value().equals(b) && edge.target().value().equals(a)));
    }
}
