package org.sagebionetworks.repo.model.dbo.migration;

import java.util.Objects;

public class DBOSubjectAccessRequirementBackup implements BackupObject {
	private Long nodeId; // this is the same as subjectId, but only for Nodes.  from DBONodeAccessRequirement
	private Long subjectId; // this is the generalized ID for any subject.  from DBOSubjectAccessRequirement
	private String subjectType; // this is the subject type.  from DBOSubjectAccessRequirement
	private Long accessRequirementId; // this field is common to DBONodeAccessRequirement and DBOSubjectAccessRequirement
	private String bindingType;

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

	public String getBindingType() {
		return bindingType;
	}

	public void setBindingType(String bindingType) {
		this.bindingType = bindingType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessRequirementId, bindingType, nodeId, subjectId, subjectType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DBOSubjectAccessRequirementBackup)) {
			return false;
		}
		DBOSubjectAccessRequirementBackup other = (DBOSubjectAccessRequirementBackup) obj;
		return Objects.equals(accessRequirementId, other.accessRequirementId)
				&& Objects.equals(bindingType, other.bindingType) && Objects.equals(nodeId, other.nodeId)
				&& Objects.equals(subjectId, other.subjectId) && Objects.equals(subjectType, other.subjectType);
	}

	@Override
	public String toString() {
		return "DBOSubjectAccessRequirementBackup [nodeId=" + nodeId + ", subjectId=" + subjectId + ", subjectType="
				+ subjectType + ", accessRequirementId=" + accessRequirementId + ", bindingType=" + bindingType + "]";
	}

	
}
