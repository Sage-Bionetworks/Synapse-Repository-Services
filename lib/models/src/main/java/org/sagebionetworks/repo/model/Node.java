package org.sagebionetworks.repo.model;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This is the DTO of a node.
 * @author jmhill
 *
 */
public class Node {

	private String id;
	private String name;
	private String description;
	private String parentId;
	private Long createdByPrincipalId;
	private Date createdOn;
	private Long modifiedByPrincipalId;
	private Date modifiedOn;
	private String nodeType;
	private String eTag;
	private Long versionNumber;
	private String versionComment;
	private String versionLabel;
	private String benefactorId;
	private String projectId;
	private Map<String, Set<Reference>> references;
	private String activityId;
	private String fileHandleId;
	private List<String> columnModelIds;

	public Long getCreatedByPrincipalId() {
		return createdByPrincipalId;
	}
	/*
	 * This is used for the sole purpose of telling NodeDAO not to 
	 * used the values form this object to overwrite the existing 
	 * values, e.g. during Update
	 */
	public void clearNodeCreationData() {
		this.createdByPrincipalId = null;
		this.createdOn = null;
	}
	
	public void setCreatedByPrincipalId(Long createdBy) {
		if(createdBy == null) throw new IllegalArgumentException("Cannot set a Node CreatedByPrincipalId to null");
		this.createdByPrincipalId = createdBy;
	}
	public Date getCreatedOn() {
		return createdOn;
	}
	public void setCreatedOn(Date createdOn) {
		if(createdOn == null) throw new IllegalArgumentException("Cannot set a Node CreatedOn to null");
		this.createdOn = createdOn;
	}
	public Long getModifiedByPrincipalId() {
		return modifiedByPrincipalId;
	}
	public void setModifiedByPrincipalId(Long modifiedBy) {
		if(modifiedBy == null) throw new IllegalArgumentException("Cannot set a Node ModifiedByPrincipalId to null");
		this.modifiedByPrincipalId = modifiedBy;
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
	
	/**
	 * The benefactor is the node that this node inherits its ACL from.
	 * @return
	 */
	public String getBenefactorId() {
		return benefactorId;
	}
	/**
	 * The benefactor is the node that this node inherits its ACL from.
	 * @param benefactorId
	 */
	public void setBenefactorId(String benefactorId) {
		this.benefactorId = benefactorId;
	}

	public String getProjectId() {
		return projectId;
	}
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getActivityId() {
		return activityId;
	}
	public void setActivityId(String activityId) {
		this.activityId = activityId;
	}
	
	public String getFileHandleId() {
		return fileHandleId;
	}
	public void setFileHandleId(String fileHandleId) {
		this.fileHandleId = fileHandleId;
	}
	
	public List<String> getColumnModelIds() {
		return columnModelIds;
	}
	public void setColumnModelIds(List<String> columnModelIds) {
		this.columnModelIds = columnModelIds;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((activityId == null) ? 0 : activityId.hashCode());
		result = prime * result
				+ ((benefactorId == null) ? 0 : benefactorId.hashCode());
		result = prime * result
				+ ((projectId == null) ? 0 : projectId.hashCode());
		result = prime * result
				+ ((columnModelIds == null) ? 0 : columnModelIds.hashCode());
		result = prime
				* result
				+ ((createdByPrincipalId == null) ? 0 : createdByPrincipalId
						.hashCode());
		result = prime * result
				+ ((createdOn == null) ? 0 : createdOn.hashCode());
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((eTag == null) ? 0 : eTag.hashCode());
		result = prime * result
				+ ((fileHandleId == null) ? 0 : fileHandleId.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime
				* result
				+ ((modifiedByPrincipalId == null) ? 0 : modifiedByPrincipalId
						.hashCode());
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
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (activityId == null) {
			if (other.activityId != null)
				return false;
		} else if (!activityId.equals(other.activityId))
			return false;
		if (benefactorId == null) {
			if (other.benefactorId != null)
				return false;
		} else if (!benefactorId.equals(other.benefactorId))
			return false;
		if (projectId == null) {
			if (other.projectId != null)
				return false;
		} else if (!projectId.equals(other.projectId))
			return false;
		if (columnModelIds == null) {
			if (other.columnModelIds != null)
				return false;
		} else if (!columnModelIds.equals(other.columnModelIds))
			return false;
		if (createdByPrincipalId == null) {
			if (other.createdByPrincipalId != null)
				return false;
		} else if (!createdByPrincipalId.equals(other.createdByPrincipalId))
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
		if (fileHandleId == null) {
			if (other.fileHandleId != null)
				return false;
		} else if (!fileHandleId.equals(other.fileHandleId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (modifiedByPrincipalId == null) {
			if (other.modifiedByPrincipalId != null)
				return false;
		} else if (!modifiedByPrincipalId.equals(other.modifiedByPrincipalId))
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

	@Override
	public String toString() {
		return "Node [id=" + id + ", name=" + name + ", description="
				+ description + ", parentId=" + parentId
				+ ", createdByPrincipalId=" + createdByPrincipalId
				+ ", createdOn=" + createdOn + ", modifiedByPrincipalId="
				+ modifiedByPrincipalId + ", modifiedOn=" + modifiedOn
				+ ", nodeType=" + nodeType + ", eTag=" + eTag
				+ ", versionNumber=" + versionNumber + ", versionComment="
				+ versionComment + ", versionLabel=" + versionLabel
				+ ", benefactorId=" + benefactorId + ", projectId=" + projectId + ", references="
				+ references + ", activityId=" + activityId + ", fileHandleId="
				+ fileHandleId + "]";
	}
}
