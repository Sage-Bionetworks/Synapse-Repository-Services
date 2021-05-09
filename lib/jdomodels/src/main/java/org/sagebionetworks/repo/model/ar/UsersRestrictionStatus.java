package org.sagebionetworks.repo.model.ar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.RestrictionLevel;

/**
 * The user's access restriction status for a single subject. This object
 * includes information on all access restrictions on a subject and the user's
 * status for each restriction.
 *
 */
public class UsersRestrictionStatus {

	private Long subjectId;
	private Long userId;
	private List<UsersRequirementStatus> accessRestrictions;
	private boolean hasUnmet;

	public UsersRestrictionStatus(Long subjectId, Long userId) {
		this.subjectId = subjectId;
		this.userId = userId;
		this.accessRestrictions = new ArrayList<UsersRequirementStatus>();
		this.hasUnmet = false;
	}

	public void addRestrictionStatus(UsersRequirementStatus toAdd) {
		this.accessRestrictions.add(toAdd);
	}

	/**
	 * @return the hasUnmet True if the user has unmet access restrictions for this
	 *         subject.
	 */
	public boolean hasUnmet() {
		return hasUnmet;
	}

	/**
	 * @param hasUnmet True if the user has unmet access restrictions for this
	 */
	public void setHasUnmet(boolean hasUnmet) {
		this.hasUnmet = hasUnmet;
	}

	/**
	 * @return the subjectId
	 */
	public Long getSubjectId() {
		return subjectId;
	}

	/**
	 * @return the userId
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * @return the accessRestrictions
	 */
	public List<UsersRequirementStatus> getAccessRestrictions() {
		return accessRestrictions;
	}

	/**
	 * Get the most restrictive level of all of the requirements on this object.
	 * 
	 * @return
	 */
	public RestrictionLevel getMostRestrictiveLevel() {
		RestrictionLevel level = RestrictionLevel.OPEN;
		if (accessRestrictions != null) {
			for (UsersRequirementStatus status : accessRestrictions) {
				RestrictionLevel localLevel = status.getRequirementType().getRestrictionLevel();
				if (RestrictionLevelComparator.SINGLETON.compare(localLevel, level) > 0) {
					level = localLevel;
				}
			}
		}
		return level;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessRestrictions, hasUnmet, subjectId, userId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UsersRestrictionStatus)) {
			return false;
		}
		UsersRestrictionStatus other = (UsersRestrictionStatus) obj;
		return Objects.equals(accessRestrictions, other.accessRestrictions) && hasUnmet == other.hasUnmet
				&& Objects.equals(subjectId, other.subjectId) && Objects.equals(userId, other.userId);
	}

	@Override
	public String toString() {
		return "SubjectStatus [subjectId=" + subjectId + ", userId=" + userId + ", accessRestrictions="
				+ accessRestrictions + ", hasUnmet=" + hasUnmet + "]";
	}

}
