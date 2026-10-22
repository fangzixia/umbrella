package com.runa.smarter.umbrella.integration.graph.wds;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.annotation.JsonDeserialize;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NetRelationTreeNode(
        String id,
        String name,
        int type,
        long objectId,
        boolean permitted,
        @JsonDeserialize(using = NetRelationTreeNodeChildrenDeserializer.class) List<NetRelationTreeNode> children) {

    public NetRelationTreeNode {
        children = children == null ? List.of() : List.copyOf(children);
    }

}
