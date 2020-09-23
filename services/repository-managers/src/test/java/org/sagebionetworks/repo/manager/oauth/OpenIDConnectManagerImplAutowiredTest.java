package org.sagebionetworks.repo.manager.oauth;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.authentication.PersonalAccessTokenManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AccessTokenGenerationRequest;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.model.dbo.persistence.DBOCredential;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTermsOfUseAgreement;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OAuthScope;
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.model.oauth.TokenTypeHint;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.OAuthBadRequestException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.jsonwebtoken.Claims;


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

	@Autowired
	PersonalAccessTokenManager personalAccessTokenManager;

	private UserInfo adminUserInfo;
	private UserInfo userInfo;
	private OAuthClient oauthClient;
	private String fullAccessToken;

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
	
		fullAccessToken = oidcTokenHelper.createTotalAccessToken(userInfo.getId());
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

	private static OIDCAuthorizationRequest createAuthorizationRequest(OAuthClient client) {
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(client.getClient_id());
		authorizationRequest.setRedirectUri(client.getRedirect_uris().get(0));
		authorizationRequest.setScope(OAuthScope.openid.name() + " " + OAuthScope.offline_access.name());
		authorizationRequest.setResponseType(OAuthResponseType.code);
		Map<String, OIDCClaimsRequestDetails> claimsToRequest = new HashMap<>();
		OIDCClaimsRequestDetails teamClaimRequestDetails = new OIDCClaimsRequestDetails();
		teamClaimRequestDetails.setValues(Collections.singletonList("2"));
		claimsToRequest.put(OIDCClaimName.team.name(), teamClaimRequestDetails);
		claimsToRequest.put(OIDCClaimName.refresh_token_id.name(), null);
		OIDCClaimsRequest claimsRequest = new OIDCClaimsRequest();
		claimsRequest.setId_token(claimsToRequest);
		claimsRequest.setUserinfo(claimsToRequest);
		authorizationRequest.setClaims(claimsRequest);
		return authorizationRequest;
	}

	// the business logic is tested in detail in the unit tests.  This just does a basic authorization round-trip.
	@Test
	public void testAuthorizationCodeRoundTrip() throws Exception {

		// Verify the client
		oauthClient = oauthClientManager.updateOpenIDConnectClientVerifiedStatus(adminUserInfo, oauthClient.getClient_id(), oauthClient.getEtag(), true);

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(oauthClient);

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
				openIDConnectManager.generateTokenResponseWithAuthorizationCode(authResponse.getAccess_code(),
						oauthClient.getClient_id(), oauthClient.getRedirect_uris().get(0), OAUTH_ENDPOINT);


		assertNotNull(tokenResponse.getAccess_token());
		assertNotNull(tokenResponse.getId_token());
		assertNotNull(tokenResponse.getRefresh_token());
		assertNotNull(tokenResponse.getExpires_in());

		oidcTokenHelper.validateJWT(tokenResponse.getId_token());

		// method under test
		JWTWrapper oidcUserInfo = (JWTWrapper) openIDConnectManager.getUserInfo(tokenResponse.getAccess_token(), OAUTH_ENDPOINT);

		oidcTokenHelper.validateJWT(oidcUserInfo.getJwt());

	}

	@Test
	public void testRefreshTokenRoundTrip() throws Exception {
		// Verify the client
		oauthClient = oauthClientManager.updateOpenIDConnectClientVerifiedStatus(adminUserInfo, oauthClient.getClient_id(), oauthClient.getEtag(), true);

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(oauthClient);

		// tested in testAuthorizationCodeRoundTrip
		OAuthAuthorizationResponse authResponse = openIDConnectManager.
				authorizeClient(userInfo, authorizationRequest);

		// tested in testAuthorizationCodeRoundTrip
		OIDCTokenResponse tokenResponse =
				openIDConnectManager.generateTokenResponseWithAuthorizationCode(authResponse.getAccess_code(),
						oauthClient.getClient_id(), oauthClient.getRedirect_uris().get(0), OAUTH_ENDPOINT);

		// Use the refresh token to get a new access token
		// method under test
		OIDCTokenResponse newTokenResponse =
				openIDConnectManager.generateTokenResponseWithRefreshToken(tokenResponse.getRefresh_token(), oauthClient.getClient_id(), null, OAUTH_ENDPOINT);

		assertNotNull(newTokenResponse.getAccess_token());
		assertNotEquals(tokenResponse.getAccess_token(), newTokenResponse.getAccess_token());
		assertNotNull(newTokenResponse.getId_token());
		assertNotEquals(tokenResponse.getId_token(), newTokenResponse.getId_token());
		assertNotNull(newTokenResponse.getRefresh_token());
		assertNotEquals(tokenResponse.getRefresh_token(), newTokenResponse.getRefresh_token());

		oidcTokenHelper.validateJWT(newTokenResponse.getId_token());

		// method under test
		JWTWrapper oidcUserInfo = (JWTWrapper) openIDConnectManager.getUserInfo(newTokenResponse.getAccess_token(), OAUTH_ENDPOINT);

		oidcTokenHelper.validateJWT(oidcUserInfo.getJwt());

		// Lastly, we requested the refresh_token_id claim, but we test that this claim doesn't not appear in the ID token or userinfo (because it doesn't make sense)
		Claims idTokenClaims = oidcTokenHelper.parseJWT(tokenResponse.getId_token()).getBody();
		assertFalse(idTokenClaims.containsKey(OIDCClaimName.refresh_token_id.name()));


		Claims userInfoClaims = oidcTokenHelper.parseJWT(oidcUserInfo.getJwt()).getBody();
		assertFalse(userInfoClaims.containsKey(OIDCClaimName.refresh_token_id.name()));

	}

	@Test
	public void testRevokedRefreshToken() throws Exception {
		// Verify the client
		oauthClient = oauthClientManager.updateOpenIDConnectClientVerifiedStatus(adminUserInfo, oauthClient.getClient_id(), oauthClient.getEtag(), true);

		OIDCAuthorizationRequest authorizationRequest = createAuthorizationRequest(oauthClient);

		// tested in testAuthorizationCodeRoundTrip
		OAuthAuthorizationResponse authResponse = openIDConnectManager.
				authorizeClient(userInfo, authorizationRequest);

		// tested in testAuthorizationCodeRoundTrip
		OIDCTokenResponse tokenResponse =
				openIDConnectManager.generateTokenResponseWithAuthorizationCode(authResponse.getAccess_code(),
						oauthClient.getClient_id(), oauthClient.getRedirect_uris().get(0), OAUTH_ENDPOINT);

		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(tokenResponse.getRefresh_token());
		revocationRequest.setToken_type_hint(TokenTypeHint.refresh_token);

		// method under test
		openIDConnectManager.revokeToken(oauthClient.getClient_id(), revocationRequest);

		// Use the refresh token to get a new access token
		// tested in testRefreshTokenRoundTrip
		assertThrows(IllegalArgumentException.class, () ->
				openIDConnectManager.generateTokenResponseWithRefreshToken(tokenResponse.getRefresh_token(), oauthClient.getClient_id(), null, OAUTH_ENDPOINT));

	}

	@Test
	public void testAuthorizationRequestWithReservedClientID() {
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID);

		assertThrows(OAuthBadRequestException.class, () -> openIDConnectManager.authorizeClient(userInfo, authorizationRequest));
	}

	@Test
	public void testRevokeAccessToken_invalidJwt() {
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken("not a JWT");
		revocationRequest.setToken_type_hint(TokenTypeHint.access_token);

		assertThrows(IllegalArgumentException.class, () -> openIDConnectManager.revokeToken(oauthClient.getClient_id(), revocationRequest));
	}

	@Test
	public void testGetTokenResponseWithRefreshTokenRollBackOnInvalidScopeRequest() {
		/*
		 Using a refresh token should force the refresh token to rotate, so the old token no longer works.
		 One exception to this rule is if the client makes an invalid scope request, the refresh token rotation
		 should be rolled back.
		 */

		//  Verify the client
		oauthClient = oauthClientManager.updateOpenIDConnectClientVerifiedStatus(adminUserInfo, oauthClient.getClient_id(), oauthClient.getEtag(), true);

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(oauthClient.getClient_id());
		authorizationRequest.setRedirectUri(oauthClient.getRedirect_uris().get(0));
		authorizationRequest.setScope(OAuthScope.view.name() + " " + OAuthScope.offline_access.name());
		authorizationRequest.setResponseType(OAuthResponseType.code);

		// tested in testAuthorizationCodeRoundTrip
		OAuthAuthorizationResponse authResponse = openIDConnectManager.
				authorizeClient(userInfo, authorizationRequest);

		// tested in testAuthorizationCodeRoundTrip
		OIDCTokenResponse tokenResponse =
				openIDConnectManager.generateTokenResponseWithAuthorizationCode(authResponse.getAccess_code(),
						oauthClient.getClient_id(), oauthClient.getRedirect_uris().get(0), OAUTH_ENDPOINT);

		// Try to use the refresh token to get a new access token, asking for a different scope than was originally granted
		// method under test
		assertThrows(IllegalArgumentException.class, () ->
				openIDConnectManager.generateTokenResponseWithRefreshToken(tokenResponse.getRefresh_token(), oauthClient.getClient_id(), OAuthScope.download.name(), OAUTH_ENDPOINT)
		);

		// Calling again with valid scope will only work if the refresh token was not rotated
		OIDCTokenResponse newTokenResponse =
				openIDConnectManager.generateTokenResponseWithRefreshToken(tokenResponse.getRefresh_token(), oauthClient.getClient_id(), "", OAUTH_ENDPOINT);

		assertNotNull(newTokenResponse);
	}

	@Test
	public void testOidcClaimsRequestStringToPojo() throws Exception {
		/**
		 * In OIDCAuthorizationRequest, the claims field is a JSON map. This field was initially set as a string, and has been
		 * changed to use the schema-to-pojo map type. This test should ensure that this change is not a breaking API change.
		 */

		// We will test all of the hard-coded strings that were previously in the OIDC/OAuth test suites.
		// Also, we will test that we can parse the object itself (OIDCClaimsRequest), as well as an object that contains the changed object (OIDCAuthorizationRequest)
		String claims = "{\"id_token\":{},\"userinfo\":{}}";
		OIDCClaimsRequest expectedClaimsRequest = new OIDCClaimsRequest();
		expectedClaimsRequest.setId_token(Collections.emptyMap());
		expectedClaimsRequest.setUserinfo(Collections.emptyMap());

		OIDCClaimsRequest parsedClaimsRequest = EntityFactory.createEntityFromJSONString(claims, OIDCClaimsRequest.class);
		assertEquals(expectedClaimsRequest, parsedClaimsRequest);

		String authorizationRequest = "{\"claims\": " + claims + "}";
		OIDCAuthorizationRequest expectedAuthzRequest = new OIDCAuthorizationRequest();
		expectedAuthzRequest.setClaims(expectedClaimsRequest);

		OIDCAuthorizationRequest parsedAuthzRequest = EntityFactory.createEntityFromJSONString(authorizationRequest, OIDCAuthorizationRequest.class);
		assertEquals(expectedAuthzRequest, parsedAuthzRequest);


		claims = "{\"id_token\":{\"userid\":null,\"email\":null,\"is_certified\":null,\"team\":{\"values\":[\"2\"]}},"+
				  "\"userinfo\":{\"userid\":null,\"email\":null,\"is_certified\":null,\"team\":{\"values\":[\"2\"]}}}";

		Map<String, OIDCClaimsRequestDetails> claimsMap = new HashMap<>();
		claimsMap.put(OIDCClaimName.userid.name(), null);
		claimsMap.put(OIDCClaimName.email.name(), null);
		claimsMap.put(OIDCClaimName.is_certified.name(), null);
		OIDCClaimsRequestDetails teamClaimReqDetails = new OIDCClaimsRequestDetails();
		teamClaimReqDetails.setValues(Collections.singletonList("2"));
		claimsMap.put(OIDCClaimName.team.name(), teamClaimReqDetails);
		expectedClaimsRequest = new OIDCClaimsRequest();
		expectedClaimsRequest.setUserinfo(claimsMap);
		expectedClaimsRequest.setId_token(claimsMap);

		parsedClaimsRequest = EntityFactory.createEntityFromJSONString(claims, OIDCClaimsRequest.class);
		assertEquals(expectedClaimsRequest, parsedClaimsRequest);

		authorizationRequest = "{\"claims\": " + claims + "}";
		expectedAuthzRequest = new OIDCAuthorizationRequest();
		expectedAuthzRequest.setClaims(expectedClaimsRequest);

		parsedAuthzRequest = EntityFactory.createEntityFromJSONString(authorizationRequest, OIDCAuthorizationRequest.class);
		assertEquals(expectedAuthzRequest, parsedAuthzRequest);
	}

	@Test
	public void testValidatePersonalAccessToken() {
		// Issue a PAT to the user
		String token = personalAccessTokenManager.issueToken(userInfo, fullAccessToken, new AccessTokenGenerationRequest(), OAUTH_ENDPOINT).getToken();

		// method under test
		assertEquals(userInfo.getId().toString(), openIDConnectManager.validateAccessToken(token));

		// Revoke the token
		Claims claims = oidcTokenHelper.parseJWT(token).getBody();
		String tokenId = claims.getId();
		personalAccessTokenManager.revokeToken(userInfo, tokenId);

		// method under test
		assertThrows(ForbiddenException.class, () -> openIDConnectManager.validateAccessToken(token));
	}
}

