package org.sagebionetworks.repo.manager;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.principal.NewUserUtils;
import org.sagebionetworks.repo.manager.team.TeamConstants;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.AuthorizationUtils;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSessionToken;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.HMACUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

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
	
	@Autowired
	private NotificationEmailDAO notificationEmailDao;
	
	/**
	 * Testing purposes only
	 * Do NOT use in non-test code
	 * i.e. {@link #createOrGetTestUser(UserInfo, String, UserProfile, DBOCredential)}
	 */
	@Autowired
	private DBOBasicDao basicDAO;

	
	public void setUserGroupDAO(UserGroupDAO userGroupDAO) {
		this.userGroupDAO = userGroupDAO;
	}
	
	@Override
	@WriteTransaction
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
		userProfileManger.createUserProfile(userProfile);
		
		bindAllAliases(user, principalId);
		
		return principalId;
	}
	
	/**
	 * This method is idempotent.
	 * @param profile
	 * @param principalId
	 */
	private void bindAllAliases(NewUser user, Long principalId) {
		// Bind all aliases
		bindAlias(user.getUserName(), AliasType.USER_NAME, principalId);
		PrincipalAlias emailAlias = bindAlias(user.getEmail(), AliasType.USER_EMAIL, principalId);
		notificationEmailDao.create(emailAlias);
	}

	@WriteTransaction
	@Override
	public PrincipalAlias bindAlias(String aliasName, AliasType type, Long principalId) {
		// bind the aliasName to this user
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(aliasName);
		alias.setPrincipalId(principalId);
		alias.setType(type);
		try {
			alias = principalAliasDAO.bindAliasToPrincipal(alias);
		} catch (NotFoundException e1) {
			throw new DatastoreException(e1);
		}
		return alias;
	}
	
	@WriteTransaction
	@Override
	public UserInfo createOrGetTestUser(UserInfo adminUserInfo, NewUser user, boolean acceptsTermsOfUse)
			throws NotFoundException {
		DBOCredential credential = null;
		DBOTermsOfUseAgreement touAgreement = new DBOTermsOfUseAgreement();
		touAgreement.setAgreesToTermsOfUse(acceptsTermsOfUse);
		DBOSessionToken token = null;
		return createOrGetTestUser(adminUserInfo, user, credential, touAgreement, token);
	}

	@WriteTransaction
	@Override
	public UserInfo createOrGetTestUser(UserInfo adminUserInfo, NewUser user, DBOCredential credential,
			DBOTermsOfUseAgreement touAgreement) throws NotFoundException {
		return createOrGetTestUser(adminUserInfo, user, credential, touAgreement, null);
	}

	@WriteTransaction
	@Override
	public UserInfo createOrGetTestUser(UserInfo adminUserInfo, NewUser user, DBOCredential credential,
			DBOTermsOfUseAgreement touAgreement, DBOSessionToken token) throws NotFoundException {
		if (!adminUserInfo.isAdmin()) {
			throw new UnauthorizedException("Must be an admin to use this service");
		}
		// Create the user
		Long principalId;
		PrincipalAlias alias = principalAliasDAO.findPrincipalWithAlias(user.getUserName());
		if (alias==null) {
			principalId = createUser(user);

			// Update the credentials
			if (credential == null) {
				credential = new DBOCredential();
			}
			credential.setPrincipalId(principalId);
			credential.setSecretKey(HMACUtils.newHMACSHA1Key());
			basicDAO.update(credential);

			if (touAgreement != null) {
				touAgreement.setPrincipalId(principalId);
				basicDAO.createOrUpdate(touAgreement);
			}
			if (token != null) {
				// set the session token and record the auth time
				authDAO.changeSessionToken(principalId, token.getSessionToken());
			}
		} else {
			principalId = alias.getPrincipalId();
		}
		return getUserInfo(principalId);
	}

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
		if(groups.contains(TeamConstants.ADMINISTRATORS_TEAM_ID)){
			isAdmin = true;
		}
		UserInfo ui = new UserInfo(isAdmin);
		ui.setId(principalId);
		ui.setCreationDate(principal.getCreationDate());
		// Put all the pieces together
		ui.setGroups(groups);
		ui.setAcceptsTermsOfUse(authDAO.hasUserAcceptedToU(principalId));
		return ui;
	}

	@WriteTransaction
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
	public PrincipalAlias lookupUserByUsernameOrEmail(String alias) {
		// Lookup the user
		PrincipalAlias pa = this.principalAliasDAO.findPrincipalWithAlias(alias, AliasType.USER_EMAIL, AliasType.USER_NAME);
		if(pa == null) {
			throw new NotFoundException("Did not find a user with alias: "+alias);
		}
		return pa;
	}

	@Override
	public void unbindAlias(String aliasName, AliasType type, Long principalId) {
		List<PrincipalAlias> aliases = principalAliasDAO.listPrincipalAliases(principalId, type, aliasName);
		if (aliases.isEmpty()) throw new NotFoundException(
				"The alias "+aliasName+" is not associated with the given user");
		if (aliases.size()>1) throw new IllegalStateException(
				"Expected one alias with name "+aliasName+" but found "+aliases.size());
		principalAliasDAO.removeAliasFromPrincipal(principalId, aliases.get(0).getAliasId());
	}
	
	@Override
	public Set<String> getDistinctUserIdsForAliases(Collection<String> aliases, Long limit, Long offset) {
		// Resolve all user names and team names to principal IDs.
		List<Long> principalIds = principalAliasDAO.findPrincipalsWithAliases(aliases, Lists.newArrayList(AliasType.USER_NAME, AliasType.TEAM_NAME));
		// create a set of string principal IDs.
		Set<String> principalIdsSet = new HashSet<>(principalIds.size());
		for(Long id: principalIds){
			principalIdsSet.add(id.toString());
		}
		// Expand teams to include members and include all users.
		return groupMembersDAO.getIndividuals(principalIdsSet, limit, offset);
	}

}
