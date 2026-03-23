package org.sagebionetworks.repo.model.grid.encoding;

import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.ValueNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;

/**
 * Maps node types to their corresponding binary codes and codecs for indexed encoding.
 */
public enum IndexedNodeCodecMapper {

    CONSTANT(0b000, new IndexedConstantNodeCodec(), ConstantNode.class),
    VAL(0b001, new IndexedValueNodeCodec(), ValueNode.class),
    OBJECT(0b010, new IndexedObjectNodeCodec(), ObjectNode.class),
    VECTOR(0b011, new IndexedVectorNodeCodec(), VectorNode.class),
    ARRAY(0b110, new IndexedArrayNodeCodec(), ArrayNode.class);

    // The binary code that with each node type
    public final int code;
    public final Class<? extends Node> nodeClass;
    public final IndexedNodeTypeCodec<? extends Node> codec;


    IndexedNodeCodecMapper(int code, IndexedNodeTypeCodec<? extends Node> codec, Class<? extends Node> nodeClass) {
        this.code = code;
        this.codec = codec;
        this.nodeClass = nodeClass;
    }

    public static IndexedNodeCodecMapper getByCode(int code) {
        for (IndexedNodeCodecMapper mapper : values()) {
            if (mapper.code == code) {
                return mapper;
            }
        }
        throw new IllegalArgumentException("Unsupported node type code: " + code);
    }

    public static IndexedNodeCodecMapper getByFirstByte(byte firstByte) {
        int code = (firstByte & 0xFF) >> 5;
        return getByCode(code);
    }

    public static IndexedNodeCodecMapper getByNodeClass(Class<? extends Node> nodeClass) {
        for (IndexedNodeCodecMapper mapper : values()) {
            if (mapper.nodeClass.equals(nodeClass)) {
                return mapper;
            }
        }
        throw new IllegalArgumentException("Unsupported node class: " + nodeClass.getName());
    }
}
