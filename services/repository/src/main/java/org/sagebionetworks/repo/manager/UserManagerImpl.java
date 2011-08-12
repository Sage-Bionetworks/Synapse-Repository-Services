package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class UserManagerImpl implements UserManager {
	@Autowired
	UserDAO userDAO;

	@Autowired
	UserGroupDAO userGroupDAO;

	private static Map<DEFAULT_GROUPS, UserGroup> defaultGroups;

	private static Map<String, UserInfo> userInfoCache = null;
	private static Long cacheTimeout = null;
	private static Date lastCacheDump = null;

	public UserManagerImpl() {
		userInfoCache = Collections
				.synchronizedMap(new HashMap<String, UserInfo>());
		defaultGroups = Collections
				.synchronizedMap(new HashMap<DEFAULT_GROUPS, UserGroup>());
		lastCacheDump = new Date();
		String s = System
				.getProperty(AuthUtilConstants.AUTH_CACHE_TIMEOUT_MILLIS);
		if (s != null && s.length() > 0) {
			cacheTimeout = Long.parseLong(s);
		} else {
			cacheTimeout = AuthUtilConstants.AUTH_CACHE_TIMEOUT_DEFAULT;
		}
	}

	// for testing
	public void setUserDAO(UserDAO userDAO) {
		this.userDAO = userDAO;
	}

	public void setUserGroupDAO(UserGroupDAO userGroupDAO) {
		this.userGroupDAO = userGroupDAO;
	}

	// private boolean isAdmin(Collection<UserGroup> userGroups) throws
	// DatastoreException, NotFoundException {
	// UserGroup adminGroup =
	// userGroupDAO.findGroup(AuthorizationConstants.ADMIN_GROUP_NAME, false);
	// for (UserGroup ug: userGroups) if (ug.getId().equals(adminGroup.getId()))
	// return true;
	// return false;
	// }

	/**
	 * 
	 * NOTE: This method has the side effect of creating in the 'permissions'
	 * representation of groups any groups that the UserDAO knows the user to
	 * belong to. That is, the 'truth' about groups is assumed to be in the
	 * system managing 'group memberships' and is mirrored in the system
	 * managing group permissions.
	 */
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserInfo getUserInfo(String userName) throws DatastoreException,
			NotFoundException {
		if (cacheTimeout > 0) { // then use cache
			Date now = new Date();
			if (lastCacheDump.getTime() + cacheTimeout < now.getTime()) {
				clearCache();
			}
			UserInfo ui = userInfoCache.get(userName);
			if (ui != null)
				return ui;
		}
		User user = userDAO.getUser(userName);
		Set<UserGroup> groups = new HashSet<UserGroup>();
		UserGroup individualGroup = null;
		boolean isAdmin = false;
		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userName)) {
			individualGroup = userGroupDAO.findGroup(
					AuthorizationConstants.ANONYMOUS_USER_ID, true);
			if (individualGroup == null)
				throw new DatastoreException(
						AuthorizationConstants.ANONYMOUS_USER_ID
								+ " user should exist.");
			// Anonymous belongs to the public group
			groups.add(getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC));
		} else {
			if (user == null)
				throw new NullPointerException("No user named " + userName
						+ ". Users: " + userDAO.getAll());
			Collection<String> groupNames = userDAO.getUserGroupNames(userName);
			// Filter out bad group names
			groupNames = filterInvalidGroupNames(groupNames);
			// these groups omit the individual group
			Map<String, UserGroup> existingGroups = userGroupDAO
					.getGroupsByNames(groupNames);
			for (String groupName : groupNames) {
				if (AuthorizationConstants.ADMIN_GROUP_NAME.equals(groupName)) {
					isAdmin = true;
					continue;
				}
				UserGroup group = existingGroups.get(groupName);
				if (group != null) {
					groups.add(group);
				} else {
					// the group needs to be created
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
			individualGroup = userGroupDAO.findGroup(userName, true);
			if (individualGroup == null) {
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
			// All authenticated users belong to the public group and the
			// authenticated user group.
			groups.add(getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS));
			groups.add(getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC));
		}
		groups.add(individualGroup);
		UserInfo userInfo = new UserInfo(isAdmin);
		userInfo.setIndividualGroup(individualGroup);
		userInfo.setUser(user);
		userInfo.setGroups(groups);
		if (cacheTimeout > 0) { // then use cache
			userInfoCache.put(userName, userInfo);
		}
		return userInfo;
	}
	
	/**
	 * Filter out any group name that is invalid
	 * @param groupNames
	 * @return
	 */
	public static Collection<String> filterInvalidGroupNames(Collection<String> groupNames){
		ArrayList<String> newList = new ArrayList<String>();
		Iterator<String> it = groupNames.iterator();
		while(it.hasNext()){
			String name = it.next();
			// Filter out any name that is an email address.
			if(!UserGroup.isEmailAddress(name)){
				newList.add(name);
			}
		}
		return newList;
	}

	/**
	 * Clear the user cache.
	 */
	private void clearCache() {
		userInfoCache.clear();
		lastCacheDump = new Date();
	}

	/**
	 * Lazy fetch of the default groups.
	 * 
	 * @param group
	 * @return
	 * @throws DatastoreException
	 */
	@Transactional(readOnly = true)
	@Override
	public UserGroup getDefaultUserGroup(DEFAULT_GROUPS group)
			throws DatastoreException {
		UserGroup ug = defaultGroups.get(group);
		if (ug == null) {
			ug = userGroupDAO.findGroup(group.name(), false);
			defaultGroups.put(group, ug);
		}
		if (ug == null)
			throw new DatastoreException(group + " should exist.");
		return ug;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteUser(String id) throws DatastoreException, NotFoundException {
		// Clear the cache when we delete a users.
		clearCache();
		userDAO.delete(id);

	}

	@Transactional(readOnly = true)
	@Override
	public UserGroup findGroup(String name, boolean b) throws DatastoreException {
		return userGroupDAO.findGroup(name, b);
	}

	@Override
	public String createPrincipal(String name, boolean isIndividual) throws DatastoreException {
		UserGroup principal = new UserGroup();
		principal.setName(name);
		principal.setIndividual(isIndividual);
		principal.setCreationDate(new Date());
		try {
			return userGroupDAO.create(principal);
		} catch (InvalidModelException e) {
			throw new DatastoreException(e);
		}
	}

	/**
	 * Does a principal exist with this name?
	 */
	@Override
	public boolean doesPrincipalExist(String name) {
		return userGroupDAO.doesPrincipalExist(name);
	}

	@Override
	public boolean deletePrincipal(String name) {
		clearCache();
		return userGroupDAO.deletePrincipal(name);
	}

}
