package org.sagebionetworks.repo.model.ar;

import java.util.Objects;

/**
 * A user's status of a single access requirement on a single subject. 
 *
 */
public class UsersRequirementStatus {

	private Long requirementId;
	private AccessRequirementType requirementType;
	private boolean isUnmet;	

	/**
	 * 
	 * @param requirementId
	 * @return
	 */
	public UsersRequirementStatus withRequirementId(Long requirementId) {
		this.requirementId = requirementId;
		return this;
	}


	/**
	 * 
	 * @param requirementType
	 * @return
	 */
	public UsersRequirementStatus withRequirementType(AccessRequirementType requirementType) {
		this.requirementType = requirementType;
		return this;
	}


	/**
	 * @param isUnmet the isUnmet to set
	 */
	public UsersRequirementStatus withIsUnmet(boolean isUnmet) {
		this.isUnmet = isUnmet;
		return this;
	}


	/**
	 * @return the requirementId
	 */
	public Long getRequirementId() {
		return requirementId;
	}

	/**
	 * @return the requirementType
	 */
	public AccessRequirementType getRequirementType() {
		return requirementType;
	}

	/**
	 * @return the isUnmet
	 */
	public boolean isUnmet() {
		return isUnmet;
	}

	@Override
	public int hashCode() {
		return Objects.hash(isUnmet, requirementId, requirementType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UsersRequirementStatus)) {
			return false;
		}
		UsersRequirementStatus other = (UsersRequirementStatus) obj;
		return isUnmet == other.isUnmet && Objects.equals(requirementId, other.requirementId)
				&& Objects.equals(requirementType, other.requirementType);
	}

	@Override
	public String toString() {
		return "RequirementStatus [requirementId=" + requirementId + ", requirementType=" + requirementType
				+ ", isUnmet=" + isUnmet + "]";
	}
	
}
