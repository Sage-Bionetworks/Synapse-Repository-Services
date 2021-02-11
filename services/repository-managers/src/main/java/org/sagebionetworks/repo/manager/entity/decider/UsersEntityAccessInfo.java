package org.sagebionetworks.repo.manager.entity.decider;

import java.util.Objects;

import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
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
	UsersRestrictionStatus accessRestrictions;
	boolean wouldHaveAccesIfCertified;

	public UsersEntityAccessInfo(AccessContext context, AuthorizationStatus status) {
		ValidateArgument.required(context, "context");
		ValidateArgument.required(context.getPermissionState(),"context.getPermissionState");
		ValidateArgument.required(context.getPermissionState().getEntityId(),"context.getPermissionState.entityId");
		ValidateArgument.required(status, "AuthorizationStatus");
		this.entityId = context.getPermissionState().getEntityId();
		this.accessRestrictions = context.getRestrictionStatus();
		this.authroizationStatus = status;
		this.wouldHaveAccesIfCertified = false;
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
	public UsersRestrictionStatus getAccessRestrictions() {
		return accessRestrictions;
	}

	/**
	 * @param accessRestrictions the accessRestrictions to set
	 */
	public UsersEntityAccessInfo withAccessRestrictions(UsersRestrictionStatus accessRestrictions) {
		this.accessRestrictions = accessRestrictions;
		return this;
	}

	/**
	 * @return the wouldHaveAccesIfCertified
	 */
	public boolean getWouldHaveAccesIfCertified() {
		if (authroizationStatus.isAuthorized()) {
			return true;
		} else {
			return wouldHaveAccesIfCertified;
		}
	}

	/**
	 * @param wouldHaveAccesIfCertified the wouldHaveAccesIfCertified to set
	 */
	public UsersEntityAccessInfo withWouldHaveAccesIfCertified(boolean wouldHaveAccesIfCertified) {
		this.wouldHaveAccesIfCertified = wouldHaveAccesIfCertified;
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
