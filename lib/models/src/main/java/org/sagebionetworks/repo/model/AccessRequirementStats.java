package org.sagebionetworks.repo.model;

import java.util.Set;

public class AccessRequirementStats {

	Set<String> requirementIdSet;
	Boolean hasACT;
	Boolean hasToU;

	public Set<String> getRequirementIdSet() {
		return requirementIdSet;
	}
	public void setRequirementIdSet(Set<String> requirementIdSet) {
		this.requirementIdSet = requirementIdSet;
	}
	public Boolean getHasACT() {
		return hasACT;
	}
	public void setHasACT(Boolean hasACT) {
		this.hasACT = hasACT;
	}
	public Boolean getHasToU() {
		return hasToU;
	}
	public void setHasToU(Boolean hasToU) {
		this.hasToU = hasToU;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hasACT == null) ? 0 : hasACT.hashCode());
		result = prime * result + ((hasToU == null) ? 0 : hasToU.hashCode());
		result = prime * result + ((requirementIdSet == null) ? 0 : requirementIdSet.hashCode());
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
		AccessRequirementStats other = (AccessRequirementStats) obj;
		if (hasACT == null) {
			if (other.hasACT != null)
				return false;
		} else if (!hasACT.equals(other.hasACT))
			return false;
		if (hasToU == null) {
			if (other.hasToU != null)
				return false;
		} else if (!hasToU.equals(other.hasToU))
			return false;
		if (requirementIdSet == null) {
			if (other.requirementIdSet != null)
				return false;
		} else if (!requirementIdSet.equals(other.requirementIdSet))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "AccessRequirementStats [requirementIdSet=" + requirementIdSet + ", hasACT=" + hasACT + ", hasToU="
				+ hasToU + "]";
	}
}
