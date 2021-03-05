package org.sagebionetworks.repo.manager.entity.decider;

import java.util.Objects;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;

/**
 * Information provided to a decider to make an authorization decision.
 *
 */
public class AccessContext {

	private UserInfo user;
	private UserEntityPermissionsState permissionsState;
	private UsersRestrictionStatus restrictionStatus;
	private ACCESS_TYPE accessType;
	private EntityType entityCreateType;

	/**
	 * @return the user
	 */
	public UserInfo getUser() {
		return user;
	}

	/**
	 * @return the permissionState
	 */
	public UserEntityPermissionsState getPermissionsState() {
		return permissionsState;
	}

	/**
	 * @return the restrictionStatus
	 */
	public UsersRestrictionStatus getRestrictionStatus() {
		return restrictionStatus;
	}

	/**
	 * @param user the user to set
	 */
	public AccessContext withUser(UserInfo user) {
		this.user = user;
		return this;
	}

	/**
	 * @param permissionsState the permissionState to set
	 */
	public AccessContext withPermissionsState(UserEntityPermissionsState permissionsState) {
		this.permissionsState = permissionsState;
		return this;
	}

	/**
	 * @param restrictionStatus the restrictionStatus to set
	 */
	public AccessContext withRestrictionStatus(UsersRestrictionStatus restrictionStatus) {
		this.restrictionStatus = restrictionStatus;
		return this;
	}
	
	/**
	 * @return the accessType
	 */
	public ACCESS_TYPE getAccessType() {
		return accessType;
	}

	/**
	 * @param accessType the accessType to set
	 */
	public AccessContext withAccessType(ACCESS_TYPE accessType) {
		this.accessType = accessType;
		return this;
	}

	/**
	 * @return the entityCreateType
	 */
	public EntityType getEntityCreateType() {
		return entityCreateType;
	}

	/**
	 * @param entityCreateType the entityCreateType to set
	 */
	public AccessContext withEntityCreateType(EntityType entityCreateType) {
		this.entityCreateType = entityCreateType;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(permissionsState, restrictionStatus, user);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof AccessContext)) {
			return false;
		}
		AccessContext other = (AccessContext) obj;
		return Objects.equals(permissionsState, other.permissionsState)
				&& Objects.equals(restrictionStatus, other.restrictionStatus) && Objects.equals(user, other.user);
	}

	@Override
	public String toString() {
		return "AccessContext [user=" + user + ", permissionState=" + permissionsState + ", restrictionStatus="
				+ restrictionStatus + "]";
	}
	
}
