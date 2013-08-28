package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.dao.DBOGroupMembersDAOImpl;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Mocks how the UserManager retrieves UserGroups.
 * Should be used in conjunction with TestUserDAO (the interception of getUsersGroups was originally in the TestUserDAO)
 */
public class TestGroupMembersDAO extends DBOGroupMembersDAOImpl {

	@Autowired
	private UserGroupDAO userGroupDAO;

	@Override
	public List<UserGroup> getUsersGroups(String principalId)
			throws DatastoreException {
		UserGroup user = null;
		try {
			user = userGroupDAO.get(principalId);
		} catch (NotFoundException e) {
			throw new DatastoreException(e);
		}
		List<UserGroup> groups = new ArrayList<UserGroup>();
		
		if (TestUserDAO.ADMIN_USER_NAME.equals(user.getName())) {
			groups.add(getOrCreateGroup(AuthorizationConstants.ADMIN_GROUP_NAME));
		} else if (TestUserDAO.MIGRATION_USER_NAME.equals(user.getName())) {
			groups.add(getOrCreateGroup(AuthorizationConstants.ADMIN_GROUP_NAME));
		}  else if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(user.getName())) {
			// not in any group
		} else {
			groups.add(getOrCreateGroup(TestUserDAO.TEST_GROUP_NAME));
		}
		return groups;
		
	}
	
	private UserGroup getOrCreateGroup(String name) {
		if (!userGroupDAO.doesPrincipalExist(name)) {
			UserGroup group = new UserGroup();
			group.setName(name);
			group.setIsIndividual(false);
			group.setCreationDate(new Date());
			userGroupDAO.create(group);
		}
		return userGroupDAO.findGroup(name, false);
	}
}
