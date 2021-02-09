package org.sagebionetworks.repo.manager.entity;

import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.ar.SubjectStatus;
import org.sagebionetworks.repo.model.dbo.entity.UserEntityPermissionsState;

public class Context {

	private UserInfo user;
	private UserEntityPermissionsState permissionState;
	private SubjectStatus restrictionStatus;

	/**
	 * @return the user
	 */
	public UserInfo getUser() {
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
	public SubjectStatus getRestrictionStatus() {
		return restrictionStatus;
	}
	
	
}
