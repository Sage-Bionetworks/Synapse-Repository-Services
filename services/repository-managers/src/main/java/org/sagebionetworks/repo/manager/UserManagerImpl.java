package org.sagebionetworks.repo.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.xpath.XPathExpressionException;

import org.sagebionetworks.authutil.AuthenticationException;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.util.UserGroupUtil;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class UserManagerImpl implements UserManager {
	
	@Autowired
	UserDAO userDAO;
	@Autowired
	UserGroupDAO userGroupDAO;	
	@Autowired
	UserProfileDAO userProfileDAO;
	@Autowired
	GroupMembersDAO groupMembersDAO;

	public void setUserGroupDAO(UserGroupDAO userGroupDAO) {
		this.userGroupDAO = userGroupDAO;
	}
	
	// adds the existing groups to the 'groups' collection passed in
	// returns true iff the user 'userName' is an administrator
	private boolean addGroups(String userName, Collection<UserGroup> groups) throws NotFoundException, DatastoreException {
		UserGroup user = userGroupDAO.findGroup(userName, true);
		if (user == null) {
			// Non-existent user cannot be an admin
			return false;
		}
		
		Collection<UserGroup> usersGroups = groupMembersDAO.getUsersGroups(user.getId());
		// Filter out bad group names
		usersGroups = filterInvalidGroupNames(usersGroups);
		groups.addAll(usersGroups);
		
		boolean isAdmin = false;
		for (UserGroup group : usersGroups) {
			if (AuthorizationConstants.ADMIN_GROUP_NAME.equals(group.getName())) {
				isAdmin = true;
				continue;
			}
		}
		return isAdmin;
	}
	
	private UserGroup createIndividualGroup(String userName, User user) throws DatastoreException {
		UserGroup individualGroup = new UserGroup();
		individualGroup.setName(userName);
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		try {
			String id = userGroupDAO.create(individualGroup);
			individualGroup = userGroupDAO.get(id);
		} catch (NotFoundException ime) {
			// shouldn't happen!
			throw new DatastoreException(ime);
		} catch (InvalidModelException ime) {
			// shouldn't happen!
			throw new DatastoreException(ime);
		}
		// we also make a user profile for this individual
		UserProfile userProfile = null;
		try {
			userProfile = userProfileDAO.get(individualGroup.getId());
		} catch (NotFoundException nfe) {
			userProfile = null;
		}
		if (userProfile==null) {
			userProfile = new UserProfile();
			userProfile.setOwnerId(individualGroup.getId());
			userProfile.setFirstName(user.getFname());
			userProfile.setLastName(user.getLname());
			userProfile.setDisplayName(user.getDisplayName());
			try {
				userProfileDAO.create(userProfile);
			} catch (InvalidModelException e) {
				throw new RuntimeException(e);
			}
		}
		return individualGroup;
	}

	/**
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
		// Always fetch current info on Groups
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
			if (user == null) {
				throw new NullPointerException("No user named " + userName);
			}
			individualGroup = userGroupDAO.findGroup(userName, true);
			if (individualGroup == null) {
				individualGroup = createIndividualGroup(userName, user);
			}
			isAdmin = addGroups(userName, groups);
			// All authenticated users belong to the public group and the
			// authenticated user group.
			groups.add(getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS));
			groups.add(getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC));
		}
		groups.add(individualGroup);
		
		// Put all the pieces together
		UserInfo ui = new UserInfo(isAdmin);
		ui.setIndividualGroup(individualGroup);
		ui.setUser(user);
		ui.setGroups(groups);
		return ui;
	}
	
	/**
	 * Filter out any group name that is invalid
	 * @param groupNames
	 * @return
	 */
	public static Collection<UserGroup> filterInvalidGroupNames(Collection<UserGroup> groupNames){
		ArrayList<UserGroup> newList = new ArrayList<UserGroup>();
		Iterator<UserGroup> it = groupNames.iterator();
		while(it.hasNext()){
			UserGroup group = it.next();
			// Filter out any name that is an email address.
			if(!UserGroupUtil.isEmailAddress(group.getName())){
				newList.add(group);
			}
		}
		return newList;
	}

	/**
	 * Lazy fetch of the default groups.
	 * 
	 * @param group
	 * @return
	 * @throws DatastoreException
	 */
	@Override
	public UserGroup getDefaultUserGroup(DEFAULT_GROUPS group)
			throws DatastoreException {
		UserGroup ug = userGroupDAO.findGroup(group.name(), false);
		if (ug == null)
			throw new DatastoreException(group + " should exist.");
		return ug;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteUser(String id) throws DatastoreException, NotFoundException {
		userDAO.delete(id);
	}

	@Override
	public UserGroup findGroup(String name, boolean b) throws DatastoreException {
		return userGroupDAO.findGroup(name, b);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String createPrincipal(String name, boolean isIndividual) throws DatastoreException {
		UserGroup principal = new UserGroup();
		principal.setName(name);
		principal.setIsIndividual(isIndividual);
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

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean deletePrincipal(String name) {
		return userGroupDAO.deletePrincipal(name);
	}
	
	/**
	 * @param principalId
	 * @return for a group, returns the group name, for a user returns the display name in the user's profile
	 */
	@Override
	public String getDisplayName(Long principalId) throws NotFoundException, DatastoreException {
		UserGroup userGroup = userGroupDAO.get(principalId.toString());
		if (userGroup.getIsIndividual()) {
			UserProfile userProfile = userProfileDAO.get(principalId.toString());
			return userProfile.getDisplayName();
		} else {
			return userGroup.getName();
		}
	}
	
	@Override
	public void updateEmail(UserInfo userInfo, String newEmail) throws DatastoreException, NotFoundException, IOException, AuthenticationException, XPathExpressionException {
		if (userInfo != null) {
			UserGroup userGroup = userGroupDAO.get(userInfo.getIndividualGroup().getId());
			userGroup.setName(newEmail);
			userGroupDAO.update(userGroup);
		}
	}

	@Override
	public Collection<UserGroup> getGroups() throws DatastoreException {
		List<String> groupsToOmit = new ArrayList<String>();
		groupsToOmit.add(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME);
		return userGroupDAO.getAllExcept(false, groupsToOmit);
	}

	@Override
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) throws DatastoreException, UnauthorizedException {
		List<String> groupsToOmit = new ArrayList<String>();
		groupsToOmit.add(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME);
		return userGroupDAO.getInRangeExcept(startIncl, endExcl, false, groupsToOmit);
	}
}
