package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.sagebionetworks.repo.manager.principal.NewUserUtils;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.CertifiedUsersDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.GroupMembersDAO;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.RealmDao;
import org.sagebionetworks.repo.model.SessionIdThreadLocal;
import org.sagebionetworks.repo.model.TeamConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.auth.AuthenticationDAO;
import org.sagebionetworks.repo.model.auth.CallersContext;
import org.sagebionetworks.repo.model.auth.IdentityProvider;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.auth.OAuthIdentityProvider;
import org.sagebionetworks.repo.model.auth.SynapseIdentityProvider;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;
import org.sagebionetworks.repo.model.dao.NotificationEmailDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.auth.UserStatusDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalOIDCBindingDao;
import org.sagebionetworks.repo.model.dbo.principal.PrincipalOidcBinding;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.securitytools.PBKDF2Utils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

public class UserManagerImpl implements UserManager {

	private final UserGroupDAO userGroupDAO;
	private final UserProfileDAO userProfileDAO;
	private final GroupMembersDAO groupMembersDAO;
	private final CertifiedUsersDAO certifiedUsersDAO;
	private final AuthenticationDAO authDAO;
	private final PrincipalAliasDAO principalAliasDAO;
	private final NotificationEmailDAO notificationEmailDao;
	private final PrincipalOIDCBindingDao principalOidcBindingDao;
	private final UserStatusDao userStatusDao;
	private final RealmDao realmDao;
	
	/**
	 * Testing purposes only
	 * Do NOT use in non-test code
	 * i.e. {@link #createOrGetTestUser(UserInfo, String, UserProfile, DBOCredential)}
	 */
	private final DBOBasicDao basicDAO;
	
	@Autowired
	public UserManagerImpl(UserGroupDAO userGroupDAO, UserProfileDAO userProfileDAO, GroupMembersDAO groupMembersDAO,
			AuthenticationDAO authDAO, PrincipalAliasDAO principalAliasDAO, NotificationEmailDAO notificationEmailDao,
			PrincipalOIDCBindingDao principalOIDCBindingDao, CertifiedUsersDAO certifiedUsersDAO,
			DBOBasicDao basicDAO, UserStatusDao userStatusDao, RealmDao realmDao) {
		super();
		this.userGroupDAO = userGroupDAO;
		this.userProfileDAO = userProfileDAO;
		this.groupMembersDAO = groupMembersDAO;
		this.authDAO = authDAO;
		this.principalAliasDAO = principalAliasDAO;
		this.notificationEmailDao = notificationEmailDao;
		this.principalOidcBindingDao = principalOIDCBindingDao;
		this.certifiedUsersDAO = certifiedUsersDAO;
		this.userStatusDao = userStatusDao;
		this.basicDAO = basicDAO;
		this.realmDao=realmDao;
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
		String realmId=AuthorizationConstants.DEFAULT_REALM_ID;
		// Make sure that the subject for an oauth provider is not bound yet
		if (user.getOauthProvider() != null) {
			lookupOidcBindingBySubject(user.getOauthProvider(), user.getSubject()).ifPresent((principalId)-> {
				throw new NameConflictException("The provided '" + user.getOauthProvider() +"' account is already registered with Synapse");
			});
			IdentityProvider identityProvider = new OAuthIdentityProvider().setProvider(user.getOauthProvider());
			Optional<String> optionalRealmId=realmDao.getRealmIdForIdentityProvider(identityProvider);
			if (optionalRealmId.isEmpty()) {
				throw new IllegalArgumentException("There is no security realm associated with "+user.getOauthProvider().name());
			}
			realmId=optionalRealmId.get();
		}
		
		Date createdOn = new Date();
		
		UserGroup individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(createdOn);
		individualGroup.setRealmId(realmId);
		Long principalId = userGroupDAO.create(individualGroup);
		
		// Make some credentials for this user
		authDAO.createNew(principalId);
		userStatusDao.setLastSeenOn(List.of(principalId), createdOn);
		
		// Create a new user profile.
		UserProfile userProfile = new UserProfile();
		
		userProfile.setOwnerId(principalId.toString());
		userProfile.setFirstName(user.getFirstName());
		userProfile.setLastName(user.getLastName());
		userProfile.setUserName(user.getUserName());
		
		userProfileDAO.create(userProfile);
		
		bindAllAliases(user, principalId);
		
		return principalId;
	}
	
