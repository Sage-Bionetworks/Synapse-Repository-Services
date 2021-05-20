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
	Long benefactorId;
	AuthorizationStatus authorizationStatus;
	UsersRestrictionStatus accessRestrictions;
	
	public UsersEntityAccessInfo(){}

	public UsersEntityAccessInfo(AccessContext context, AuthorizationStatus status) {
		ValidateArgument.required(context, "context");
		ValidateArgument.required(context.getPermissionsState(),"context.permissionState");
		ValidateArgument.required(context.getPermissionsState().getEntityId(),"context.permissionState.entityId");
		ValidateArgument.required(status, "AuthorizationStatus");
		this.entityId = context.getPermissionsState().getEntityId();
		this.benefactorId = context.getPermissionsState().getBenefactorId();
		this.accessRestrictions = context.getRestrictionStatus();
		this.authorizationStatus = status;
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
	public AuthorizationStatus getAuthorizationStatus() {
		return authorizationStatus;
	}

	/**
	 * @param authroizationStatus the authroizationStatus to set
	 */
	public UsersEntityAccessInfo withAuthorizationStatus(AuthorizationStatus authroizationStatus) {
		this.authorizationStatus = authroizationStatus;
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
	 * @return the benefactorId
	 */
	public Long getBenefactorId() {
		return benefactorId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(accessRestrictions, authorizationStatus, entityId);
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
				&& Objects.equals(authorizationStatus, other.authorizationStatus)
				&& Objects.equals(entityId, other.entityId);
	}

	@Override
	public String toString() {
		return "UsersEntityAccessInfo [entityId=" + entityId + ", authroizationStatus=" + authorizationStatus
				+ ", accessRestrictions=" + accessRestrictions + "]";
	}

}
