package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.sagebionetworks.repo.model.auth.IdentityProvider;
import org.sagebionetworks.repo.model.auth.OAuthIdentityProvider;
import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmIdList;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.BootstrapTeam;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;

@ExtendWith(MockitoExtension.class)
class RealmManagerImplTest {
	
	@Mock
	private RealmDao realmDao;
	
	@Mock
	private UserGroupDAO userGroupDAO;
	
	@Mock
	private PrincipalAliasDAO principalAliasDAO;
	
	@Mock
	private TeamManager teamManager;
	
	@Mock
	private IdGenerator idGenerator;
	
	@Mock
	private StackConfiguration stackConfiguration;
	
	@Mock
	private AccessControlListDAO aclDAO;

	@InjectMocks
	RealmManagerImpl realmManager;

	private UserInfo userInfo;
	private UserInfo adminUserInfo;
	private AccessControlList rootEntityAcl;
	
	@BeforeEach
	void setUp() throws Exception {
		userInfo = new UserInfo(false, 2L, AuthorizationConstants.DEFAULT_REALM_ID);
		adminUserInfo = new UserInfo(true, 1L, AuthorizationConstants.DEFAULT_REALM_ID);
		rootEntityAcl = new AccessControlList();
		Set<ResourceAccess> raSet = new HashSet<ResourceAccess>();
		rootEntityAcl.setResourceAccess(raSet);
	}
	
	private static final String ID ="101";
	private static final String REALM_NAME ="Realm-Name";
	private static final List<IdentityProvider> IDP_LIST = List.of(new OAuthIdentityProvider().
			setProvider(OAuthProvider.GOOGLE_OAUTH_2_0));
	private static final String REALM_ANONYMOUS_PRINCIPAL_ALIAS = REALM_NAME+"-Anonymous";
	private static final String REALM_AUTHENTICATED_PRINCIPAL_ALIAS = REALM_NAME+" Authenticated_Users";
	private static final String REALM_PUBLIC_PRINCIPAL_ALIAS = REALM_NAME+"-Public";
	private static final String REALM_ADMIN_PRINCIPAL_ALIAS = REALM_NAME+"-Administrators";
	private static final Long ANONYMOUS_ID = 101L;
	private static final Long AUTHENTICATED_ID = 102L;
	private static final Long PUBLIC_ID = 103L;
	private static final String ADMIN_TEAM_ID = "999";
	private static final String ROOT_ENTITY_ID = "123";


