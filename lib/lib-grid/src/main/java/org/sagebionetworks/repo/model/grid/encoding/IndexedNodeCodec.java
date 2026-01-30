package org.sagebionetworks.repo.model.grid.encoding;

import static org.sagebionetworks.repo.model.grid.encoding.IndexedEncodingUtils.NODE_TYPE_ARRAY;
import static org.sagebionetworks.repo.model.grid.encoding.IndexedEncodingUtils.NODE_TYPE_CONSTANT;
import static org.sagebionetworks.repo.model.grid.encoding.IndexedEncodingUtils.NODE_TYPE_OBJECT;
import static org.sagebionetworks.repo.model.grid.encoding.IndexedEncodingUtils.NODE_TYPE_VECTOR;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.sagebionetworks.repo.model.grid.ClockTable;
import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;
import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;
import org.sagebionetworks.util.ValidateArgument;

/**
 * Encodes and decodes JSON CRDT nodes in the Indexed binary format.
 *
 * This class delegates to individual node encoders provided by an {@link IndexedNodeEncoderProvider}.
 *
 * @see <a href="https://jsonjoy.com/specs/json-crdt/encoding/indexed-encoding">Indexed Encoding</a>
 */
public class IndexedNodeCodec implements NodeCodec {
	private final IndexedNodeEncoderProvider encoderProvider;

	public IndexedNodeCodec() {
		this.encoderProvider = new IndexedNodeEncoderProvider();
	}

	@Override
	public int encode(Node node, ClockTable clockTable, OutputStream out) throws IOException {
		ValidateArgument.required(node, "node");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(out, "out");
        if (node.getClass().equals(ConstantNode.class)) {
            return encoderProvider.getConstantNodeEncoder().encode((ConstantNode) node, clockTable, out);
        } else if (node.getClass().equals(ArrayNode.class)) {
            return encoderProvider.getArrayNodeEncoder().encode((ArrayNode) node, clockTable, out);
        } else if (node.getClass().equals(ObjectNode.class)) {
            return encoderProvider.getObjectNodeEncoder().encode((ObjectNode) node, clockTable, out);
        } else if (node.getClass().equals(VectorNode.class)) {
            return encoderProvider.getVectorNodeEncoder().encode((VectorNode) node, clockTable, out);
        }
        throw new IllegalArgumentException("Unsupported node type: " + node.getClass().getName());
    }

	@Override
	public Node decode(LogicalTimestamp nodeId, ClockTable clockTable, InputStream in) throws IOException {
		ValidateArgument.required(nodeId, "nodeId");
		ValidateArgument.required(clockTable, "clockTable");
		ValidateArgument.required(in, "in");

		IndexedNodeHeader typeAndLength = IndexedEncodingUtils.readNodeTypeAndLength(in);
		switch (typeAndLength.getNodeType()) {
			case NODE_TYPE_CONSTANT:
				return encoderProvider.getConstantNodeEncoder().decode(nodeId, typeAndLength.getLength(), clockTable, in);
			case NODE_TYPE_OBJECT:
				return encoderProvider.getObjectNodeEncoder().decode(nodeId, typeAndLength.getLength(), clockTable, in);
			case NODE_TYPE_VECTOR:
				return encoderProvider.getVectorNodeEncoder().decode(nodeId, typeAndLength.getLength(), clockTable, in);
			case NODE_TYPE_ARRAY:
				return encoderProvider.getArrayNodeEncoder().decode(nodeId, typeAndLength.getLength(), clockTable, in);
			default:
				throw new IllegalArgumentException("Unsupported node type: " + typeAndLength.getNodeType());
		}
	}
}