	/**
	 * This method is idempotent.
	 * @param user
	 * @param principalId
	 */
	private void bindAllAliases(NewUser user, Long principalId) {
		// Bind all aliases
		bindAlias(user.getUserName(), AliasType.USER_NAME, principalId);
		PrincipalAlias emailAlias = bindAlias(user.getEmail(), AliasType.USER_EMAIL, principalId);
		if (user.getOauthProvider() != null) {
			bindUserToOidcSubject(emailAlias, user.getOauthProvider(), user.getSubject());
		}
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
	public UserInfo createOrGetTestUser(UserInfo adminUserInfo, NewUser user) throws NotFoundException {
		String password = null;
		boolean signTermsOfService = true;
		return createOrGetTestUser(adminUserInfo, user, password, signTermsOfService);
	}


	@WriteTransaction
	@Override
	public UserInfo createOrGetTestUser(UserInfo adminUserInfo, NewUser user, String password, boolean signTermsOfService) throws NotFoundException {
		if (!adminUserInfo.isAdmin()) {
			throw new UnauthorizedException("Must be an admin to use this service");
		}
		// Create the user
		Long principalId;
		PrincipalAlias alias = principalAliasDAO.findPrincipalWithAlias(user.getUserName());
		if (alias==null) {
			principalId = createUser(user);

			DBOCredential credential = new DBOCredential();
			credential.setEtag(UUID.randomUUID().toString());
			credential.setPrincipalId(principalId);
			
			if (password != null) {
				credential.setPassHash(PBKDF2Utils.hashPassword(password, null));
			}
			
			basicDAO.update(credential);

			if (signTermsOfService) {
				authDAO.addTermsOfServiceAgreement(principalId, authDAO.getCurrentTermsOfServiceRequirements().getMinimumTermsOfServiceVersion(), new Date());
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

		RealmPrincipal realmPrincipals = realmDao.getRealmPrincipals(principal.getRealmId());

		// Check which group(s) of Anonymous, Public, or Authenticated the user belongs to
		Set<Long> groups = new HashSet<Long>();
		boolean isUserAnonymous = (principalId==Long.parseLong(realmPrincipals.getAnonymousUser()));
		// Everyone except the anonymous users belongs to "authenticated users"
		if (!isUserAnonymous) {
			// All authenticated users belong to the authenticated user group
			groups.add(Long.parseLong(realmPrincipals.getAuthenticatedUsers()));
		}
		
		// Everyone belongs to their own group and to Public
		groups.add(principalId);
		groups.add(Long.parseLong(realmPrincipals.getPublicGroup()));
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
		UserInfo ui = new UserInfo(isAdmin, principalId, principal.getRealmId());
		ui.setCreationDate(principal.getCreationDate());
		// Put all the pieces together
		ui.setGroups(groups);
		ui.setRealmAnonymousUserId(Long.valueOf(realmPrincipals.getAnonymousUser()));
		ui.setRealmAuthenticatedUsersId(Long.valueOf(realmPrincipals.getAuthenticatedUsers()));
		ui.setRealmPublicUsersId(Long.valueOf(realmPrincipals.getPublicGroup()));
		ui.setTwoFactorAuthEnabled(authDAO.isTwoFactorAuthEnabled(principalId));
		ui.setCertified(certifiedUsersDAO.isCertifiedUser(principalId.toString()));
		ui.setContext(new CallersContext().setSessionId(SessionIdThreadLocal.getThreadsSessionId().orElse("missing")));
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
	public List<UserGroup> getGroupsInRange(UserInfo userInfo, long startIncl, long endExcl, String sort, boolean ascending) 
			throws DatastoreException, UnauthorizedException {
		return userGroupDAO.getInRange(startIncl, endExcl, false, userInfo.getRealmId());
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
	public PrincipalAlias lookupUserByAliasType(AliasType type, String alias) {
		PrincipalAlias pa = this.principalAliasDAO.findPrincipalWithAlias(alias, type);
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
	

	@Override
	public Optional<PrincipalOidcBinding> lookupOidcBindingBySubject(OAuthProvider provider, String subject) {
		ValidateArgument.required(provider, "The provider");
		ValidateArgument.requiredNotBlank(subject, "The subject");
		
		return principalOidcBindingDao.findBindingForSubject(provider, subject);
	}
	
	@WriteTransaction
	@Override
	public PrincipalOidcBinding bindUserToOidcSubject(PrincipalAlias principalAlias, OAuthProvider provider, String subject) {
		ValidateArgument.required(principalAlias, "The principalAlias");
		ValidateArgument.required(provider, "The provider");
		ValidateArgument.requiredNotBlank(subject, "The subject");
		
		principalOidcBindingDao.bindPrincipalToSubject(principalAlias.getPrincipalId(), principalAlias.getAliasId(), provider, subject);
		
		return principalOidcBindingDao.findBindingForSubject(provider, subject).orElseThrow( () -> new IllegalStateException("Failed to bind principal to provider subject."));
	}
	
	@Override
	@WriteTransaction
	public void setOidcBindingAlias(PrincipalOidcBinding binding, PrincipalAlias principalAlias) {
		ValidateArgument.required(binding, "The binding");
		ValidateArgument.required(principalAlias, "The principalAlias");
		
		principalOidcBindingDao.setBindingAlias(binding.getBindingId(), principalAlias.getAliasId());
	}
	
	@Override
	@WriteTransaction
	public void deleteOidcBinding(Long bindingId) {
		ValidateArgument.required(bindingId, "The binding id");
		
		principalOidcBindingDao.deleteBinding(bindingId);
	}
	
	@Override
	@WriteTransaction
	public void clearOidcBindings(Long userId) {
		ValidateArgument.required(userId, "The user id");

		principalOidcBindingDao.clearBindings(userId);
		
	}

	@Override
	public List<IdentityProvider> getIdentityProviders(UserInfo userInfo) {
		ValidateArgument.required(userInfo, "userInfo");
		List<IdentityProvider> providers = new ArrayList<>();
		if (AuthorizationConstants.DEFAULT_REALM_ID.equals(userInfo.getRealmId())) {
			providers.add(new SynapseIdentityProvider());
		}
		List<OAuthProvider> oauthProviders = principalOidcBindingDao.getLinkedProviders(userInfo.getId());
		for (OAuthProvider oauthProvider : oauthProviders) {
			OAuthIdentityProvider oip = new OAuthIdentityProvider();
			oip.setProvider(oauthProvider);
			providers.add(oip);
		}
		return providers;
	}

	@Override
	public void truncateAll() {
		userGroupDAO.truncateAll();
	}

}
