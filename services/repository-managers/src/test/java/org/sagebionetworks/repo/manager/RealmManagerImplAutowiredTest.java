package org.sagebionetworks.repo.manager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.IdentityProvider;
import org.sagebionetworks.repo.model.auth.OAuthIdentityProvider;
import org.sagebionetworks.repo.model.auth.Realm;
import org.sagebionetworks.repo.model.auth.RealmIdList;
import org.sagebionetworks.repo.model.auth.RealmPrincipal;
import org.sagebionetworks.repo.model.oauth.OAuthProvider;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class RealmManagerImplAutowiredTest {
	
	@Autowired
	private RealmManager realmManager;

    private static final String NAME = "test-realm";
    private static final List<IdentityProvider> IDPS = 
    		List.of(new OAuthIdentityProvider().setProvider(OAuthProvider.SAGE_BIONETWORKS));

    UserInfo adminUserInfo;
    private Realm realmToDelete=null;
    
	@BeforeEach
	public void setUp() throws Exception {
		adminUserInfo = new UserInfo(true, 0L, AuthorizationConstants.DEFAULT_REALM_ID);
	}

	@AfterEach
	public void tearDown() throws Exception {
		if (realmToDelete!=null) {
			realmManager.deleteRealm(adminUserInfo, realmToDelete.getId());
		}
	}

	@Test
	public void testRoundtrip() throws DatastoreException, UnauthorizedException, NotFoundException {
		// create realm
		Realm realm = new Realm();
		realm.setName(NAME);
		realm.setIdentityProvider(IDPS);
		Realm created = realmManager.createRealm(adminUserInfo, realm);
		String id = created.getId();
		assertNotNull(id);
		realmToDelete=realm;
		
		// list realms
		RealmIdList realmIdList = realmManager.listRealmIds();
		assertTrue(realmIdList.getRealms().contains(id));
		
		// get realm by id
		Realm retrieved = realmManager.getRealm(id);
		assertEquals(created, retrieved);
		
		// get realm principals
		RealmPrincipal principals = realmManager.getRealmPrincipals(id);
		assertEquals(id, principals.getRealmId());
		assertNotNull(principals.getAnonymousUser());
		assertNotNull(principals.getAuthenticatedUsers());
		assertNotNull(principals.getPublicGroup());
		assertNotNull(principals.getAdministrativeGroup());
		
		// delete realm
		realmManager.deleteRealm(adminUserInfo, id);
		realmToDelete=null;
	}
}
