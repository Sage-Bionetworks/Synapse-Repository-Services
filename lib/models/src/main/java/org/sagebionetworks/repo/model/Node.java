package org.sagebionetworks.repo.model;

import java.util.Date;

/**
 * This is the DTO of a node.
 * @author jmhill
 *
 */
public class Node {
	
	String id;
	String name;
	String description;
	String parentId;
	String createdBy;
	Date createdOn;
	String modifiedBy;
	Date modifiedOn;
	String nodeType;
	String eTag;
			
	public String getCreatedBy() {
		return createdBy;
	}
	public void setCreatedBy(String createdBy) {
		if(createdBy == null) throw new IllegalArgumentException("Cannot set a Node CreatedBy to null");
		this.createdBy = createdBy;
	}
	public Date getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Date createdOn) {
		if(createdOn == null) throw new IllegalArgumentException("Cannot set a Node CreatedOn to null");
		this.createdOn = createdOn;
	}
	public String getModifiedBy() {
		return modifiedBy;
	}
	public void setModifiedBy(String modifiedBy) {
		if(modifiedBy == null) throw new IllegalArgumentException("Cannot set a Node ModifiedBy to null");
		this.modifiedBy = modifiedBy;
	}
	public Date getModifiedOn() {
		return modifiedOn;
	}
	public void setModifiedOn(Date modifiedOn) {
		if(modifiedOn == null) throw new IllegalArgumentException("Cannot set a Node ModifiedOn to null");
		this.modifiedOn = modifiedOn;
	}

	public String getNodeType() {
		return nodeType;
	}

	public void setNodeType(String nodeType) {
		if(nodeType == null) throw new IllegalArgumentException("Cannot set a Node Type to null");
		this.nodeType = nodeType;
	}

	public String getId() {
		return id;
	}
	public void setId(String id) {
		if(id == null) throw new IllegalArgumentException("Cannot set a Node ID to null");
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		if(name == null) throw new IllegalArgumentException("Cannot set a Node Name to null");
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getParentId() {
		return parentId;
	}
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}
	
	public String getETag() {
		return eTag;
	}

	public void setETag(String eTag) {
		if(eTag == null) throw new IllegalArgumentException("Cannot set a Node eTag to null");
		this.eTag = eTag;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((createdBy == null) ? 0 : createdBy.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result
				+ ((modifiedBy == null) ? 0 : modifiedBy.hashCode());
		result = prime * result
				+ ((modifiedOn == null) ? 0 : modifiedOn.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result + ((nodeType == null) ? 0 : nodeType.hashCode());
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
		Node other = (Node) obj;
		if (createdBy == null) {
			if (other.createdBy != null)
				return false;
		} else if (!createdBy.equals(other.createdBy))
			return false;
		if (createdOn == null) {
			if (other.createdOn != null)
				return false;
		} else if (!createdOn.equals(other.createdOn))
			return false;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (eTag == null) {
			if (other.eTag != null)
				return false;
		} else if (!eTag.equals(other.eTag))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modifiedBy == null) {
			if (other.modifiedBy != null)
				return false;
		} else if (!modifiedBy.equals(other.modifiedBy))
			return false;
		if (modifiedOn == null) {
			if (other.modifiedOn != null)
				return false;
		} else if (!modifiedOn.equals(other.modifiedOn))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		if (nodeType == null) {
			if (other.nodeType != null)
				return false;
		} else if (!nodeType.equals(other.nodeType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Node [id=" + id + ", name=" + name + ", description="
				+ description + ", parentId=" + parentId + ", createdBy="
				+ createdBy + ", createdOn=" + createdOn + ", modifiedBy="
				+ modifiedBy + ", modifiedOn=" + modifiedOn + ", type=" + nodeType
				+ ", eTag=" + eTag + "]";
	}

}
