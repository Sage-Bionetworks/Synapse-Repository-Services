package org.sagebionetworks.repo.manager.oauth;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class OAuthClientManagerImplAutowiredTest {
	private static final String CLIENT_NAME = "some client";
	private static final String CLIENT_URI = "https://client.uri.com/index.html";
	private static final String POLICY_URI = "https://client.uri.com/policy.html";
	private static final String TOS_URI = "https://client.uri.com/termsOfService.html";
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://client.com/redir");
	
	@Autowired
	private UserManager userManager;

	@Autowired
	private OAuthClientManager oauthClientManager;
	
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private List<String> oauthClientsToDelete;
	
	@BeforeEach
	public void setUp() throws Exception {
		oauthClientsToDelete = new LinkedList<String>();
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		userInfo = userManager.createOrGetTestUser(adminUserInfo, nu, cred, tou);
	}
	
	@AfterEach
	public void tearDown() throws Exception {
		for(String id: oauthClientsToDelete) {
			try {
				oauthClientManager.deleteOpenIDConnectClient(adminUserInfo, id);
			} catch (NotFoundException e) {
				// stale ID, no deletion necessary
			}
		}
		try {
			userManager.deletePrincipal(adminUserInfo, userInfo.getId());
		} catch (DataAccessException e) {
		}
	}

	// the business logic is tested in detail in the unit tests.  This just does a basic round-trip.
	@Test
	public void testRoundTrip() throws Exception {
		OAuthClient toCreate = new OAuthClient();
		toCreate.setClient_name(CLIENT_NAME);
		toCreate.setClient_uri(CLIENT_URI);
		toCreate.setCreatedOn(new Date(System.currentTimeMillis()));
		toCreate.setModifiedOn(new Date(System.currentTimeMillis()));
		toCreate.setPolicy_uri(POLICY_URI);
		toCreate.setRedirect_uris(REDIRCT_URIS);
		toCreate.setTos_uri(TOS_URI);
		toCreate.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);
		
		// method under test
		OAuthClient created = oauthClientManager.createOpenIDConnectClient(userInfo, toCreate);
		String id = created.getClient_id();
		String etag = created.getEtag();
		assertNotNull(id);
		oauthClientsToDelete.add(id);
		
		// method under test
		created = oauthClientManager.updateOpenIDConnectClientVerifiedStatus(adminUserInfo, id, etag, true);
		
		assertTrue(created.getVerified());
		
		// method under test
		assertEquals(created, oauthClientManager.getOpenIDConnectClient(userInfo, id));
		
		// method under test
		OAuthClientList list = oauthClientManager.listOpenIDConnectClients(userInfo, null);
		assertEquals(1, list.getResults().size());
		assertEquals(created, list.getResults().get(0));
		
		created.setPolicy_uri("http://someOtherPolicyUri.com");
		created.setClient_name("some other name");
		
		// method under test
		OAuthClient updated = oauthClientManager.updateOpenIDConnectClient(userInfo, created);
		created.setEtag(updated.getEtag());
		created.setModifiedOn(updated.getModifiedOn());
		assertEquals(created, updated);
		
		// method under test
		OAuthClientIdAndSecret idAndSecret = oauthClientManager.createClientSecret(userInfo, id);
		
		// method under test
		assertTrue(oauthClientManager.validateClientCredentials(idAndSecret));
		
		assertEquals(id, idAndSecret.getClient_id());
		assertNotNull(idAndSecret.getClient_secret());
		
		// method under test
		oauthClientManager.deleteOpenIDConnectClient(userInfo, id);
		oauthClientsToDelete.remove(id);
		
		assertThrows(NotFoundException.class, () -> {
			oauthClientManager.getOpenIDConnectClient(userInfo, id);
		});
		
	}

}
