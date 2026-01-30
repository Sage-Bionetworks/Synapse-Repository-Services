package org.sagebionetworks.repo.manager;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.sagebionetworks.repo.manager.team.TeamManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.RealmDao;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmIdList;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;
import org.sagebionetworks.repo.model.principal.AliasType;
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
	
	String createRealmAdminTeam(UserInfo userInfo, String realmId, String realmName) {
		Team adminTeam = new Team();
		adminTeam.setCanPublicJoin(false);
		adminTeam.setDescription("Administration team for "+realmName);
		adminTeam.setName(realmName+ADMINISTRATORS_SUFFIX);
		adminTeam = teamManager.create(userInfo, adminTeam, realmId);
		return adminTeam.getId();
	}
	
	// Since the realm name will become the prefix for the aliases of the 
	// realm's principals, we apply the same constraint that we do for those 
	// names.
	private static final String REALM_NAME_REGEX = "^[A-Za-z0-9._-]{3,}";

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
		realmPrincipal.setAdministrativeGroup(createRealmAdminTeam(userInfo, realm.getId(), name));
		realmPrincipal = realmDao.createRealmPrincipals(realmPrincipal);
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
		// first, delete the realm-principal association
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
