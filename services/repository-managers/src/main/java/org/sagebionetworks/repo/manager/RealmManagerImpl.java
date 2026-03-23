package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessControlListDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RealmDao;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmIdList;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.BootstrapTeam;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

public class RealmManagerImpl implements RealmManager {

	@Autowired
	private RealmDao realmDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private PrincipalAliasDAO principalAliasDAO;
	
	@Autowired
	private TeamManager teamManager;

	@Autowired
	private IdGenerator idGenerator;
	
	@Autowired
	private AccessControlListDAO aclDAO;
	
	@Autowired
	private StackConfiguration stackConfiguration;
	
	static final String ANONYMOUS_SUFFIX = "-Anonymous";
	static final String PUBLIC_SUFFIX = "-Public";
	static final String AUTH_USERS_SUFFIX = " Authenticated_Users";
	static final String ADMINISTRATORS_SUFFIX = "-Administrators";
	
	String createRealmPrincipal(String realmId, String alias, boolean isIndvidual) {
		if (!principalAliasDAO.isAliasAvailable(alias)) {
			throw new IllegalArgumentException("Alias "+alias+" is not available.");
		}
		UserGroup userGroup = new UserGroup();
		userGroup.setIsIndividual(isIndvidual);
		userGroup.setRealmId(realmId);
		userGroup.setCreationDate(new Date());
		Long principalId = userGroupDAO.create(userGroup);
		PrincipalAlias principalAlias = new PrincipalAlias();
		principalAlias.setPrincipalId(principalId);
		principalAlias.setType(isIndvidual ? AliasType.USER_NAME : AliasType.TEAM_NAME);
		principalAlias.setAlias(alias);
		principalAliasDAO.bindAliasToPrincipal(principalAlias);
		return String.valueOf(principalId);
	}

	String createRealmAdminTeam(String realmId, String realmName) {
		BootstrapTeam bootstrapTeamForRealm = new BootstrapTeam();
		bootstrapTeamForRealm.setId(idGenerator.generateNewId(IdType.PRINCIPAL_ID).toString());
		bootstrapTeamForRealm.setCanPublicJoin(false);
		bootstrapTeamForRealm.setDescription("Administration team for " + realmName);
		bootstrapTeamForRealm.setName(realmName + ADMINISTRATORS_SUFFIX);
		return teamManager.bootstrapTeam(bootstrapTeamForRealm, realmId);
	}
	
	// Since the realm name will become the prefix for the aliases of the 
	// realm's principals, we apply the same constraint that we do for those 
	// names.
	private static final String REALM_NAME_REGEX = "^[A-Za-z0-9._-]{3,}";
	
	private AccessControlList getRootACL() {
		String rootNodeId = stackConfiguration.getRootFolderEntityId();
		Optional<AccessControlList> optionalAcl = aclDAO.getAcl(rootNodeId, ObjectType.ENTITY);
		if (optionalAcl.isEmpty()) {
			throw new IllegalStateException("Cannot find ACL for entity "+rootNodeId);
		}
		return optionalAcl.get();
	}

	@Override
	@WriteTransaction
	public Realm createRealm(UserInfo userInfo, Realm realm) {
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only an administrator can perform this action.");
		}
		String name = realm.getName();
		ValidateArgument.required(name, "name");
		Matcher matcher = Pattern.compile(REALM_NAME_REGEX).matcher(name);
		ValidateArgument.requirement(matcher.matches(), "Realm name can only contain letters, numbers, dots, dashes and underscores, and must be at least three characters.");
		ValidateArgument.requiredNotEmpty(realm.getIdentityProvider(), "identity providers");
		realm = realmDao.createRealm(realm);
		RealmPrincipal realmPrincipal = new RealmPrincipal();
		realmPrincipal.setRealmId(realm.getId());
		realmPrincipal.setAnonymousUser(createRealmPrincipal(realm.getId(), name+ANONYMOUS_SUFFIX, true));
		realmPrincipal.setAuthenticatedUsers(createRealmPrincipal(realm.getId(), name+AUTH_USERS_SUFFIX, false));
		realmPrincipal.setPublicGroup(createRealmPrincipal(realm.getId(), name+PUBLIC_SUFFIX, false));
		realmPrincipal.setAdministrativeGroup(createRealmAdminTeam(realm.getId(), name));
		realmDao.createRealmPrincipals(realmPrincipal);
		// update the root ACL to give authenticated users CREATE permission
		AccessControlList acl = getRootACL();
		ResourceAccess ra = new ResourceAccess().
				setPrincipalId(Long.parseLong(realmPrincipal.getAuthenticatedUsers())).
				setAccessType(Set.of(ACCESS_TYPE.CREATE));
		acl.getResourceAccess().add(ra);
		aclDAO.update(acl, ObjectType.ENTITY);
		return realm;
	}

	@Override
	public RealmIdList listRealmIds() {
		return realmDao.listRealmIds();
	}

	@Override
	public Realm getRealm(String id) {
		return realmDao.getRealm(id);
	}

	@Override
	public RealmPrincipal getRealmPrincipals(String id) {
		return realmDao.getRealmPrincipals(id);
	}

	void removeUserGroup(String id) {
		if (id==null) return;
		principalAliasDAO.removeAllAliasFromPrincipal(Long.parseLong(id));
		userGroupDAO.delete(id);
	}
	
	static boolean removePrincipalFromACL(AccessControlList acl, Long principalId) {
		Set<ResourceAccess> modifiedResourceAccess = new HashSet<ResourceAccess>();
		boolean removed=false;
		for (ResourceAccess ra : acl.getResourceAccess()) {
			if (!ra.getPrincipalId().equals(principalId)) {
				modifiedResourceAccess.add(ra);
			} else {
				removed=true;
			}
		}
		acl.setResourceAccess(modifiedResourceAccess);
		return removed;
	}
	@Override
	@WriteTransaction
	public void deleteRealm(UserInfo userInfo, String realmId) {
		if (AuthorizationConstants.DEFAULT_REALM_ID.equals(realmId)) {
			throw new IllegalArgumentException("Cannot delete default realm.");
		}
		if (!userInfo.isAdmin()) {
			throw new UnauthorizedException("Only an administrator can perform this action.");
		}
		RealmPrincipal realmPrincipal = realmDao.getRealmPrincipals(realmId);
		// first, remove the realm's authenticated users from the root node
		AccessControlList acl = getRootACL();
		boolean removed = removePrincipalFromACL(acl, Long.parseLong(realmPrincipal.getAuthenticatedUsers()));
		if (!removed) {
			throw new IllegalStateException("Unable to find authenticated users group for realm "+realmId+" in root ACL.");
		}
		aclDAO.update(acl, ObjectType.ENTITY);
		// next, delete the realm-principal association
		realmDao.deleteRealmPrincipals(realmId);
		// next, delete the principals
		if (realmPrincipal.getAdministrativeGroup() != null) {
			teamManager.delete(userInfo, realmPrincipal.getAdministrativeGroup());
		}
		removeUserGroup(realmPrincipal.getAnonymousUser());
		removeUserGroup(realmPrincipal.getAuthenticatedUsers());
		removeUserGroup(realmPrincipal.getPublicGroup());
		// finally, delete the realm
		realmDao.deleteRealm(realmId);
	}

}
