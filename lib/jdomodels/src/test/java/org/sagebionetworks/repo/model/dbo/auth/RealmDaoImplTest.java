package org.sagebionetworks.repo.model.dbo.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.RealmDao;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.auth.IdentityProvider;
import org.sagebionetworks.repo.model.auth.OAuthIdentityProvider;
import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;
import org.sagebionetworks.repo.model.auth.SynapseIdentityProvider;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:jdomodels-test-context.xml"})
class RealmDaoImplTest {
	
	private static final List<IdentityProvider> IDP_LIST = List.of(new OAuthIdentityProvider().
			setProvider(OAuthProvider.ARCUS_BIOSCIENCES));
	
	@Autowired
	private RealmDao realmDao;
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	private List<String> idsToDelete;
	private List<String> ugsToDelete;
	

	@BeforeEach
	void setUp() throws Exception {
		idsToDelete = new ArrayList<String>();
		ugsToDelete = new ArrayList<String>();
	}

	@AfterEach
	void tearDown() throws Exception {
		for (String id : idsToDelete) {
			try {
				realmDao.deleteRealmPrincipals(id);
			} catch (NotFoundException e) {
				// continue
			}
		}
		for (String id: ugsToDelete) {
			try {
				userGroupDAO.delete(id);
			} catch (NotFoundException e) {
				// continue
			}
		}
		for (String id : idsToDelete) {
			try {
				realmDao.deleteRealm(id);
			} catch (NotFoundException e) {
				// continue
			}
		}
		ugsToDelete.clear();
		idsToDelete.clear();
	}
	
	private static String NAME = "realm-name";
	
