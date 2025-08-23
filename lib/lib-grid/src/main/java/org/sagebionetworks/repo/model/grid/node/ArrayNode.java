package org.sagebionetworks.repo.model.grid.node;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * The Replicated Growable Array (RGA) algorithm is used for all JSON CRDT nodes
 * which implement an ordered lists of values. An ArrayNode represents a single
 * node in the RGA, positioned after the {@link ArrayNode#referenceNodeId}.
 */
public class ArrayNode implements Node {

	private LogicalTimestamp nodeId; // the ID of this node.
	private LogicalTimestamp arrayId; // the ID of the array that this node belongs too.
	private LogicalTimestamp dataId; // the ID of the data contained in this node.
	private LogicalTimestamp referenceNodeId; // the ID of the node that proceeds this node in the array (its parent).
	private Boolean isDeleted;

	@Override
	public LogicalTimestamp getId() {
		return nodeId;
	}

	public LogicalTimestamp getNodeId() {
		return nodeId;
	}

	public ArrayNode setNodeId(LogicalTimestamp nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	public LogicalTimestamp getArrayId() {
		return arrayId;
	}

	public ArrayNode setArrayId(LogicalTimestamp arrayId) {
		this.arrayId = arrayId;
		return this;
	}

	public LogicalTimestamp getDataId() {
		return dataId;
	}

	public ArrayNode setDataId(LogicalTimestamp dataId) {
		this.dataId = dataId;
		return this;
	}

	public LogicalTimestamp getReferenceNodeId() {
		return referenceNodeId;
	}

	public ArrayNode setReferenceNodeId(LogicalTimestamp referenceNodeId) {
		this.referenceNodeId = referenceNodeId;
		return this;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public ArrayNode setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(arrayId, dataId, isDeleted, nodeId, referenceNodeId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArrayNode other = (ArrayNode) obj;
		return Objects.equals(arrayId, other.arrayId) && Objects.equals(dataId, other.dataId)
				&& Objects.equals(isDeleted, other.isDeleted) && Objects.equals(nodeId, other.nodeId)
				&& Objects.equals(referenceNodeId, other.referenceNodeId);
	}

	@Override
	public String toString() {
		return "ArrayNode [nodeId=" + nodeId + ", arrayId=" + arrayId + ", dataId=" + dataId + ", referenceNodeId="
				+ referenceNodeId + ", isDeleted=" + isDeleted + "]";
	}

}
