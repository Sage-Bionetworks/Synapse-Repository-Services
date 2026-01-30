package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroup;

public class UserGroupTestUtils {

	public static UserGroup createUser() {
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		user.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		return user;
	}

	public static UserGroup createGroup() {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		group.setRealmId(AuthorizationConstants.DEFAULT_REALM_ID);
		return group;
	}
}
