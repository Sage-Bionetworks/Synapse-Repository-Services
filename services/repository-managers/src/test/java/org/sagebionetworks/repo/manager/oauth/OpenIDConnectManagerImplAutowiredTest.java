package org.sagebionetworks.repo.manager.oauth;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.sagebionetworks.repo.model.oauth.OAuthTokenRevocationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCClaimName;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequest;
import org.sagebionetworks.repo.model.oauth.OIDCClaimsRequestDetails;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.model.oauth.TokenTypeHint;
import org.sagebionetworks.repo.web.NotFoundException;
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
	public void testAuthorizationCodeRoundTrip() throws Exception {

		// Verify the client
		oauthClient = oauthClientManager.updateOpenIDConnectClientVerifiedStatus(adminUserInfo, oauthClient.getClient_id(), oauthClient.getEtag(), true);

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(oauthClient.getClient_id());
		authorizationRequest.setRedirectUri(oauthClient.getRedirect_uris().get(0));
		authorizationRequest.setScope(OAuthScope.openid.name() + " " + OAuthScope.offline_access.name());
		authorizationRequest.setResponseType(OAuthResponseType.code);
		Map<String, OIDCClaimsRequestDetails> claimsToRequest = new HashMap<>();
		OIDCClaimsRequestDetails teamClaimRequestDetails = new OIDCClaimsRequestDetails();
		teamClaimRequestDetails.setValues(Collections.singletonList("2"));
		claimsToRequest.put(OIDCClaimName.team.name(), teamClaimRequestDetails);
		OIDCClaimsRequest claimsRequest = new OIDCClaimsRequest();
		claimsRequest.setId_token(claimsToRequest);
		claimsRequest.setUserinfo(claimsToRequest);
		authorizationRequest.setClaims(claimsRequest);

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
				openIDConnectManager.getTokenResponseWithAuthorizationCode(authResponse.getAccess_code(),
						oauthClient.getClient_id(), oauthClient.getRedirect_uris().get(0), OAUTH_ENDPOINT);


		assertNotNull(tokenResponse.getAccess_token());
		assertNotNull(tokenResponse.getId_token());
		assertNotNull(tokenResponse.getRefresh_token());

		oidcTokenHelper.validateJWT(tokenResponse.getId_token());

		// method under test
		JWTWrapper oidcUserInfo = (JWTWrapper) openIDConnectManager.getUserInfo(tokenResponse.getAccess_token(), OAUTH_ENDPOINT);

		oidcTokenHelper.validateJWT(oidcUserInfo.getJwt());

	}

	@Test
	public void testRefreshTokenRoundTrip() throws Exception {
		// Verify the client
		oauthClient = oauthClientManager.updateOpenIDConnectClientVerifiedStatus(adminUserInfo, oauthClient.getClient_id(), oauthClient.getEtag(), true);

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(oauthClient.getClient_id());
		authorizationRequest.setRedirectUri(oauthClient.getRedirect_uris().get(0));
		authorizationRequest.setScope(OAuthScope.openid.name() + " " + OAuthScope.offline_access.name());
		authorizationRequest.setResponseType(OAuthResponseType.code);
		Map<String, OIDCClaimsRequestDetails> claimsToRequest = new HashMap<>();
		OIDCClaimsRequestDetails teamClaimRequestDetails = new OIDCClaimsRequestDetails();
		teamClaimRequestDetails.setValues(Collections.singletonList("2"));
		claimsToRequest.put(OIDCClaimName.team.name(), teamClaimRequestDetails);
		OIDCClaimsRequest claimsRequest = new OIDCClaimsRequest();
		claimsRequest.setId_token(claimsToRequest);
		claimsRequest.setUserinfo(claimsToRequest);
		authorizationRequest.setClaims(claimsRequest);

		// tested in testAuthorizationCodeRoundTrip
		OAuthAuthorizationResponse authResponse = openIDConnectManager.
				authorizeClient(userInfo, authorizationRequest);

		// tested in testAuthorizationCodeRoundTrip
		OIDCTokenResponse tokenResponse =
				openIDConnectManager.getTokenResponseWithAuthorizationCode(authResponse.getAccess_code(),
						oauthClient.getClient_id(), oauthClient.getRedirect_uris().get(0), OAUTH_ENDPOINT);

		// Use the refresh token to get a new access token
		// method under test
		OIDCTokenResponse newTokenResponse =
				openIDConnectManager.getTokenResponseWithRefreshToken(tokenResponse.getRefresh_token(), oauthClient.getClient_id(), null, OAUTH_ENDPOINT);

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
	}

	@Test
	public void testRevokedRefreshToken() throws Exception {
		// Verify the client
		oauthClient = oauthClientManager.updateOpenIDConnectClientVerifiedStatus(adminUserInfo, oauthClient.getClient_id(), oauthClient.getEtag(), true);

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(oauthClient.getClient_id());
		authorizationRequest.setRedirectUri(oauthClient.getRedirect_uris().get(0));
		authorizationRequest.setScope(OAuthScope.openid.name() + " " + OAuthScope.offline_access.name());
		authorizationRequest.setResponseType(OAuthResponseType.code);
		Map<String, OIDCClaimsRequestDetails> claimsToRequest = new HashMap<>();
		OIDCClaimsRequestDetails teamClaimRequestDetails = new OIDCClaimsRequestDetails();
		teamClaimRequestDetails.setValues(Collections.singletonList("2"));
		claimsToRequest.put(OIDCClaimName.team.name(), teamClaimRequestDetails);
		OIDCClaimsRequest claimsRequest = new OIDCClaimsRequest();
		claimsRequest.setId_token(claimsToRequest);
		claimsRequest.setUserinfo(claimsToRequest);
		authorizationRequest.setClaims(claimsRequest);

		// tested in testAuthorizationCodeRoundTrip
		OAuthAuthorizationResponse authResponse = openIDConnectManager.
				authorizeClient(userInfo, authorizationRequest);

		// tested in testAuthorizationCodeRoundTrip
		OIDCTokenResponse tokenResponse =
				openIDConnectManager.getTokenResponseWithAuthorizationCode(authResponse.getAccess_code(),
						oauthClient.getClient_id(), oauthClient.getRedirect_uris().get(0), OAUTH_ENDPOINT);

		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(tokenResponse.getRefresh_token());
		revocationRequest.setToken_type_hint(TokenTypeHint.refresh_token);

		// method under test
		openIDConnectManager.revokeToken(revocationRequest);

		// Use the refresh token to get a new access token
		// tested in testRefreshTokenRoundTrip
		assertThrows(IllegalArgumentException.class, () ->
				openIDConnectManager.getTokenResponseWithRefreshToken(tokenResponse.getRefresh_token(), oauthClient.getClient_id(), null, OAUTH_ENDPOINT));

	}

	@Test
	public void testAuthorizationRequestWithReservedClientID() {
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(AuthorizationConstants.SYNAPSE_OAUTH_CLIENT_ID);

		assertThrows(IllegalArgumentException.class, () -> openIDConnectManager.authorizeClient(userInfo, authorizationRequest));
	}

	@Test
	public void testRefreshTokenIdClaimInIdToken() {
		/*
		 The refresh_token_id claim is used to add a refresh token ID to the access token.
		 The client can request this claim for the ID token, but nothing should happen.
		 */

		// Verify the client
		oauthClient = oauthClientManager.updateOpenIDConnectClientVerifiedStatus(adminUserInfo, oauthClient.getClient_id(), oauthClient.getEtag(), true);

		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(oauthClient.getClient_id());
		authorizationRequest.setRedirectUri(oauthClient.getRedirect_uris().get(0));
		authorizationRequest.setScope(OAuthScope.openid.name() + " " + OAuthScope.offline_access.name());
		authorizationRequest.setResponseType(OAuthResponseType.code);
		Map<String, OIDCClaimsRequestDetails> claimsToRequest = new HashMap<>();
		claimsToRequest.put(OIDCClaimName.email.name(), null);
		claimsToRequest.put(OIDCClaimName.refresh_token_id.name(), null); // !!
		OIDCClaimsRequest claimsRequest = new OIDCClaimsRequest();
		claimsRequest.setId_token(claimsToRequest);
		claimsRequest.setUserinfo(claimsToRequest);
		authorizationRequest.setClaims(claimsRequest);

		// tested in testAuthorizationCodeRoundTrip
		OAuthAuthorizationResponse authResponse = openIDConnectManager.
				authorizeClient(userInfo, authorizationRequest);

		// method under test
		OIDCTokenResponse tokenResponse =
				openIDConnectManager.getTokenResponseWithAuthorizationCode(authResponse.getAccess_code(),
						oauthClient.getClient_id(), oauthClient.getRedirect_uris().get(0), OAUTH_ENDPOINT);

		// Check the ID token
		oidcTokenHelper.validateJWT(tokenResponse.getId_token());
		Claims idTokenClaims = oidcTokenHelper.parseJWT(tokenResponse.getId_token()).getBody();
		assertTrue(idTokenClaims.containsKey(OIDCClaimName.email.name()));
		assertFalse(idTokenClaims.containsKey(OIDCClaimName.refresh_token_id.name()));

		// Check the userInfo response
		JWTWrapper oidcUserInfo = (JWTWrapper) openIDConnectManager.getUserInfo(tokenResponse.getAccess_token(), OAUTH_ENDPOINT);

		oidcTokenHelper.validateJWT(oidcUserInfo.getJwt());
		Claims userInfoClaims = oidcTokenHelper.parseJWT(oidcUserInfo.getJwt()).getBody();
		assertTrue(userInfoClaims.containsKey(OIDCClaimName.email.name()));
		assertFalse(userInfoClaims.containsKey(OIDCClaimName.refresh_token_id.name()));
	}

	@Test
	public void testGetTokenResponseWithRefreshTokenRollBack() {
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
				openIDConnectManager.getTokenResponseWithAuthorizationCode(authResponse.getAccess_code(),
						oauthClient.getClient_id(), oauthClient.getRedirect_uris().get(0), OAUTH_ENDPOINT);

		// Try to use the refresh token to get a new access token, asking for a different scope than was originally granted
		// method under test
		assertThrows(IllegalArgumentException.class, () ->
				openIDConnectManager.getTokenResponseWithRefreshToken(tokenResponse.getRefresh_token(), oauthClient.getClient_id(), OAuthScope.download.name(), OAUTH_ENDPOINT)
		);

		// Calling again with valid scope will only work if the refresh token was not rotated
		OIDCTokenResponse newTokenResponse =
				openIDConnectManager.getTokenResponseWithRefreshToken(tokenResponse.getRefresh_token(), oauthClient.getClient_id(), "", OAUTH_ENDPOINT);

		assertNotNull(newTokenResponse);
	}
}