	@Test
	void testCreate() {
		Realm mockCreated = new Realm();
		mockCreated.setCreatedOn(new Date());
		mockCreated.setId(ID);
		mockCreated.setIdentityProvider(IDP_LIST);
		mockCreated.setName(REALM_NAME);
		when(realmDao.createRealm(any())).thenReturn(mockCreated);
		
		when(principalAliasDAO.isAliasAvailable(any())).thenReturn(true);
		

		when(userGroupDAO.create(any())).thenReturn(ANONYMOUS_ID, AUTHENTICATED_ID, PUBLIC_ID);

		when(idGenerator.generateNewId(IdType.PRINCIPAL_ID)).thenReturn(888l);
		when(teamManager.bootstrapTeam(any(BootstrapTeam.class), anyString())).thenReturn("888");
		
		when(stackConfiguration.getRootFolderEntityId()).thenReturn(ROOT_ENTITY_ID);
		when(aclDAO.getAcl(ROOT_ENTITY_ID, ObjectType.ENTITY)).thenReturn(Optional.of(rootEntityAcl));
		
		Realm realm = new Realm();
		realm.setName(REALM_NAME);
		realm.setIdentityProvider(IDP_LIST);
		
		// method under test
		Realm created = realmManager.createRealm(adminUserInfo, realm);
		assertEquals(mockCreated, created);
		verify(realmDao).createRealm(realm);
		
		ArgumentCaptor<UserGroup> userGroupCaptor = ArgumentCaptor.forClass(UserGroup.class);
		verify(userGroupDAO, times(3)).create(userGroupCaptor.capture());
		List<UserGroup> ugs = userGroupCaptor.getAllValues();
		assertEquals(3, ugs.size());
		UserGroup anonymousUg = ugs.get(0);
		assertTrue(anonymousUg.getIsIndividual());
		assertEquals(ID, anonymousUg.getRealmId());
		assertNotNull(anonymousUg.getCreationDate());
		UserGroup authenticatedUg = ugs.get(1);
		assertFalse(authenticatedUg.getIsIndividual());
		assertEquals(ID, authenticatedUg.getRealmId());
		assertNotNull(authenticatedUg.getCreationDate());
		UserGroup publicUg = ugs.get(2);
		assertFalse(publicUg.getIsIndividual());
		assertEquals(ID, publicUg.getRealmId());
		assertNotNull(publicUg.getCreationDate());
		
		ArgumentCaptor<PrincipalAlias> principalAliasCaptor = ArgumentCaptor.forClass(PrincipalAlias.class);
		verify(principalAliasDAO, times(3)).bindAliasToPrincipal(principalAliasCaptor.capture());
		List<PrincipalAlias> boundValues = principalAliasCaptor.getAllValues();
		assertEquals(3, boundValues.size());
		PrincipalAlias anonymousAlias = boundValues.get(0);
		assertEquals(REALM_ANONYMOUS_PRINCIPAL_ALIAS, anonymousAlias.getAlias());
		assertEquals(AliasType.USER_NAME, anonymousAlias.getType());
		PrincipalAlias authenticatedAlias = boundValues.get(1);
		assertEquals(REALM_AUTHENTICATED_PRINCIPAL_ALIAS, authenticatedAlias.getAlias());
		assertEquals(AliasType.TEAM_NAME, authenticatedAlias.getType());
		PrincipalAlias publicAlias = boundValues.get(2);
		assertEquals(REALM_PUBLIC_PRINCIPAL_ALIAS, publicAlias.getAlias());
		assertEquals(AliasType.TEAM_NAME, publicAlias.getType());
		
		ArgumentCaptor<BootstrapTeam> teamCaptor = ArgumentCaptor.forClass(BootstrapTeam.class);
		verify(teamManager).bootstrapTeam(teamCaptor.capture(), eq(ID));
		BootstrapTeam adminTeam = teamCaptor.getValue();
		assertEquals(REALM_ADMIN_PRINCIPAL_ALIAS, adminTeam.getName());
		assertFalse(adminTeam.getCanPublicJoin());

		
		ArgumentCaptor<RealmPrincipal> realmPrincipalCaptor = ArgumentCaptor.forClass(RealmPrincipal.class);
		verify(realmDao).createRealmPrincipals(realmPrincipalCaptor.capture());
		RealmPrincipal actualRealmPrincipal = realmPrincipalCaptor.getValue();
		assertEquals(ID, actualRealmPrincipal.getRealmId());
		assertEquals(String.valueOf(ANONYMOUS_ID), actualRealmPrincipal.getAnonymousUser());
		assertEquals(String.valueOf(AUTHENTICATED_ID), actualRealmPrincipal.getAuthenticatedUsers());
		assertEquals(String.valueOf(PUBLIC_ID), actualRealmPrincipal.getPublicGroup());
		assertEquals(adminTeam.getId(), actualRealmPrincipal.getAdministrativeGroup());
		
		ArgumentCaptor<AccessControlList> aclCaptor = ArgumentCaptor.forClass(AccessControlList.class);
		verify(aclDAO).update(aclCaptor.capture(), eq(ObjectType.ENTITY));
		AccessControlList updatedACL = aclCaptor.getValue();
		Set<ResourceAccess> ras = updatedACL.getResourceAccess();
		// the ACL starts empty, so we just verify the the expected row is now there;
		// we don't have to worry about looking through any other rows
		ResourceAccess ra = ras.iterator().next();
		assertEquals(AUTHENTICATED_ID, ra.getPrincipalId());
		assertEquals(Set.of(ACCESS_TYPE.CREATE), ra.getAccessType());
	}
	
	@Test
	void testCreateRealmNotAdmin() {
		Realm realm = new Realm();
		// method under test
		assertThrows(UnauthorizedException.class, ()->{realmManager.createRealm(userInfo, realm);});
	}
	
	@Test
	void testCreateRealmMissingRequiredFields() {
		Realm realm = new Realm();
		realm.setName(REALM_NAME);
		realm.setIdentityProvider(null); // not allowed to be missing
		assertThrows(IllegalArgumentException.class, ()->{
			// method under test
			realmManager.createRealm(adminUserInfo, realm);
		});
		
		realm.setIdentityProvider(Collections.EMPTY_LIST); // not allowed to be missing
		assertThrows(IllegalArgumentException.class, ()->{
			// method under test
			realmManager.createRealm(adminUserInfo, realm);
		});
		
		
		realm.setName(null); // not allowed to be missing
		realm.setIdentityProvider(IDP_LIST);
		assertThrows(IllegalArgumentException.class, ()->{
			// method under test
			realmManager.createRealm(adminUserInfo, realm);
		});
	}
	
	@Test
	void testCreateRealmIllegalName() {
		Realm realm = new Realm();
		realm.setName(REALM_NAME+"$"); // illegal name
		realm.setIdentityProvider(IDP_LIST);
		assertThrows(IllegalArgumentException.class, ()->{
			// method under test
			realmManager.createRealm(adminUserInfo, realm);
		});

	}
	
