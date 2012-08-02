package org.sagebionetworks.repo.model;

import java.util.List;

public class AccessRequirementBackup {
	private AccessRequirement accessRequirement;
	private List<AccessApproval> accessApprovals;
	
	public AccessRequirement getAccessRequirement() {
		return accessRequirement;
	}
	public void setAccessRequirement(AccessRequirement accessRequirement) {
		this.accessRequirement = accessRequirement;
	}
	public List<AccessApproval> getAccessApprovals() {
		return accessApprovals;
	}
	public void setAccessApprovals(List<AccessApproval> accessApprovals) {
		this.accessApprovals = accessApprovals;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((accessApprovals == null) ? 0 : accessApprovals.hashCode());
		result = prime
				* result
				+ ((accessRequirement == null) ? 0 : accessRequirement
						.hashCode());
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
		AccessRequirementBackup other = (AccessRequirementBackup) obj;
		if (accessApprovals == null) {
			if (other.accessApprovals != null)
				return false;
		} else if (!accessApprovals.equals(other.accessApprovals))
			return false;
		if (accessRequirement == null) {
			if (other.accessRequirement != null)
				return false;
		} else if (!accessRequirement.equals(other.accessRequirement))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "AccessRequirementBackup [accessRequirement="
				+ accessRequirement + ", accessApprovals=" + accessApprovals
				+ "]";
	}
	
	
}
