package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembershipDAO;
import org.sagebionetworks.repo.model.GroupPermissionsDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class UserGroupManagerImpl implements UserGroupManager {
	@Autowired
	UserDAO userDAO;
	
	@Autowired
	GroupMembershipDAO groupMembershipDAO;
	
	@Autowired
	GroupPermissionsDAO groupPermissionsDAO;
	
	// for testing
	public void setUserDAO(UserDAO userDAO) {this.userDAO=userDAO;}
	public void setGroupMembershipDAO(GroupMembershipDAO groupMembershipDAO) {this.groupMembershipDAO=groupMembershipDAO;}

	
	/**
	 *
	 * NOTE:  This method has the side effect of creating in the 'permissions' representation
	 * of groups any groups that the GroupMembershipDAO knows the user to belong to.  That is,
	 * the 'truth' about groups is assumed to be in the system managing 'group memberships'
	 * and is mirrored in the system managing group permissions.
	 */
	public UserInfo getUserInfo(String userName) throws DatastoreException, NotFoundException {
		UserInfo userInfo = new UserInfo();
		User user = null;
		if (AuthUtilConstants.ANONYMOUS_USER_ID.equals(userName)) {
			user = new User();
			user.setUserId(AuthUtilConstants.ANONYMOUS_USER_ID);
		} else {
			user = userDAO.getUser(userName);
			if (user==null) throw new NullPointerException("No user named "+userName+". Users: "+userDAO.getAll());
		}
		userInfo.setUser(user);
		Collection<String> groupNames = null;
		if (AuthUtilConstants.ANONYMOUS_USER_ID.equals(userName)) {
			groupNames = new HashSet<String>();
		} else {
			groupNames = groupMembershipDAO.getUserGroupNames(userName);
		}
		Map<String, UserGroup> existingGroups = groupPermissionsDAO.getGroupsByNames(groupNames);
		Set<UserGroup> groups = new HashSet<UserGroup>();
		for (String groupName : groupNames) {
			UserGroup group = existingGroups.get(groupName);
			if (group!=null) {
				groups.add(group);
			} else {
				// the group needs to be created
				// possibly set other fields
				if (groupName.equals(AuthorizationConstants.ADMIN_GROUP_NAME)) {
					throw new IllegalStateException("Admin group should exist in the system.");
				} else {
					group = new UserGroup();
					group.setName(groupName);
					try {
						String id = groupPermissionsDAO.create(group);
						group.setId(id);
					} catch (InvalidModelException ime) {
						// should not happen if our code is written correctly
						throw new RuntimeException(ime);
					}
					groups.add(group);
				}
			}
		}
		if (!AuthUtilConstants.ANONYMOUS_USER_ID.equals(userName)) {
			UserGroup individualGroup = groupPermissionsDAO.getIndividualGroup(userName);
			if (individualGroup==null) {
				individualGroup = groupPermissionsDAO.createIndividualGroup(userName);
			}
			userInfo.setIndividualGroup(individualGroup);
		}
		groups.add(groupPermissionsDAO.getPublicGroup());
		userInfo.setGroups(groups);
		return userInfo;
	}	
	

	

}
