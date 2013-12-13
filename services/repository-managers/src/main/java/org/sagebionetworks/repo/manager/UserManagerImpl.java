package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
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
	
	/**
	 * Testing purposes only
	 * Do NOT use in non-test code
	 * i.e. {@link #createUser(UserInfo, String, UserProfile, DBOCredential)}
	 */
	@Autowired
	private DBOBasicDao basicDAO;
	
	public UserManagerImpl(UserGroupDAO userGroupDAO, UserProfileDAO userProfileDAO, GroupMembersDAO groupMembersDAO, AuthenticationDAO authDAO, DBOBasicDao basicDAO) {
		this.userGroupDAO = userGroupDAO;
		this.userProfileDAO = userProfileDAO;
		this.groupMembersDAO = groupMembersDAO;
		this.authDAO = authDAO;
		this.basicDAO = basicDAO;
	}
	
	public void setUserGroupDAO(UserGroupDAO userGroupDAO) {
		this.userGroupDAO = userGroupDAO;
	}
	
	@Override
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public long createUser(NewUser user) {
		if (userGroupDAO.doesPrincipalExist(user.getEmail())) {
			throw new NameConflictException("User '" + user.getEmail() + "' already exists");
		}
		
		UserGroup individualGroup = new UserGroup();
		individualGroup.setName(user.getEmail());
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		try {
			String id = userGroupDAO.create(individualGroup);
			individualGroup = userGroupDAO.get(id);
		} catch (NotFoundException ime) {
			throw new DatastoreException(ime);
		}
		
		// Make some credentials for this user
		Long principalId = Long.parseLong(individualGroup.getId());
		authDAO.createNew(principalId);
		
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
		
		return principalId;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserInfo createUser(UserInfo adminUserInfo, String username, UserProfile profile, DBOCredential credential) throws NotFoundException {
		if (!adminUserInfo.isAdmin()) {
			throw new UnauthorizedException("Must be an admin to use this service");
		}
		
		// Setup the tables as done normally
		NewUser user = new NewUser();
		user.setEmail(username);
		
		Long principalId = createUser(user);
		
		// Update the profile
		if (profile == null) {
			profile = new UserProfile();
		}
		profile.setOwnerId(principalId.toString());
		profile.setEtag(userProfileDAO.get(principalId.toString()).getEtag());
		userProfileDAO.update(profile);
		
		// Update the credentials
		if (credential == null) {
			credential = new DBOCredential();
		}
		credential.setPrincipalId(principalId);
		credential.setSecretKey(HMACUtils.newHMACSHA1Key());
		basicDAO.update(credential);
		
		return getUserInfo(principalId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserInfo getUserInfo(String userName) throws DatastoreException, NotFoundException {
		UserGroup individualGroup = userGroupDAO.findGroup(userName, true);
		if (individualGroup==null) throw new NotFoundException("Cannot find user with name "+userName);
		return getUserInfo(individualGroup);
	}
		
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserInfo getUserInfo(Long principalId) throws DatastoreException, NotFoundException {
		UserGroup individualGroup = userGroupDAO.get(principalId.toString());
		if (!individualGroup.getIsIndividual()) 
			throw new IllegalArgumentException(individualGroup.getName()+" is not an individual group.");
		return getUserInfo(individualGroup);
	}
		
	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	private UserInfo getUserInfo(UserGroup individualGroup) throws DatastoreException, NotFoundException {
		
		// Check which group(s) of Anonymous, Public, or Authenticated the user belongs to  
		Set<UserGroup> groups = new HashSet<UserGroup>();
		if (!AuthorizationUtils.isUserAnonymous(individualGroup)) {
			// All authenticated users belong to the authenticated user group
			groups.add(userGroupDAO.get(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString()));
		}
		
		// Everyone belongs to their own group and to Public
		groups.add(individualGroup);
		groups.add(userGroupDAO.get(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId().toString()));
		
		// Add all groups the user belongs to
		groups.addAll(groupMembersDAO.getUsersGroups(individualGroup.getId()));

		// Check to see if the user is an Admin
		boolean isAdmin = false;
		for (UserGroup group : groups) {
			if (BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId().toString().equals(group.getId())) {
				isAdmin = true;
				break;
			}
		}
	
		// Put all the pieces together
		UserInfo ui = new UserInfo(isAdmin);
		ui.setIndividualGroup(individualGroup);
		ui.setGroups(groups);
		ui.setUser(getUser(individualGroup));
		return ui;
	}
	
	/**
	 * Constructs a User object out of information from the UserGroup and UserProfile
	 */
	private User getUser(UserGroup individualGroup) throws DatastoreException,
			NotFoundException {
		User user = new User();
		user.setUserId(individualGroup.getName());
		user.setId(individualGroup.getName()); // i.e. username == user id

		if (AuthorizationUtils.isUserAnonymous(individualGroup.getName())) {
			return user;
		}

		user.setCreationDate(individualGroup.getCreationDate());
		
		// Get the terms of use acceptance
		user.setAgreesToTermsOfUse(authDAO.hasUserAcceptedToU(individualGroup.getId()));

		// The migrator may delete its own profile during migration
		// But those details do not matter for this user
		if (BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString().equals(individualGroup.getId())) {
			return user;
		}

		UserProfile up = userProfileDAO.get(individualGroup.getId());
		user.setFname(up.getFirstName());
		user.setLname(up.getLastName());
		user.setDisplayName(up.getDisplayName());

		return user;
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
	public void deletePrincipal(UserInfo adminUserInfo, Long principalId) throws NotFoundException {
		if (!adminUserInfo.isAdmin()) {
			throw new UnauthorizedException("Must be an admin to use this service");
		}
		
		userGroupDAO.delete(principalId.toString());
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
		
		// The mapping between usernames and user IDs is currently done on a one-to-one basis.
		// This means that changing the email associated with an ID in the UserGroup table 
		//   introduces an inconsistency between the UserGroup table and ID Generator table.
		// Until the Named ID Generator supports a one-to-many mapping, this method is disabled
		throw new NotFoundException("This service is currently unavailable");
		
		/*
		if (userInfo != null) {
			UserGroup userGroup = userGroupDAO.get(userInfo.getIndividualGroup().getId());
			userGroup.setName(newEmail);
			userGroupDAO.update(userGroup);
		}
		*/
	}

	@Override
	public Collection<UserGroup> getGroups() throws DatastoreException {
		return userGroupDAO.getAll(false);
	}

	@Override
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) 
			throws DatastoreException, UnauthorizedException {
		return userGroupDAO.getInRange(startIncl, endExcl, false);
	}
}
