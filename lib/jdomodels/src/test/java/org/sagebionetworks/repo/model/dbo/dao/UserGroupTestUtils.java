package org.sagebionetworks.repo.model.dbo.dao;

import org.sagebionetworks.repo.model.UserGroup;

public class UserGroupTestUtils {

	public static UserGroup createUser() {
		UserGroup user = new UserGroup();
		user.setIsIndividual(true);
		return user;
	}

	public static UserGroup createGroup() {
		UserGroup group = new UserGroup();
		group.setIsIndividual(false);
		return group;
	}
}
