package org.sagebionetworks.repo.model;

/**
 * The ID and type of a node.
 *
 */
public class NodeIdAndType {

	String nodeId;
	EntityType type;
	
	/**
	 * Create with ID and type.
	 * 
	 * @param nodeId
	 * @param type
	 */
	public NodeIdAndType(String nodeId, EntityType type) {
		super();
		this.nodeId = nodeId;
		this.type = type;
	}
	
	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	public EntityType getType() {
		return type;
	}
	public void setType(EntityType type) {
		this.type = type;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		NodeIdAndType other = (NodeIdAndType) obj;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "NodeIdAndType [nodeId=" + nodeId + ", type=" + type + "]";
	}

}
