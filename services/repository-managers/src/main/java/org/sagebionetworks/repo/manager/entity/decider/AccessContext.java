package org.sagebionetworks.repo.manager.entity.decider;

import java.util.Objects;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.UsersRestrictionStatus;
import org.sagebionetworks.repo.model.auth.TwoFactorState;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;

/**
 * Information provided to a decider to make an authorization decision.
 *
 */
public class AccessContext {

	private UserInfo user;
	private UserEntityPermissionsState permissionsState;
	private UsersRestrictionStatus restrictionStatus;
	private TwoFactorState twoFactorAuthState;
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
	 * @return
	 */
	public TwoFactorState getTwoFactorAuthState() {
		return twoFactorAuthState;
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
	 * @param twoFactorAuthState
	 * @return
	 */
	public AccessContext withTwoFactorAuthState(TwoFactorState twoFactorAuthState) {
		this.twoFactorAuthState = twoFactorAuthState;
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
		return Objects.hash(accessType, entityCreateType, permissionsState, restrictionStatus, twoFactorAuthState, user);
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
		return accessType == other.accessType && entityCreateType == other.entityCreateType
				&& Objects.equals(permissionsState, other.permissionsState) && Objects.equals(restrictionStatus, other.restrictionStatus)
				&& twoFactorAuthState == other.twoFactorAuthState && Objects.equals(user, other.user);
	}

	@Override
	public String toString() {
		return "AccessContext [user=" + user + ", permissionsState=" + permissionsState + ", restrictionStatus=" + restrictionStatus
				+ ", twoFactorAuthState=" + twoFactorAuthState + ", accessType=" + accessType + ", entityCreateType=" + entityCreateType
				+ "]";
	}
	
}
