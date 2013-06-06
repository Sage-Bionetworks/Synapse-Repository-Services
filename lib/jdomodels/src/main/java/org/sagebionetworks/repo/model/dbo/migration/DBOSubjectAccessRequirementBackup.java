package org.sagebionetworks.repo.model.dbo.migration;

public class DBOSubjectAccessRequirementBackup implements BackupObject {
	private Long nodeId; // this is the same as subjectId, but only for Nodes.  from DBONodeAccessRequirement
	private Long subjectId; // this is the generalized ID for any subject.  from DBOSubjectAccessRequirement
	private String subjectType; // this is the subject type.  from DBOSubjectAccessRequirement
	private Long accessRequirementId; // this field is common to DBONodeAccessRequirement and DBOSubjectAccessRequirement

	@Override
	public String getImmutableAlias() {
		return "DBOSubjectAccessRequirementBackup";
	}

	public Long getNodeId() {
		return nodeId;
	}

	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}

	public Long getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(Long subjectId) {
		this.subjectId = subjectId;
	}

	public String getSubjectType() {
		return subjectType;
	}

	public void setSubjectType(String subjectType) {
		this.subjectType = subjectType;
	}

	public Long getAccessRequirementId() {
		return accessRequirementId;
	}

	public void setAccessRequirementId(Long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((accessRequirementId == null) ? 0 : accessRequirementId
						.hashCode());
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		result = prime * result
				+ ((subjectId == null) ? 0 : subjectId.hashCode());
		result = prime * result
				+ ((subjectType == null) ? 0 : subjectType.hashCode());
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
		DBOSubjectAccessRequirementBackup other = (DBOSubjectAccessRequirementBackup) obj;
		if (accessRequirementId == null) {
			if (other.accessRequirementId != null)
				return false;
		} else if (!accessRequirementId.equals(other.accessRequirementId))
			return false;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		if (subjectId == null) {
			if (other.subjectId != null)
				return false;
		} else if (!subjectId.equals(other.subjectId))
			return false;
		if (subjectType == null) {
			if (other.subjectType != null)
				return false;
		} else if (!subjectType.equals(other.subjectType))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBOSubjectAccessRequirementBackup [nodeId=" + nodeId
				+ ", subjectId=" + subjectId + ", subjectType=" + subjectType
				+ ", accessRequirementId=" + accessRequirementId + "]";
	}

	
}
