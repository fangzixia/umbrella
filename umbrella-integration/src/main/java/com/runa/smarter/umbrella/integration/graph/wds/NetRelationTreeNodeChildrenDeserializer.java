package com.runa.smarter.umbrella.integration.graph.wds;

import java.util.List;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.type.TypeFactory;

/** WDS 叶子节点可能返回 {@code "children": ""} 而非空数组。 */
public final class NetRelationTreeNodeChildrenDeserializer extends ValueDeserializer<List<NetRelationTreeNode>> {

    @Override
    public List<NetRelationTreeNode> deserialize(JsonParser parser, DeserializationContext context) {
        JsonToken token = parser.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            return List.of();
        }
        if (token == JsonToken.START_ARRAY) {
            var type = TypeFactory.createDefaultInstance().constructCollectionType(List.class, NetRelationTreeNode.class);
            return context.readValue(parser, type);
        }
        if (token == JsonToken.VALUE_NULL) {
            return List.of();
        }
        return List.of();
    }

}
