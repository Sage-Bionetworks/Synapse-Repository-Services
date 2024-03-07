package org.sagebionetworks.repo.manager.entity.decider;

import java.util.Objects;

import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.UserRestrictionStatusWithHasUnmet;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;

/**
 * Information provided to a decider to make an authorization decision.
 *
 */
public class AccessContext {

	private UserInfo user;
	private UserEntityPermissionsState permissionsState;
	private UserRestrictionStatusWithHasUnmet restrictionStatusWithHasUnmet;
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
	 * @param restrictionStatusWithHasUnmet the restrictionStatus to set
	 */
	public AccessContext withUserRestrictionStatusWithHasUnmet(UserRestrictionStatusWithHasUnmet restrictionStatusWithHasUnmet){
		this.restrictionStatusWithHasUnmet = restrictionStatusWithHasUnmet;
		return this;
	}

	/**
	 * @return the restrictionStatusWithHasUnmet
	 */
	public UserRestrictionStatusWithHasUnmet getRestrictionStatusWithHasUnmet() {
		return restrictionStatusWithHasUnmet;
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
		return Objects.hash(accessType, entityCreateType, permissionsState, restrictionStatusWithHasUnmet, user);
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
				&& Objects.equals(permissionsState, other.permissionsState)
				&& Objects.equals(restrictionStatusWithHasUnmet, other.restrictionStatusWithHasUnmet)
				&& Objects.equals(user, other.user);
	}

	@Override
	public String toString() {
		return "AccessContext [user=" + user + ", permissionsState=" + permissionsState
				+ ",restrictionStatusWithHasUnmet=" + restrictionStatusWithHasUnmet
				+ ", accessType=" + accessType + ", entityCreateType=" + entityCreateType
				+ "]";
	}

}
