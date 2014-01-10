package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.principal.NewUserUtils;
import org.sagebionetworks.repo.manager.principal.UserProfileUtillity;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
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
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	/**
	 * Testing purposes only
	 * Do NOT use in non-test code
	 * i.e. {@link #createUser(UserInfo, String, UserProfile, DBOCredential)}
	 */
	@Autowired
	private DBOBasicDao basicDAO;
	
	public UserManagerImpl() { }
	
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
		// First validate and trim the new user
		NewUserUtils.validateAndTrim(user);
		// Determine if the email already exists
		PrincipalAlias alias = principalAliasDAO.findPrincipalWithAlias(user.getEmail());
		if (alias != null) {
			throw new NameConflictException("User '" + user.getEmail() + "' already exists");
		}
		// Check the username
		alias = principalAliasDAO.findPrincipalWithAlias(user.getUserName());
		if (alias != null) {
			throw new NameConflictException("User '" + user.getUserName() + "' already exists");
		}
		
		UserGroup individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		Long id;
		try {
			id = userGroupDAO.create(individualGroup);
			individualGroup = userGroupDAO.get(id);
		} catch (NotFoundException ime) {
			throw new DatastoreException(ime);
		}
		// Bind the email to this user.
		alias = new PrincipalAlias();
		alias.setAlias(user.getEmail());
		alias.setIsValidated(false);
		alias.setPrincipalId(id);
		alias.setType(AliasType.USER_EMAIL);
		// bind this alias
		try {
			principalAliasDAO.bindAliasToPrincipal(alias);
		} catch (NotFoundException e1) {
			throw new DatastoreException(e1);
		}
		// bind the username to this user
		alias = new PrincipalAlias();
		alias.setAlias(user.getUserName());
		alias.setIsValidated(true);
		alias.setPrincipalId(id);
		alias.setType(AliasType.USER_NAME);
		try {
			principalAliasDAO.bindAliasToPrincipal(alias);
		} catch (NotFoundException e1) {
			throw new DatastoreException(e1);
		}
		
		// Make some credentials for this user
		Long principalId = Long.parseLong(individualGroup.getId());
		authDAO.createNew(principalId);
		
		// Create a new user profile.
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(individualGroup.getId());
		userProfile.setFirstName(user.getFirstName());
		userProfile.setLastName(user.getLastName());
		userProfile.setDisplayName(NewUserUtils.createDisplayName(user));
		try {
			userProfileDAO.create(userProfile);
		} catch (InvalidModelException e) {
			throw new RuntimeException(e);
		}
		
		return principalId;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserInfo createUser(UserInfo adminUserInfo, NewUser user, DBOCredential credential) throws NotFoundException {
		if (!adminUserInfo.isAdmin()) {
			throw new UnauthorizedException("Must be an admin to use this service");
		}
		// Create the user
		Long principalId = createUser(user);
		
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
	public UserInfo getUserInfo(Long principalId) throws NotFoundException {
		UserGroup principal = userGroupDAO.get(principalId);
		// Lookup the user's name
		// Check which group(s) of Anonymous, Public, or Authenticated the user belongs to  
		Set<Long> groups = new HashSet<Long>();
		boolean isUserAnonymous = AuthorizationUtils.isUserAnonymous(principalId);
		// Everyone except the anonymous users belongs to "authenticated users"
		if (!isUserAnonymous) {
			// All authenticated users belong to the authenticated user group
			groups.add(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId());
		}
		
		// Everyone belongs to their own group and to Public
		groups.add(principalId);
		groups.add(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId());
		// Add all groups the user belongs to
		List<UserGroup> groupFromDAO = groupMembersDAO.getUsersGroups(principal.getId());
		// Add each group
		for(UserGroup ug: groupFromDAO){
			groups.add(Long.parseLong(ug.getId()));
		}

		// Check to see if the user is an Admin
		boolean isAdmin = false;
		// If the user belongs to the admin group they are an admin
		if(groups.contains(BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId())){
			isAdmin = true;
		}
		UserInfo ui = new UserInfo(isAdmin);
		
		if (isUserAnonymous) {
			// Anonymous users have not accepted the ToC.
			ui.setAgreesToTermsOfUse(false);
		}else{
			// Lookup the Toc status.
			ui.setAgreesToTermsOfUse(authDAO.hasUserAcceptedToU(principalId));
		}
		ui.setCreationDate(principal.getCreationDate());
		// Put all the pieces together
		ui.setGroups(groups);
		return ui;
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
	public Collection<UserGroup> getGroups() throws DatastoreException {
		return userGroupDAO.getAll(false);
	}

	@Override
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) 
			throws DatastoreException, UnauthorizedException {
		return userGroupDAO.getInRange(startIncl, endExcl, false);
	}

	@Override
	public String getUserName(long userId) {
		List<PrincipalAlias> aliases = this.principalAliasDAO.listPrincipalAliases(userId, AliasType.USER_NAME);
		if(aliases.size() < 1){
			// Use a temporary name composed of their ID until this users sets their username I
			return UserProfileUtillity.createTempoaryUserName(userId);
		}else{
			// Use the first name
			return aliases.get(0).getAlias();
		}
	}

	@Override
	public PrincipalAlias lookupPrincipalByAlias(String alias) {
		return this.principalAliasDAO.findPrincipalWithAlias(alias);
	}

	@Override
	public PrincipalAlias bindOpenIDToPrincipal(Long principalId, String OpenId) throws DatastoreException, NotFoundException {
		// First validate the ID belongs to a user
		UserGroup ug = this.userGroupDAO.get(principalId);
		if(!ug.getIsIndividual()) throw new IllegalArgumentException("Cannot bind an OpenId to a team/group");
		// Bind it
		// Bind the email to this user.
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(OpenId);
		alias.setIsValidated(true);
		alias.setPrincipalId(principalId);
		alias.setType(AliasType.USER_OPEN_ID);
		// Bind it.
		return this.principalAliasDAO.bindAliasToPrincipal(alias);
	}
	

}
