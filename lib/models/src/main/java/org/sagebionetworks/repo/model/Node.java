package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.Map;
import java.util.Set;

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
	Long versionNumber;
	String versionComment;
	String versionLabel;
	Map<String, Set<Reference>> references;
			
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

	public Long getVersionNumber() {
		return versionNumber;
	}
	public void setVersionNumber(Long versionNumber) {
		this.versionNumber = versionNumber;
	}
	public String getVersionComment() {
		return versionComment;
	}
	public void setVersionComment(String versionComment) {
		this.versionComment = versionComment;
	}
	public String getVersionLabel() {
		return versionLabel;
	}
	public void setVersionLabel(String versionLabel) {
		this.versionLabel = versionLabel;
	}
	/**
	 * @return the references
	 */
	public Map<String, Set<Reference>> getReferences() {
		return references;
	}
	/**
	 * @param references the references to set
	 */
	public void setReferences(Map<String, Set<Reference>> references) {
		this.references = references;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
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
				+ ((nodeType == null) ? 0 : nodeType.hashCode());
		result = prime * result
				+ ((parentId == null) ? 0 : parentId.hashCode());
		result = prime * result
				+ ((references == null) ? 0 : references.hashCode());
		result = prime * result
				+ ((versionComment == null) ? 0 : versionComment.hashCode());
		result = prime * result
				+ ((versionLabel == null) ? 0 : versionLabel.hashCode());
		result = prime * result
				+ ((versionNumber == null) ? 0 : versionNumber.hashCode());
		return result;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
		if (nodeType == null) {
			if (other.nodeType != null)
				return false;
		} else if (!nodeType.equals(other.nodeType))
			return false;
		if (parentId == null) {
			if (other.parentId != null)
				return false;
		} else if (!parentId.equals(other.parentId))
			return false;
		if (references == null) {
			if (other.references != null)
				return false;
		} else if (!references.equals(other.references))
			return false;
		if (versionComment == null) {
			if (other.versionComment != null)
				return false;
		} else if (!versionComment.equals(other.versionComment))
			return false;
		if (versionLabel == null) {
			if (other.versionLabel != null)
				return false;
		} else if (!versionLabel.equals(other.versionLabel))
			return false;
		if (versionNumber == null) {
			if (other.versionNumber != null)
				return false;
		} else if (!versionNumber.equals(other.versionNumber))
			return false;
		return true;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Node [createdBy=" + createdBy + ", createdOn=" + createdOn
				+ ", description=" + description + ", eTag=" + eTag + ", id="
				+ id + ", modifiedBy=" + modifiedBy + ", modifiedOn="
				+ modifiedOn + ", name=" + name + ", nodeType=" + nodeType
				+ ", parentId=" + parentId + ", references=" + references
				+ ", versionComment=" + versionComment + ", versionLabel="
				+ versionLabel + ", versionNumber=" + versionNumber + "]";
	}

}
