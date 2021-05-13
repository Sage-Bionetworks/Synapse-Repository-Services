package org.sagebionetworks.repo.model.dbo.file.download.v2;

import java.util.Objects;

/**
 * The user must meet the given access requirement in order to download the
 * file.
 *
 */
public class MeetAccessRestriction implements RequiredAction {

	/**
	 * The ID of the un-met access requirement.
	 */
	private long accessRequirementId;

	/**
	 * @return the accessRequirementId
	 */
	public long getAccessRequirementId() {
		return accessRequirementId;
	}

	/**
	 * @param accessRequirementId the accessRequirementId to set
	 */
	public MeetAccessRestriction withAccessRequirementId(long accessRequirementId) {
		this.accessRequirementId = accessRequirementId;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessRequirementId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MeetAccessRestriction)) {
			return false;
		}
		MeetAccessRestriction other = (MeetAccessRestriction) obj;
		return accessRequirementId == other.accessRequirementId;
	}

	@Override
	public String toString() {
		return "MeetAccessRestriction [accessRequirementId=" + accessRequirementId + "]";
	}
	
}
