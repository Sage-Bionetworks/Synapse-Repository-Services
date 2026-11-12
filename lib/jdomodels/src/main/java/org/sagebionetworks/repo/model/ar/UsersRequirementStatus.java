package org.sagebionetworks.repo.model.ar;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.sagebionetworks.repo.model.auth.IdentityProviderName;

/**
 * A user's status of a single access requirement on a single subject. 
 *
 */
public class UsersRequirementStatus {

	private Long requirementId;
	private AccessRequirementType requirementType;
	private boolean isUnmet;
	private boolean isTwoFaRequired;
	private List<IdentityProviderName> identityProviders = Collections.emptyList();
	private boolean isExemptionEligible;

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
	
	public boolean isTwoFaRequired() {
		return isTwoFaRequired;
	}
	
	public UsersRequirementStatus withIsTwoFaRequired(boolean isTwoFaRequired) {
		this.isTwoFaRequired = isTwoFaRequired;
		return this;
	}

	/**
	 * @return The identity providers that the accessor must have authenticated through. Empty if the
	 *         requirement does not restrict how the accessor authenticated. Never null.
	 */
	public List<IdentityProviderName> getIdentityProviders() {
		return identityProviders;
	}

	/**
	 * @param identityProviders Null is treated as empty, since either means the requirement does not
	 *                          restrict how the accessor authenticated.
	 */
	public UsersRequirementStatus withIdentityProviders(List<IdentityProviderName> identityProviders) {
		this.identityProviders = identityProviders == null ? Collections.emptyList() : identityProviders;
		return this;
	}

	/**
	 * @param callersIdentityProvider The identity provider that authenticated the caller, or null if
	 *                                nothing authenticated the caller.
	 * @return True if the caller's identity provider satisfies this requirement's identity provider
	 *         restriction.
	 */
	public boolean isIdentityProviderRequirementMet(IdentityProviderName callersIdentityProvider) {
		if (identityProviders.isEmpty()) {
			return true;
		}
		// Checked explicitly, since an immutable list throws when asked whether it contains null
		return callersIdentityProvider != null && identityProviders.contains(callersIdentityProvider);
	}

	public boolean isExemptionEligible() {
		return isExemptionEligible;
	}

	public UsersRequirementStatus withIsExemptionEligible(boolean isExemptionEligible) {
		this.isExemptionEligible = isExemptionEligible;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(isTwoFaRequired, identityProviders, isUnmet, requirementId, requirementType, isExemptionEligible);
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
		return isTwoFaRequired == other.isTwoFaRequired && Objects.equals(identityProviders, other.identityProviders) && isUnmet == other.isUnmet && Objects.equals(requirementId, other.requirementId)
				&& requirementType == other.requirementType && isExemptionEligible == other.isExemptionEligible;
	}

	@Override
	public String toString() {
		return "UsersRequirementStatus [requirementId=" + requirementId + ", requirementType=" + requirementType + ", isUnmet=" + isUnmet
				+ ", isTwoFaRequired=" + isTwoFaRequired + ", identityProviders=" + identityProviders + ", isExemptionEligible=" + isExemptionEligible + "]";
	}
	
}
