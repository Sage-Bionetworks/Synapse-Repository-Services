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
	boolean entityExists;
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
		this.entityExists= context.getPermissionsState().doesEntityExist();
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

	public boolean doesEntityExist() {
		return entityExists;
	}

	public void setEntityExists(boolean entityExists) {
		this.entityExists = entityExists;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessRestrictions == null) ? 0 : accessRestrictions.hashCode());
		result = prime * result + ((authorizationStatus == null) ? 0 : authorizationStatus.hashCode());
		result = prime * result + ((benefactorId == null) ? 0 : benefactorId.hashCode());
		result = prime * result + (entityExists ? 1231 : 1237);
		result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
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
		UsersEntityAccessInfo other = (UsersEntityAccessInfo) obj;
		if (accessRestrictions == null) {
			if (other.accessRestrictions != null)
				return false;
		} else if (!accessRestrictions.equals(other.accessRestrictions))
			return false;
		if (authorizationStatus == null) {
			if (other.authorizationStatus != null)
				return false;
		} else if (!authorizationStatus.equals(other.authorizationStatus))
			return false;
		if (benefactorId == null) {
			if (other.benefactorId != null)
				return false;
		} else if (!benefactorId.equals(other.benefactorId))
			return false;
		if (entityExists != other.entityExists)
			return false;
		if (entityId == null) {
			if (other.entityId != null)
				return false;
		} else if (!entityId.equals(other.entityId))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "UsersEntityAccessInfo [entityId=" + entityId + ", authroizationStatus=" + authorizationStatus
				+ ", accessRestrictions=" + accessRestrictions + "]";
	}

}
