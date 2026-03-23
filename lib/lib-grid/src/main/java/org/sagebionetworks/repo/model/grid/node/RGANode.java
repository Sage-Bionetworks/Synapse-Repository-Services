package org.sagebionetworks.repo.model.grid.node;

import java.util.Objects;

import org.sagebionetworks.repo.model.grid.patch.LogicalTimestamp;

/**
 * The Replicated Growable Array (RGA) algorithm is used for all JSON CRDT nodes
 * which implement an ordered lists of values. The RGANode represents a single
 * node in the RGA, positioned after the {@link RGANode#refId}.
 */
public class RGANode {

	private LogicalTimestamp containerId; // the ID of the containing node arr, str, or bin node that this node belongs to
	private LogicalTimestamp nodeId; // the ID of the RGA Node
	private LogicalTimestamp dataId; // the ID of the data contained in this node.
	private LogicalTimestamp refId; // the ID of the node that precedes this node in the array (its parent).
	private boolean isDeleted = false;

	public LogicalTimestamp getId() {
		return nodeId;
	}

	public LogicalTimestamp getNodeId() {
		return nodeId;
	}

	public RGANode setNodeId(LogicalTimestamp nodeId) {
		this.nodeId = nodeId;
		return this;
	}

	/**
	 * @return the ID of the containing node arr, str, or bin node that this node belongs to
	 */
	public LogicalTimestamp getContainerId() {
		return containerId;
	}

	public RGANode setContainerId(LogicalTimestamp containerId) {
		this.containerId = containerId;
		return this;
	}

	/**
	 * @return the ID of the data contained in this node.
	 */
	public LogicalTimestamp getDataId() {
		return dataId;
	}

	public RGANode setDataId(LogicalTimestamp dataId) {
		this.dataId = dataId;
		return this;
	}

	/**
	 * @return the ID of the node that precedes this node in the array (its parent).
	 */
	public LogicalTimestamp getReferenceNodeId() {
		return refId;
	}

	public RGANode setReferenceNodeId(LogicalTimestamp refId) {
		this.refId = refId;
		return this;
	}

	public Boolean getIsDeleted() {
		return isDeleted;
	}

	public RGANode setIsDeleted(Boolean isDeleted) {
		this.isDeleted = isDeleted;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeId, dataId, isDeleted, refId, containerId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RGANode other = (RGANode) obj;
		return Objects.equals(nodeId, other.nodeId) && Objects.equals(dataId, other.dataId)
				&& Objects.equals(isDeleted, other.isDeleted) && Objects.equals(refId, other.refId)
				&& Objects.equals(containerId, other.containerId);
	}

	@Override
	public String toString() {
		return "RGANode [nodeId=" + nodeId + ", dataId=" + dataId + ", refId=" + refId + ", isDeleted=" + isDeleted +
				", containerId=" + containerId + "]";
	}

}