	@Test
	void testBootstrap() {
		// verify that the bootstrapped realm was set up correctly by Spring
		String id = AuthorizationConstants.DEFAULT_REALM_ID;
		List<String> idList =realmDao.listRealmIds().getRealms();
		assertTrue(idList.contains(id));
		Realm defaultRealm = realmDao.getRealm(id);
		List<IdentityProvider> idps = defaultRealm.getIdentityProvider();
		assertEquals(3, idps.size());
		assert(idps.contains(new SynapseIdentityProvider()));
		assert(idps.contains(new OAuthIdentityProvider().setProvider(OAuthProvider.GOOGLE_OAUTH_2_0)));
		assert(idps.contains(new OAuthIdentityProvider().setProvider(OAuthProvider.ORCID)));
		
		RealmPrincipal defaultRealmPrincipals = realmDao.getRealmPrincipals(id);
		assertEquals(id, defaultRealmPrincipals.getRealmId());
		assertEquals(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString(), defaultRealmPrincipals.getAnonymousUser());;
		assertEquals(BOOTSTRAP_PRINCIPAL.AUTHENTICATED_USERS_GROUP.getPrincipalId().toString(), defaultRealmPrincipals.getAuthenticatedUsers());
		assertEquals(BOOTSTRAP_PRINCIPAL.PUBLIC_GROUP.getPrincipalId().toString(), defaultRealmPrincipals.getPublicGroup());
		assertEquals(BOOTSTRAP_PRINCIPAL.ADMINISTRATORS_GROUP.getPrincipalId().toString(), defaultRealmPrincipals.getAdministrativeGroup());

		// method under test
		Optional<String> realmForAnonymous = realmDao.getRealmForAnonymousPrincipal(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
		
		assertEquals(id, realmForAnonymous.get());
	
	}
	
	private String createUserGroup(boolean isIndividual, String reamlId) {
		String id = userGroupDAO.create(new UserGroup().
				setIsIndividual(isIndividual).
					setRealmId(reamlId)).
						toString();
		ugsToDelete.add(id);
		return id;
	}

	@Test
	void testRoundtrip() {
		// method under test
		Optional<String> realmIdForProvider = realmDao.getRealmIdForIdentityProvider(IDP_LIST.get(0));
		assertTrue(realmIdForProvider.isEmpty());
		
		// create a realm
		Realm realm = new Realm();
		realm.setName(NAME); 
		realm.setIdentityProvider(IDP_LIST);
		// method under test
		Realm created = realmDao.createRealm(realm);
		String id = created.getId();
		assertNotNull(id);
		idsToDelete.add(id);
		
		assertNotNull(created.getCreatedOn());
		assertEquals(NAME, created.getName());
		assertEquals(IDP_LIST, created.getIdentityProvider());
		
		// method under test
		List<String> ids = realmDao.listRealmIds().getRealms();
		assertTrue(ids.contains(id));
		
		// method under test
		Realm retrievedRealm = realmDao.getRealm(id);
		assertEquals(created, retrievedRealm);
		
		// method under test
		realmIdForProvider= realmDao.getRealmIdForIdentityProvider(IDP_LIST.get(0));
		assertEquals(id, realmIdForProvider.get());
	
		RealmPrincipal realmPrincipal = new RealmPrincipal();
		realmPrincipal.setRealmId(id);
		realmPrincipal.setAnonymousUser(createUserGroup(true, id));
		realmPrincipal.setAuthenticatedUsers(createUserGroup(false, id));
		realmPrincipal.setPublicGroup(createUserGroup(false, id));
		realmPrincipal.setAdministrativeGroup(createUserGroup(false, id));
		
		// method under test
		realmDao.createRealmPrincipals(realmPrincipal);
		// to verify we have to retrieve the content from the RDS:
		
		// method under test
		RealmPrincipal retrievedRealmPrincial = realmDao.getRealmPrincipals(id);
		assertEquals(realmPrincipal, retrievedRealmPrincial);
		
		// method under test
		Optional<String> realmForAnonymous = realmDao.getRealmForAnonymousPrincipal(retrievedRealmPrincial.getAnonymousUser());
		
		assertEquals(id, realmForAnonymous.get());
		
		// method under test
		realmDao.deleteRealmPrincipals(id);
		
		// now there should be no principals
		retrievedRealmPrincial = realmDao.getRealmPrincipals(id);
		assertNull(retrievedRealmPrincial.getAnonymousUser());
		assertNull(retrievedRealmPrincial.getAuthenticatedUsers());
		assertNull(retrievedRealmPrincial.getPublicGroup());
		assertNull(retrievedRealmPrincial.getAdministrativeGroup());
		
		// need to delete the principals before we can delete the realm
		for (String ugId: ugsToDelete) {
			userGroupDAO.delete(ugId);
		}
		
		// method under test
		realmDao.deleteRealm(id);
		
		// verify that we can't retrieve it
		assertThrows(NotFoundException.class, ()->{
			realmDao.getRealm(id);
		});
	}
	
	@Test
	void testRepeatedIdp() {
		// test that tries to use the same IdP in two realms

		Realm realm = new Realm();
		realm.setName(NAME);
		realm.setIdentityProvider(IDP_LIST);
		// method under test
		Realm created = realmDao.createRealm(realm);
		String id = created.getId();
		assertNotNull(id);
		idsToDelete.add(id);
		
		Realm realm2 = new Realm();
		realm2.setName(NAME+"_2");
		// this is illegal: we can't use the same IDP in two realms
		realm2.setIdentityProvider(IDP_LIST);
		// method under test
		assertThrows(Exception.class, ()->{realmDao.createRealm(realm2);});
	}
	
	@Test
	void testRepeatedPrincipal() {
		// test same principal in two realms
		// create a realm
		Realm realm = new Realm();
		realm.setName(NAME); 
		realm.setIdentityProvider(IDP_LIST);
		// method under test
		Realm created = realmDao.createRealm(realm);
		String id = created.getId();
		idsToDelete.add(id);
	
		RealmPrincipal realmPrincipal = new RealmPrincipal();
		realmPrincipal.setRealmId(id);
		// this is illegal: We can't use a default principal from another realm
		realmPrincipal.setAnonymousUser(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId().toString());
		
		// method under test
		assertThrows(Exception.class, ()->{
			realmDao.createRealmPrincipals(realmPrincipal);});
	}

	// try all IdPs in the oauthprovider enum
	// this ensures all enum values are defined in the DDL file
	@Test
	void testValidateAllOAuthProviders() {
		List<IdentityProvider> idps = new ArrayList<IdentityProvider>();
		for (OAuthProvider provider: OAuthProvider.values()) {
			switch (provider) {
			case GOOGLE_OAUTH_2_0:
			case ORCID:
				// can't try the providers which are taken by the default realm
				break;
			default:
				idps.add(new OAuthIdentityProvider().setProvider(provider));
			}
		}
		Realm realm = new Realm();
		realm.setName(NAME);
		realm.setIdentityProvider(idps);
		
		// method under test
		Realm created = realmDao.createRealm(realm);
		
		idsToDelete.add(created.getId());

		// 'createRealm' may not raise an exception when it fails
		// but the following will
		realmDao.getRealm(created.getId());
	}
	
	@Test
	public void testNonExistentAnonymous() {
		// method under test
		Optional<String> realmForAnonymous = realmDao.getRealmForAnonymousPrincipal("9090909");
		
		assertTrue(realmForAnonymous.isEmpty());
	}

}
