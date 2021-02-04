package org.sagebionetworks.repo.manager.entity;

import java.util.Objects;
import java.util.Optional;

import org.sagebionetworks.repo.model.ar.SubjectStatus;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.util.ValidateArgument;

/**
 * The final determination of a user's access to an Entity. Includes information
 * about all access restrictions on the entity and the user's current status for
 * each restriction.
 *
 */
public class UsersEntityAccessInfo {

	Long entityId;
	AuthorizationStatus authroizationStatus;
	SubjectStatus accessRestrictions;
	Optional<Boolean> wouldHaveAccesIfCertified;

	public UsersEntityAccessInfo(Long entityId, AuthorizationStatus status) {
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(status, "AuthorizationStatus");
		this.entityId = entityId;
		this.authroizationStatus = status;
		this.wouldHaveAccesIfCertified = Optional.empty();
	}

	/**
	 * @return the entityId
	 */
	public Long getEntityId() {
		return entityId;
	}

	/**
	 * @param entityId the entityId to set
	 */
	public UsersEntityAccessInfo withEntityId(Long entityId) {
		this.entityId = entityId;
		return this;
	}

	/**
	 * @return the authroizationStatus
	 */
	public AuthorizationStatus getAuthroizationStatus() {
		return authroizationStatus;
	}

	/**
	 * @param authroizationStatus the authroizationStatus to set
	 */
	public UsersEntityAccessInfo withAuthroizationStatus(AuthorizationStatus authroizationStatus) {
		this.authroizationStatus = authroizationStatus;
		return this;
	}

	/**
	 * @return the accessRestrictions
	 */
	public SubjectStatus getAccessRestrictions() {
		return accessRestrictions;
	}

	/**
	 * @param accessRestrictions the accessRestrictions to set
	 */
	public UsersEntityAccessInfo withAccessRestrictions(SubjectStatus accessRestrictions) {
		this.accessRestrictions = accessRestrictions;
		return this;
	}

	/**
	 * @return the wouldHaveAccesIfCertified
	 */
	public boolean getWouldHaveAccesIfCertified() {
		if(authroizationStatus.isAuthorized()) {
			return true;
		}else {
			return wouldHaveAccesIfCertified.orElse(false);
		}
	}

	/**
	 * @param wouldHaveAccesIfCertified the wouldHaveAccesIfCertified to set
	 */
	public UsersEntityAccessInfo withWouldHaveAccesIfCertified(Boolean wouldHaveAccesIfCertified) {
		this.wouldHaveAccesIfCertified = Optional.ofNullable(wouldHaveAccesIfCertified);
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessRestrictions, authroizationStatus, entityId, wouldHaveAccesIfCertified);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof UsersEntityAccessInfo)) {
			return false;
		}
		UsersEntityAccessInfo other = (UsersEntityAccessInfo) obj;
		return Objects.equals(accessRestrictions, other.accessRestrictions)
				&& Objects.equals(authroizationStatus, other.authroizationStatus)
				&& Objects.equals(entityId, other.entityId)
				&& Objects.equals(wouldHaveAccesIfCertified, other.wouldHaveAccesIfCertified);
	}

	@Override
	public String toString() {
		return "UsersEntityAccessInfo [entityId=" + entityId + ", authroizationStatus=" + authroizationStatus
				+ ", accessRestrictions=" + accessRestrictions + ", wouldHaveAccesIfCertified="
				+ wouldHaveAccesIfCertified + "]";
	}

}
