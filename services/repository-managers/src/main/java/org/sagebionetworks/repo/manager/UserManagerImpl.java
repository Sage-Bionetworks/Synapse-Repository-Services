package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.DEFAULT_GROUPS;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class UserManagerImpl implements UserManager {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private GroupMembersDAO groupMembersDAO;
	
	@Autowired
	private AuthenticationDAO authDAO;
	

	public void setUserGroupDAO(UserGroupDAO userGroupDAO) {
		this.userGroupDAO = userGroupDAO;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void createUser(NewUser user) throws DatastoreException {
		UserGroup individualGroup = new UserGroup();
		individualGroup.setName(user.getEmail());
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
		
		// Make a user profile for this individual
		UserProfile userProfile = null;
		try {
			userProfile = userProfileDAO.get(individualGroup.getId());
		} catch (NotFoundException nfe) {
			userProfile = null;
		}
		if (userProfile==null) {
			userProfile = new UserProfile();
			userProfile.setOwnerId(individualGroup.getId());
			userProfile.setFirstName(user.getFirstName());
			userProfile.setLastName(user.getLastName());
			userProfile.setDisplayName(user.getDisplayName());
			try {
				userProfileDAO.create(userProfile);
			} catch (InvalidModelException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserInfo getUserInfo(String userName) throws DatastoreException,
			NotFoundException {		
		User user = getUser(userName);
		UserGroup individualGroup = userGroupDAO.findGroup(userName, true);
		
		// Check which group(s) of Anonymous, Public, or Authenticated the user belongs to  
		Set<UserGroup> groups = new HashSet<UserGroup>();
		if (!AuthorizationConstants.ANONYMOUS_USER_ID.equals(userName)) {
			// All authenticated users belong to the authenticated user group
			groups.add(getDefaultUserGroup(DEFAULT_GROUPS.AUTHENTICATED_USERS));
		}
		
		// Everyone belongs to their own group and to Public
		groups.add(individualGroup);
		groups.add(getDefaultUserGroup(DEFAULT_GROUPS.PUBLIC));
		
		// Add all groups the user belongs to
		groups.addAll(groupMembersDAO.getUsersGroups(individualGroup.getId()));

		// Check to see if the user is an Admin
		boolean isAdmin = false;
		for (UserGroup group : groups) {
			if (AuthorizationConstants.ADMIN_GROUP_NAME.equals(group.getName())) {
				isAdmin = true;
				break;
		}
	}
	
		// Put all the pieces together
		UserInfo ui = new UserInfo(isAdmin);
		ui.setIndividualGroup(individualGroup);
		ui.setUser(user);
		ui.setGroups(groups);
		return ui;
	}
	
	/**
	 * Constructs a User object out of information from the UserGroup and UserProfile
	 */
	private User getUser(String userName) throws DatastoreException,
			NotFoundException {
		User user = new User();
		user.setUserId(userName);
		user.setId(userName); // i.e. username == user id

		if (AuthorizationConstants.ANONYMOUS_USER_ID.equals(userName)) {
			return user;
		}

		UserGroup ug = userGroupDAO.findGroup(userName, true);
		if (ug == null) {
			throw new NotFoundException("User " + userName + " does not exist");
		}
		user.setCreationDate(ug.getCreationDate());

		// The migrator may delete its own profile during testing
		// But those details do not matter for this user
		if (userName.equals(AuthorizationConstants.MIGRATION_USER_NAME)) {
			return user;
		}

		UserProfile up = userProfileDAO.get(ug.getId());
		user.setFname(up.getFirstName());
		user.setLname(up.getLastName());
		user.setDisplayName(up.getDisplayName());
		user.setAgreesToTermsOfUse(up.getAgreesToTermsOfUse() != null
				&& up.getAgreesToTermsOfUse() >= AuthorizationConstants.MOST_RECENT_TERMS_OF_USE);

		return user;
	}

	/**
	 * Lazy fetch of the default groups.
	 */
	@Override
	public UserGroup getDefaultUserGroup(DEFAULT_GROUPS group)
			throws DatastoreException {
		UserGroup ug = userGroupDAO.findGroup(group.name(), false);
		if (ug == null)
			throw new DatastoreException(group + " should exist.");
		return ug;
	}

	@Override
	public UserGroup findGroup(String name, boolean b) throws DatastoreException {
		return userGroupDAO.findGroup(name, b);
	}

	@Override
	public boolean doesPrincipalExist(String name) {
		return userGroupDAO.doesPrincipalExist(name);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public boolean deletePrincipal(String name) {
		return userGroupDAO.deletePrincipal(name);
	}
	
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
	public String getGroupName(String principalId) throws NotFoundException {
		UserGroup userGroup = userGroupDAO.get(principalId);
		return userGroup.getName();
	}
	
	@Override
	public void updateEmail(UserInfo userInfo, String newEmail) throws DatastoreException, NotFoundException {
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
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) 
			throws DatastoreException, UnauthorizedException {
		List<String> groupsToOmit = new ArrayList<String>();
		groupsToOmit.add(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME);
		return userGroupDAO.getInRangeExcept(startIncl, endExcl, false, groupsToOmit);
	}
}
