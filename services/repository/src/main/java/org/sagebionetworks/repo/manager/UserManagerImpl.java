package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

public class UserManagerImpl implements UserManager {
	@Autowired
	UserDAO userDAO;
		
	@Autowired
	UserGroupDAO userGroupDAO;
	
	private static Map<String,UserInfo> userInfoCache = null;
	private static Long cacheTimeout = null;
	private static Date lastCacheDump = null;
	
	public UserManagerImpl() {
		userInfoCache = Collections.synchronizedMap(new HashMap<String,UserInfo>());
		lastCacheDump = new Date();
		String s = System.getProperty(AuthUtilConstants.AUTH_CACHE_TIMEOUT_MILLIS);
		if (s!=null && s.length()>0) {
			cacheTimeout = Long.parseLong(s);
		} else {
			cacheTimeout = AuthUtilConstants.AUTH_CACHE_TIMEOUT_DEFAULT;
		}
	}
	
	// for testing
	public void setUserDAO(UserDAO userDAO) {this.userDAO=userDAO;}
	public void setUserGroupDAO(UserGroupDAO userGroupDAO) {this.userGroupDAO=userGroupDAO;}

	private boolean isAdmin(Collection<UserGroup> userGroups) throws DatastoreException, NotFoundException {
		UserGroup adminGroup = userGroupDAO.findGroup(AuthorizationConstants.ADMIN_GROUP_NAME, false);
		for (UserGroup ug: userGroups) if (ug.getId().equals(adminGroup.getId())) return true;
		return false;
	}
	
	
	/**
	 *
	 * NOTE:  This method has the side effect of creating in the 'permissions' representation
	 * of groups any groups that the UserDAO knows the user to belong to.  That is,
	 * the 'truth' about groups is assumed to be in the system managing 'group memberships'
	 * and is mirrored in the system managing group permissions.
	 */
	public UserInfo getUserInfo(String userName) throws DatastoreException, NotFoundException {
		if (cacheTimeout>0) { // then use cache
			Date now = new Date();
			if (lastCacheDump.getTime()+cacheTimeout<now.getTime()) {
				userInfoCache.clear();
				lastCacheDump = now;
			}
			UserInfo ui = userInfoCache.get(userName);
			if (ui!=null) return ui;
		}
		User user = userDAO.getUser(userName);
		Set<UserGroup> groups = new HashSet<UserGroup>();
		UserGroup individualGroup = null;
		if (AuthUtilConstants.ANONYMOUS_USER_ID.equals(userName)) {
			individualGroup = userGroupDAO.findGroup(AuthorizationConstants.ANONYMOUS_USER_ID, true);
			if (individualGroup==null) throw new DatastoreException("Anonymous user should exist.");
		} else {
			if (user==null) throw new NullPointerException("No user named "+userName+". Users: "+userDAO.getAll());
			Collection<String> groupNames = userDAO.getUserGroupNames(userName);
			// these groups omit the individual group
			Map<String, UserGroup> existingGroups = userGroupDAO.getGroupsByNames(groupNames);
			for (String groupName : groupNames) {
				UserGroup group = existingGroups.get(groupName);
				if (group!=null) {
					groups.add(group);
				} else {
					// the group needs to be created
					if (groupName.equals(AuthorizationConstants.ADMIN_GROUP_NAME)) {
						throw new IllegalStateException("Admin group should exist in the system.");
					} else {
						group = new UserGroup();
						group.setName(groupName);
						group.setIndividual(false);
						group.setCreationDate(new Date());
						try {
							String id = userGroupDAO.create(group);
							group.setId(id);
						} catch (InvalidModelException ime) {
							// should not happen if our code is written correctly
							throw new RuntimeException(ime);
						}
						groups.add(group);
					}
				}
			}
			individualGroup = userGroupDAO.findGroup(userName, true);
			if (individualGroup==null) {
				individualGroup = new UserGroup();
				individualGroup.setName(userName);
				individualGroup.setIndividual(true);
				individualGroup.setCreationDate(new Date());
				try {
					individualGroup.setId(userGroupDAO.create(individualGroup));
				} catch (InvalidModelException ime) {
					// shouldn't happen!
					throw new DatastoreException(ime);
				}
			}
			UserGroup publicGroup = userGroupDAO.findGroup(AuthorizationConstants.PUBLIC_GROUP_NAME, false);
			if (publicGroup==null) throw new DatastoreException("Public group should exist.");
			groups.add(publicGroup);
		}
		groups.add(individualGroup);
		UserInfo userInfo = new UserInfo(isAdmin(groups));
		userInfo.setIndividualGroup(individualGroup);
		userInfo.setUser(user);
		userInfo.setGroups(groups);
		if (cacheTimeout>0) { // then use cache
			userInfoCache.put(userName, userInfo);
		}
		return userInfo;
	}	
	

	

}