	@Test
	void testCreateRealmAliasUnavailable() {
		Realm mockCreated = new Realm();
		mockCreated.setCreatedOn(new Date());
		mockCreated.setId(ID);
		mockCreated.setIdentityProvider(IDP_LIST);
		mockCreated.setName(REALM_NAME);
		when(realmDao.createRealm(any())).thenReturn(mockCreated);
		
		// this will cause the exception
		when(principalAliasDAO.isAliasAvailable(any())).thenReturn(false);
		
		Realm realm = new Realm();
		realm.setName(REALM_NAME);
		realm.setIdentityProvider(IDP_LIST);
		// method under test
		assertThrows(IllegalArgumentException.class, ()-> {
			realmManager.createRealm(adminUserInfo, realm);
		});
	}
	
	@Test
	void testListRealmIds() {
		when(realmDao.listRealmIds()).thenReturn(new RealmIdList().setRealms(List.of(ID)));
		// method under test
		RealmIdList idList = realmManager.listRealmIds();
		assertEquals(1, idList.getRealms().size());
		assertEquals(ID, idList.getRealms().get(0));
	}
	
	@Test
	void testGetRealm() {
		Realm mockCreated = new Realm();
		mockCreated.setCreatedOn(new Date());
		mockCreated.setId(ID);
		mockCreated.setIdentityProvider(IDP_LIST);
		mockCreated.setName(REALM_NAME);
		when(realmDao.getRealm(any())).thenReturn(mockCreated);
		// method under test
		Realm retrieved = realmManager.getRealm(ID);
		
		assertEquals(mockCreated, retrieved);
	}

	@Test
	void testGetRealmPrincipals() {
		when(realmDao.getRealmPrincipals(ID)).thenReturn(new RealmPrincipal().setRealmId(ID));
		// method under test
		RealmPrincipal realmPrincipal = realmManager.getRealmPrincipals(ID);
		
		assertEquals(ID, realmPrincipal.getRealmId());
	}
	
	@Test
	void testDeleteRealm() {
		when(realmDao.getRealmPrincipals(ID)).thenReturn(
				new RealmPrincipal().
				setRealmId(ID).
				setAnonymousUser(ANONYMOUS_ID.toString()).
				setAuthenticatedUsers(AUTHENTICATED_ID.toString()).
				setPublicGroup(PUBLIC_ID.toString()).
				setAdministrativeGroup(ADMIN_TEAM_ID));
		when(stackConfiguration.getRootFolderEntityId()).thenReturn(ROOT_ENTITY_ID);
		// add row to acl
		ResourceAccess ra = new ResourceAccess();
		ra.setPrincipalId(AUTHENTICATED_ID);
		ra.setAccessType(Set.of(ACCESS_TYPE.CREATE));
		rootEntityAcl.getResourceAccess().add(ra);
		when(aclDAO.getAcl(ROOT_ENTITY_ID, ObjectType.ENTITY)).thenReturn(Optional.of(rootEntityAcl));
		
		// method under test
		realmManager.deleteRealm(adminUserInfo, ID);
		
		verify(realmDao).deleteRealmPrincipals(ID);
		
		verify(teamManager).delete(adminUserInfo, ADMIN_TEAM_ID);
		
		verify(principalAliasDAO).removeAllAliasFromPrincipal(ANONYMOUS_ID);
		verify(principalAliasDAO).removeAllAliasFromPrincipal(AUTHENTICATED_ID);
		verify(principalAliasDAO).removeAllAliasFromPrincipal(PUBLIC_ID);
		
		verify(userGroupDAO).delete(ANONYMOUS_ID.toString());
		verify(userGroupDAO).delete(AUTHENTICATED_ID.toString());
		verify(userGroupDAO).delete(PUBLIC_ID.toString());
		
		verify(realmDao).deleteRealm(ID);
		
		ArgumentCaptor<AccessControlList> aclCaptor = ArgumentCaptor.forClass(AccessControlList.class);
		verify(aclDAO).update(aclCaptor.capture(), eq(ObjectType.ENTITY));
		AccessControlList updatedACL = aclCaptor.getValue();
		assertTrue(updatedACL.getResourceAccess().isEmpty());
	}
	
	@Test
	void testDeleteRealmNotAdmin() {
		// method under test
		assertThrows(UnauthorizedException.class, ()-> {
			realmManager.deleteRealm(userInfo, ID);
		});
	}
	
	@Test
	void testDeleteDefaultRealm() {
		// method under test
		assertThrows(IllegalArgumentException.class, ()-> {
			realmManager.deleteRealm(userInfo, AuthorizationConstants.DEFAULT_REALM_ID);
		});
	}
}
