package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.principal.NewUserUtils;
import org.sagebionetworks.repo.model.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DomainType;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.dao.AuthorizationUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
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
	private UserProfileManager userProfileManger;
	
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
	
	public UserManagerImpl(UserGroupDAO userGroupDAO, UserProfileManager userProfileManger, GroupMembersDAO groupMembersDAO, AuthenticationDAO authDAO, DBOBasicDao basicDAO, PrincipalAliasDAO principalAliasDAO) {
		this.userGroupDAO = userGroupDAO;
		this.userProfileManger = userProfileManger;
		this.groupMembersDAO = groupMembersDAO;
		this.authDAO = authDAO;
		this.basicDAO = basicDAO;
		this.principalAliasDAO = principalAliasDAO;
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
		// Make some credentials for this user
		Long principalId = Long.parseLong(individualGroup.getId());
		authDAO.createNew(principalId);
		
		// Create a new user profile.
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(individualGroup.getId());
		userProfile.setFirstName(user.getFirstName());
		userProfile.setLastName(user.getLastName());
		userProfile.setUserName(user.getUserName());
		userProfile.setEmails(new LinkedList<String>());
		userProfile.getEmails().add(user.getEmail());
		userProfileManger.createUserProfile(userProfile);
		
		return principalId;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserInfo createUser(UserInfo adminUserInfo, NewUser user, DBOCredential credential,
			DBOTermsOfUseAgreement touAgreement) throws NotFoundException {
		return createUser(adminUserInfo, user, credential, touAgreement, null);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserInfo createUser(UserInfo adminUserInfo, NewUser user, DBOCredential credential,
			DBOTermsOfUseAgreement touAgreement, DBOSessionToken token) throws NotFoundException {
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
		
		if (touAgreement != null) {
			if (touAgreement.getDomain() == null) {
				throw new IllegalArgumentException("Terms of use cannot be set without a domain specified");
			}
			touAgreement.setPrincipalId(principalId);
			basicDAO.createOrUpdate(touAgreement);
		}
		if (token != null) {
			if (token.getDomain() == null) {
				throw new IllegalArgumentException("Session token cannot be set without a domain specified");
			}
			token.setPrincipalId(principalId);
			basicDAO.createOrUpdate(token);
		}
		return getUserInfo(principalId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public UserInfo getUserInfo(Long principalId) throws NotFoundException {
		UserGroup principal = userGroupDAO.get(principalId);
		if(!principal.getIsIndividual()) throw new IllegalArgumentException("Principal: "+principalId+" is not a User");
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
		ui.setId(principalId);
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
	public PrincipalAlias lookupPrincipalByAlias(String alias) {
		return this.principalAliasDAO.findPrincipalWithAlias(alias);
	}

}
