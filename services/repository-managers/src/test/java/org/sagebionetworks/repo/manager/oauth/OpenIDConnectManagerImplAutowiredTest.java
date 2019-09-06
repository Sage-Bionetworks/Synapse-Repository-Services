package org.sagebionetworks.repo.manager.oauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class OpenIDConnectManagerImplAutowiredTest {
	private static final String CLIENT_NAME = "some client";
	private static final String CLIENT_URI = "https://client.uri.com/index.html";
	private static final String POLICY_URI = "https://client.uri.com/policy.html";
	private static final String TOS_URI = "https://client.uri.com/termsOfService.html";
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://client.com/redir");
	
	@Autowired
	private UserManager userManager;

	@Autowired
	OpenIDConnectManager openIDConnectManager;
	
	@Autowired
	OIDCTokenHelper oidcTokenHelper;
	
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private List<String> oauthClientsToDelete;
	
	@Before
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
	
	@After
	public void tearDown() throws Exception {
		for(String id: oauthClientsToDelete) {
			try {
				openIDConnectManager.deleteOpenIDConnectClient(adminUserInfo, id);
			} catch (NotFoundException e) {
				// stale ID, no deletion necessary
			}
		}
		try {
			userManager.deletePrincipal(adminUserInfo, userInfo.getId());
		} catch (DataAccessException e) {
			// stale ID, no deletion necessary
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
		OAuthClient created = openIDConnectManager.createOpenIDConnectClient(userInfo, toCreate);
		String id = created.getClientId();
		assertNotNull(id);
		oauthClientsToDelete.add(id);
		
		// method under test
		assertEquals(created, openIDConnectManager.getOpenIDConnectClient(userInfo, id));
		
		// method under test
		OAuthClientList list = openIDConnectManager.listOpenIDConnectClients(userInfo, null);
		assertEquals(1, list.getResults().size());
		assertEquals(created, list.getResults().get(0));
		
		created.setPolicy_uri("http://someOtherPolicyUri.com");
		created.setClient_name("some other name");
		
		// method under test
		OAuthClient updated = openIDConnectManager.updateOpenIDConnectClient(userInfo, created);
		created.setEtag(updated.getEtag());
		created.setModifiedOn(updated.getModifiedOn());
		assertEquals(created, updated);
		
		// method under test
		OAuthClientIdAndSecret idAndSecret = openIDConnectManager.createClientSecret(userInfo, id);
		
		assertEquals(id, idAndSecret.getClientId());
		assertNotNull(idAndSecret.getClientSecret());
		
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(id);
		authorizationRequest.setRedirectUri(created.getRedirect_uris().get(0));
		authorizationRequest.setScope(OAuthScope.openid.name());
		authorizationRequest.setResponseType(OAuthResponseType.code);
		
		
		// method under test
		OIDCAuthorizationRequestDescription description = 
				openIDConnectManager.getAuthenticationRequestDescription(authorizationRequest);
		
		assertNotNull(description);
		
		// method under test
		OAuthAuthorizationResponse authResponse = openIDConnectManager.
				authorizeClient(userInfo, authorizationRequest);
		
		assertNotNull(authResponse.getAccess_code());
		
		String oauthEndpoint = "https://repo-prod.prod.sagebase.org/auth/v1";
		
		// method under test
		OIDCTokenResponse tokenResponse = 
				openIDConnectManager.getAccessToken(authResponse.getAccess_code(), 
				id, created.getRedirect_uris().get(0), oauthEndpoint);
		
		
		assertNotNull(tokenResponse.getAccess_token());
		assertNotNull(tokenResponse.getId_token());
		
		oidcTokenHelper.validateJWT(tokenResponse.getId_token());
		Jwt<JwsHeader,Claims> accessToken = oidcTokenHelper.parseJWT(tokenResponse.getAccess_token());
		
		String oidcUserInfo = (String)openIDConnectManager.getUserInfo(accessToken, oauthEndpoint);
		
		oidcTokenHelper.validateJWT(oidcUserInfo);
		
		// method under test
		openIDConnectManager.deleteOpenIDConnectClient(userInfo, id);
		oauthClientsToDelete.remove(id);
		
		try {
			openIDConnectManager.getOpenIDConnectClient(userInfo, id);
			fail("NotFoundException expected");
		} catch (NotFoundException e) {
			// as expected
		}
		
	}

}
