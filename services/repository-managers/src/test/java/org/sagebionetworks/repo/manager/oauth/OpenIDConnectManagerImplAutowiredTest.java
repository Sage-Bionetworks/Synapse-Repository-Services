package org.sagebionetworks.repo.manager.oauth;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
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
import org.springframework.test.context.junit.jupiter.SpringExtension;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class OpenIDConnectManagerImplAutowiredTest {
	private static final String CLIENT_NAME = "some client";
	private static final String CLIENT_URI = "https://client.uri.com/index.html";
	private static final String POLICY_URI = "https://client.uri.com/policy.html";
	private static final String TOS_URI = "https://client.uri.com/termsOfService.html";
	private static final List<String> REDIRCT_URIS = Collections.singletonList("https://client.com/redir");
	private static final String OAUTH_ENDPOINT = "https://repo-prod.prod.sagebase.org/auth/v1";
	
	@Autowired
	private UserManager userManager;

	@Autowired
	OAuthClientManager oauthClientManager;
	
	@Autowired
	OpenIDConnectManager openIDConnectManager;
	
	@Autowired
	OIDCTokenHelper oidcTokenHelper;
	
	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private OAuthClient oauthClient;
	
	@BeforeEach
	public void setUp() throws Exception {
		adminUserInfo = userManager.getUserInfo(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		DBOCredential cred = new DBOCredential();
		cred.setSecretKey("");
		NewUser nu = new NewUser();
		nu.setEmail(UUID.randomUUID().toString() + "@test.com");
		nu.setUserName(UUID.randomUUID().toString());
		
		DBOTermsOfUseAgreement tou = new DBOTermsOfUseAgreement();
		tou.setAgreesToTermsOfUse(Boolean.TRUE);
		userInfo = userManager.createOrGetTestUser(adminUserInfo, nu, cred, tou);
		
		
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
		oauthClient = oauthClientManager.createOpenIDConnectClient(userInfo, toCreate);
		assertNotNull(oauthClient.getClient_id());

	}
	
	@AfterEach
	public void tearDown() throws Exception {
		try {
			oauthClientManager.deleteOpenIDConnectClient(adminUserInfo, oauthClient.getClient_id());
		} catch (NotFoundException e) {
			// stale ID, no deletion necessary
		}
		try {
			userManager.deletePrincipal(adminUserInfo, userInfo.getId());
		} catch (DataAccessException e) {
		}
	}

	// the business logic is tested in detail in the unit tests.  This just does a basic authorization round-trip.
	@Test
	public void testAuthorizationRoundTrip() throws Exception {		

		// Verify the client
		oauthClient = oauthClientManager.updateOpenIDConnectClientVerifiedStatus(adminUserInfo, oauthClient.getClient_id(), oauthClient.getEtag(), true);
		
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(oauthClient.getClient_id());
		authorizationRequest.setRedirectUri(oauthClient.getRedirect_uris().get(0));
		authorizationRequest.setScope(OAuthScope.openid.name());
		authorizationRequest.setResponseType(OAuthResponseType.code);
		authorizationRequest.setClaims("{\"id_token\":{\"team\":{\"values\":[\"2\"]}},\"userinfo\":{\"team\":{\"values\":[\"2\"]}}}");
				
		// method under test
		OIDCAuthorizationRequestDescription description = 
				openIDConnectManager.getAuthenticationRequestDescription(authorizationRequest);
		
		assertNotNull(description);
		
		// method under test
		OAuthAuthorizationResponse authResponse = openIDConnectManager.
				authorizeClient(userInfo, authorizationRequest);
		
		assertNotNull(authResponse.getAccess_code());
		
		// method under test
		OIDCTokenResponse tokenResponse = 
				openIDConnectManager.getAccessToken(authResponse.getAccess_code(), 
						oauthClient.getClient_id(), oauthClient.getRedirect_uris().get(0), OAUTH_ENDPOINT);
		
		
		assertNotNull(tokenResponse.getAccess_token());
		assertNotNull(tokenResponse.getId_token());
		
		oidcTokenHelper.validateJWT(tokenResponse.getId_token());
		
		// method under test
		JWTWrapper oidcUserInfo = (JWTWrapper)openIDConnectManager.getUserInfo(tokenResponse.getAccess_token(), OAUTH_ENDPOINT);
		
		oidcTokenHelper.validateJWT(oidcUserInfo.getJwt());
		
	}
	
	@Test
	public void testAuthorizationRequestWithReservedClientID() {
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID);

		assertThrows(IllegalArgumentException.class, ()->openIDConnectManager.authorizeClient(userInfo, authorizationRequest));
	}

}
