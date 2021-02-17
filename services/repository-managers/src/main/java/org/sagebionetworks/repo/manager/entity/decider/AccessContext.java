package org.sagebionetworks.repo.manager.entity.decider;

import java.util.Objects;

import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;

/**
 * Information provided to a decider to make an authorization decision.
 *
 */
public class AccessContext {

	private UserInfoState user;
	private UserEntityPermissionsState permissionState;
	private UsersRestrictionStatus restrictionStatus;

	/**
	 * @return the user
	 */
	public UserInfoState getUser() {
		return user;
	}

	/**
	 * @return the permissionState
	 */
	public UserEntityPermissionsState getPermissionState() {
		return permissionState;
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
	public AccessContext withUser(UserInfoState user) {
		this.user = user;
		return this;
	}

	/**
	 * @param permissionState the permissionState to set
	 */
	public AccessContext withPermissionState(UserEntityPermissionsState permissionState) {
		this.permissionState = permissionState;
		return this;
	}

	/**
	 * @param restrictionStatus the restrictionStatus to set
	 */
	public AccessContext withRestrictionStatus(UsersRestrictionStatus restrictionStatus) {
		this.restrictionStatus = restrictionStatus;
		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(permissionState, restrictionStatus, user);
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
		return Objects.equals(permissionState, other.permissionState)
				&& Objects.equals(restrictionStatus, other.restrictionStatus) && Objects.equals(user, other.user);
	}

	@Override
	public String toString() {
		return "AccessContext [user=" + user + ", permissionState=" + permissionState + ", restrictionStatus="
				+ restrictionStatus + "]";
	}
	
}
