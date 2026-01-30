package org.sagebionetworks.repo.model.grid.encoding;

import org.sagebionetworks.repo.model.grid.node.ArrayNode;
import org.sagebionetworks.repo.model.grid.node.ConstantNode;
import org.sagebionetworks.repo.model.grid.node.Node;
import org.sagebionetworks.repo.model.grid.node.ObjectNode;
import org.sagebionetworks.repo.model.grid.node.VectorNode;

/**
 * Provider for obtaining {@link IndexedNodeTypeCodec} instances based on node type.
 * This allows for modular encoder implementations that can be tested in isolation.
 */
public class IndexedNodeEncoderProvider {

	private final IndexedNodeTypeCodec<ConstantNode> constantNodeEncoder;
	private final IndexedNodeTypeCodec<ArrayNode> arrayNodeEncoder;
	private final IndexedNodeTypeCodec<ObjectNode> objectNodeEncoder;
	private final IndexedNodeTypeCodec<VectorNode> vectorNodeEncoder;

	public IndexedNodeEncoderProvider() {
		this.constantNodeEncoder = new IndexedConstantNodeCodec();
		this.arrayNodeEncoder = new IndexedArrayNodeCodec();
		this.objectNodeEncoder = new IndexedObjectNodeCodec();
		this.vectorNodeEncoder = new IndexedVectorNodeCodec();
	}


	/**
	 * Get the encoder for ConstantNode.
	 *
	 * @return the encoder for ConstantNode
	 */
	public IndexedNodeTypeCodec<ConstantNode> getConstantNodeEncoder() {
		return constantNodeEncoder;
	};

	/**
	 * Get the encoder for ArrayNode.
	 *
	 * @return the encoder for ArrayNode
	 */
	public IndexedNodeTypeCodec<ArrayNode> getArrayNodeEncoder() {
		return arrayNodeEncoder;
	};

	/**
	 * Get the encoder for ObjectNode.
	 *
	 * @return the encoder for ObjectNode
	 */
	public IndexedNodeTypeCodec<ObjectNode> getObjectNodeEncoder() {
		return objectNodeEncoder;
	};

	/**
	 * Get the encoder for VectorNode.
	 *
	 * @return the encoder for VectorNode
	 */
	public IndexedNodeTypeCodec<VectorNode> getVectorNodeEncoder() {
		return vectorNodeEncoder;
	};

	/**
	 * Get an encoder for the given node type.
	 *
	 * @param nodeClass the class of the node
	 * @param <T> the node type
	 * @return the appropriate encoder
	 * @throws IllegalArgumentException if no encoder exists for the node type
	 */
	@SuppressWarnings("unchecked")
	public  <T extends Node> IndexedNodeTypeCodec<T> getEncoder(Class<T> nodeClass) {
		if (ConstantNode.class.equals(nodeClass)) {
			return (IndexedNodeTypeCodec<T>) getConstantNodeEncoder();
		} else if (ArrayNode.class.equals(nodeClass)) {
			return (IndexedNodeTypeCodec<T>) getArrayNodeEncoder();
		} else if (ObjectNode.class.equals(nodeClass)) {
			return (IndexedNodeTypeCodec<T>) getObjectNodeEncoder();
		} else if (VectorNode.class.equals(nodeClass)) {
			return (IndexedNodeTypeCodec<T>) getVectorNodeEncoder();
		} else {
			throw new IllegalArgumentException("Unsupported node type: " + nodeClass.getName());
		}
	}
}
