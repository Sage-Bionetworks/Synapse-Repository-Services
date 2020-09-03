package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseBadRequestException;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.OAuthErrorResponse;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientAuthorizationHistoryList;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformation;
import org.sagebionetworks.repo.model.oauth.OAuthRefreshTokenInformationList;
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
import org.sagebionetworks.repo.model.oauth.OIDConnectConfiguration;
import org.sagebionetworks.repo.model.oauth.TokenTypeHint;
import org.sagebionetworks.repo.web.OAuthErrorCode;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClient;
import org.sagebionetworks.simpleHttpClient.SimpleHttpClientImpl;
import org.sagebionetworks.simpleHttpClient.SimpleHttpRequest;
import org.sagebionetworks.simpleHttpClient.SimpleHttpResponse;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

public class ITOpenIDConnectTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static SynapseAdminClient synapseAnonymous;
	private static Long user1ToDelete;
	private static Long user2ToDelete;
	private static StackConfiguration config;
	private static SimpleHttpClient simpleClient;

	private String clientToDelete;
	
	@BeforeAll
	public static void beforeClass() throws Exception {
		config = StackConfigurationSingleton.singleton();
		// Create 2 users
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapseOne = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseOne);
		user1ToDelete = SynapseClientHelper.createUser(adminSynapse, synapseOne);
		
		synapseAnonymous = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(synapseAnonymous);

		simpleClient = new SimpleHttpClientImpl();
	}
	
	@AfterAll
	public static void afterClass() throws Exception {
		try {
			if (user1ToDelete!=null) adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }
		try {
			if (user2ToDelete!=null) adminSynapse.deleteUser(user2ToDelete);
		} catch (SynapseException e) { }
	}

	@AfterEach
	public void after() throws Exception {
		try {
			if (clientToDelete!=null) {
				synapseOne.deleteOAuthClient(clientToDelete);
			}
		} catch (SynapseException e) {
			// already gone
		}
	}

	private OAuthClient setUpOAuthClient(boolean verified) throws SynapseException {
		OAuthClient client = new OAuthClient();
		client.setClient_name("some client");
		client.setRedirect_uris(Collections.singletonList("https://foo.bar.com"));
		client = synapseOne.createOAuthClient(client);
		clientToDelete = client.getClient_id();
		if (verified) {
			return adminSynapse.updateOAuthClientVerifiedStatus(client.getClient_id(), client.getEtag(), true);
		} else {
			return client;
		}
	}

	@Test
	public void testClientNotVerified() throws Exception {
		OAuthClient client = setUpOAuthClient(false);

		assertFalse(client.getVerified());
		
		OAuthClientIdAndSecret secret = synapseOne.createOAuthClientSecret(client.getClient_id());
		assertEquals(client.getClient_id(), secret.getClient_id());
		assertNotNull(secret.getClient_secret());
		
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(client.getClient_id());
		authorizationRequest.setRedirectUri(client.getRedirect_uris().get(0));
		authorizationRequest.setResponseType(OAuthResponseType.code);
		authorizationRequest.setScope("openid");
		Map<String, OIDCClaimsRequestDetails> claimsToRequest = new HashMap<>();
		claimsToRequest.put(OIDCClaimName.userid.name(), null);
		claimsToRequest.put(OIDCClaimName.email.name(), null);
		claimsToRequest.put(OIDCClaimName.is_certified.name(), null);
		OIDCClaimsRequestDetails teamClaimRequestDetails = new OIDCClaimsRequestDetails();
		teamClaimRequestDetails.setValues(Collections.singletonList("2"));
		claimsToRequest.put(OIDCClaimName.team.name(), teamClaimRequestDetails);
		OIDCClaimsRequest claimsRequest = new OIDCClaimsRequest();
		claimsRequest.setId_token(claimsToRequest);
		claimsRequest.setUserinfo(claimsToRequest);
		authorizationRequest.setClaims(claimsRequest);


		// ---- Auth Description Test ----
		
		// The client is not verified, we cannot get the description
		SynapseForbiddenException ex = assertThrows(SynapseForbiddenException.class, () -> {
				synapseAnonymous.getAuthenticationRequestDescription(authorizationRequest);
		});
		
		assertEquals("The OAuth client (" + client.getClient_id() + ") is not verified.", ex.getMessage());
		
		// Re-read the client to fetch the latest etag (changed above when the secret was generated)
		client = synapseOne.getOAuthClient(client.getClient_id());
		// Verify the client
		client = adminSynapse.updateOAuthClientVerifiedStatus(client.getClient_id(), client.getEtag(), true);
		
		// This goes through
		synapseAnonymous.getAuthenticationRequestDescription(authorizationRequest);
		
		// ---- Authorization Code Test ----
		
		// Remove the client verification
		client = adminSynapse.updateOAuthClientVerifiedStatus(client.getClient_id(), client.getEtag(), false);
		
		// The client is not verified, we cannot authorize the request
		ex = assertThrows(SynapseForbiddenException.class, () -> {
			synapseOne.authorizeClient(authorizationRequest);
		});
		
		assertEquals("The OAuth client (" + client.getClient_id() + ") is not verified.", ex.getMessage());

		// Verify the client
		client = adminSynapse.updateOAuthClientVerifiedStatus(client.getClient_id(), client.getEtag(), true);
		
		// We should now be able to authorize the request
		OAuthAuthorizationResponse oauthAuthorizationResponse = synapseOne.authorizeClient(authorizationRequest);
		
		// Un-verify the client
		client = adminSynapse.updateOAuthClientVerifiedStatus(client.getClient_id(), client.getEtag(), false);
		
		String accessCode = oauthAuthorizationResponse.getAccess_code();
		String redirectUri = client.getRedirect_uris().get(0);
		// Note, we use Basic auth to authorize the client when asking for an access token
		OIDCTokenResponse tokenResponse = null;
		
		
		// ---- Token Exchange TEST ----
		
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			
			ex = assertThrows(SynapseForbiddenException.class, () -> {
				synapseAnonymous.getTokenResponse(OAuthGrantType.authorization_code, 
						accessCode, redirectUri, null, null, null);
			});
			
			assertEquals("The OAuth client (" + client.getClient_id() + ") is not verified.", ex.getMessage());
			
			// Verify the client once again
			client = adminSynapse.updateOAuthClientVerifiedStatus(client.getClient_id(), client.getEtag(), true);
			
			tokenResponse = synapseAnonymous.getTokenResponse(OAuthGrantType.authorization_code, 
					accessCode, redirectUri, null, null, null);
			
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
		
		// ---- User Info TEST ----
		
		// Un-verify the client
		client = adminSynapse.updateOAuthClientVerifiedStatus(client.getClient_id(), client.getEtag(), false);
		
		// Note, we use a bearer token to authorize the client 
		try {
			synapseAnonymous.setBearerAuthorizationToken(tokenResponse.getAccess_token());
			
			SynapseUnauthorizedException uex = assertThrows(SynapseUnauthorizedException.class, () -> {
				synapseAnonymous.getUserInfoAsJSON();
			});
			
			assertEquals("The OAuth client (" + client.getClient_id() + ") is not verified.", uex.getMessage());
			
			// Verify the client once again
			client = adminSynapse.updateOAuthClientVerifiedStatus(client.getClient_id(), client.getEtag(), true);
			
			// Now we should be able to get the user info
			synapseAnonymous.getUserInfoAsJSON();
			
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
	}
	
	@Test
	public void testRoundTrip() throws Exception {
		OIDConnectConfiguration connectConfig = synapseAnonymous.getOIDConnectConfiguration();
		assertNotNull(connectConfig.getIssuer());
		
		JsonWebKeySet jsonWebKeySet = synapseAnonymous.getOIDCJsonWebKeySet();
		assertFalse(jsonWebKeySet.getKeys().isEmpty());
		
		OAuthClient client = setUpOAuthClient(false);
		
		assertEquals(client, synapseOne.getOAuthClient(client.getClient_id()));
		assertFalse(client.getVerified());
		
		// Sets the verified status of the client (only admins and ACT can do this)
		client = adminSynapse.updateOAuthClientVerifiedStatus(client.getClient_id(), client.getEtag(), true);
		assertTrue(client.getVerified());
		
		// Re-read the client as the user
		assertEquals(client, synapseOne.getOAuthClient(client.getClient_id()));
		assertTrue(client.getVerified());
		
		OAuthClientList clientList = synapseOne.listOAuthClients(null);
		assertEquals(client, clientList.getResults().get(0));
		
		OAuthClientIdAndSecret secret = synapseOne.createOAuthClientSecret(client.getClient_id());
		assertEquals(client.getClient_id(), secret.getClient_id());
		assertNotNull(secret.getClient_secret());
				
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(client.getClient_id());
		authorizationRequest.setRedirectUri(client.getRedirect_uris().get(0));
		authorizationRequest.setResponseType(OAuthResponseType.code);
		authorizationRequest.setScope("openid");

		Map<String, OIDCClaimsRequestDetails> claimsToRequest = new HashMap<>();
		claimsToRequest.put(OIDCClaimName.userid.name(), null);
		claimsToRequest.put(OIDCClaimName.email.name(), null);
		claimsToRequest.put(OIDCClaimName.is_certified.name(), null);
		OIDCClaimsRequestDetails teamClaimRequestDetails = new OIDCClaimsRequestDetails();
		teamClaimRequestDetails.setValues(Collections.singletonList("2"));
		claimsToRequest.put(OIDCClaimName.team.name(), teamClaimRequestDetails);
		OIDCClaimsRequest claimsRequest = new OIDCClaimsRequest();
		claimsRequest.setId_token(claimsToRequest);
		claimsRequest.setUserinfo(claimsToRequest);
		authorizationRequest.setClaims(claimsRequest);

		String nonce = UUID.randomUUID().toString();
		authorizationRequest.setNonce(nonce);
		
		// Note, we get the authorization description anonymously
		OIDCAuthorizationRequestDescription description = 
				synapseAnonymous.getAuthenticationRequestDescription(authorizationRequest);
		// make sure we got something back
		assertFalse(description.getScope().isEmpty());
		
		// We have not yet authorized the client
		assertFalse(synapseOne.hasUserAuthorizedClient(authorizationRequest));
		
		OAuthAuthorizationResponse oauthAuthorizationResponse = synapseOne.authorizeClient(authorizationRequest);
		
		// Now we HAVE authorized the client
		assertTrue(synapseOne.hasUserAuthorizedClient(authorizationRequest));
		
		// Note, we use Basic auth to authorize the client when asking for an access token
		OIDCTokenResponse tokenResponse = null;
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			tokenResponse = synapseAnonymous.getTokenResponse(OAuthGrantType.authorization_code, 
					oauthAuthorizationResponse.getAccess_code(), client.getRedirect_uris().get(0), null, null, null);
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

		// we can also authorize the client using client_secret_post
		{
			SimpleHttpRequest request = new SimpleHttpRequest();
			request.setUri(config.getAuthenticationServicePublicEndpoint()+"/oauth2/token");
			Map<String, String> requestHeaders = new HashMap<String, String>();
			requestHeaders.put("Content-Type", "application/x-www-form-urlencoded");
			request.setHeaders(requestHeaders);
			String requestBody = "client_id="+client.getClient_id()+
					"&client_secret="+secret.getClient_secret()+
					"&grant_type=authorization_code"+
					"&code="+oauthAuthorizationResponse.getAccess_code()+
					"&redirect_uri="+client.getRedirect_uris().get(0);
			SimpleHttpResponse response = simpleClient.post(request, requestBody);
			assertEquals(HttpStatus.SC_OK, response.getStatusCode());
			assertNotNull(response.getContent());
		}

		Jwt<JwsHeader, Claims> parsedIdToken = JSONWebTokenHelper.parseJWT(tokenResponse.getId_token(), jsonWebKeySet);
		UserProfile myProfile = synapseOne.getMyProfile();
		String myId = myProfile.getOwnerId();
		Claims idClaims = parsedIdToken.getBody();
		assertEquals(myId, idClaims.get("userid", String.class));
		assertTrue(idClaims.get("is_certified", Boolean.class));
		String email = myProfile.getEmails().get(0);
		assertEquals(email, idClaims.get("email", String.class));
		assertEquals(Collections.EMPTY_LIST, idClaims.get("team", List.class));
		assertEquals(nonce, idClaims.get("nonce"));
		
		// the access token encodes claims we can refresh
		Jwt<JwsHeader, Claims> parsedAccessToken = JSONWebTokenHelper.parseJWT(tokenResponse.getAccess_token(), jsonWebKeySet);
		Claims accessClaims = parsedAccessToken.getBody();
		Map access = (Map)accessClaims.get("access", Map.class);
		List<String> userInfoScope = (List<String>)access.get("scope");
		assertEquals(1, userInfoScope.size());
		assertEquals(OAuthScope.openid.name(), userInfoScope.get(0));
		Map userInfoClaims = (Map)access.get("oidc_claims");
		assertTrue(userInfoClaims.containsKey("userid"));
		assertTrue(userInfoClaims.containsKey("email"));
		assertTrue(userInfoClaims.containsKey("is_certified"));
		assertTrue(userInfoClaims.containsKey("team"));

		// Note, we use a bearer token to authorize the client 
		try {
			synapseAnonymous.setBearerAuthorizationToken(tokenResponse.getAccess_token());
			JSONObject userInfo = synapseAnonymous.getUserInfoAsJSON();
			// check userInfo
			assertEquals(myId, (String)userInfo.get("userid"));
			assertEquals(email, (String)userInfo.get("email"));
			assertTrue((Boolean)userInfo.get("is_certified"));
			assertEquals(0, ((JSONArray)userInfo.get("team")).length());
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
		
		OAuthClient retrieved = synapseOne.getOAuthClient(client.getClient_id());
		retrieved.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);
		OAuthClient updated = synapseOne.updateOAuthClient(retrieved);
		assertEquals(retrieved.getClient_name(), updated.getClient_name());
		
		// Note, we use a bearer token to authorize the client 
		try {
			synapseAnonymous.setBearerAuthorizationToken(tokenResponse.getAccess_token());
			Jwt<JwsHeader,Claims> userInfo = synapseAnonymous.getUserInfoAsJSONWebToken();
			Claims body = userInfo.getBody();
			assertEquals(myId, body.get("userid", String.class));
			assertEquals(email, body.get("email", String.class));
			assertTrue(body.get("is_certified", Boolean.class));
			assertEquals(Collections.EMPTY_LIST, body.get("team", List.class));
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
	}

	private static OIDCAuthorizationRequest setUpAuthorizationRequest(OAuthClient client) throws Exception {
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(client.getClient_id());
		authorizationRequest.setRedirectUri(client.getRedirect_uris().get(0));
		authorizationRequest.setResponseType(OAuthResponseType.code);
		authorizationRequest.setScope("openid offline_access"); // offline_access is needed for a refresh token

		Map<String, OIDCClaimsRequestDetails> claimsToRequest = new HashMap<>();
		claimsToRequest.put(OIDCClaimName.userid.name(), null);
		claimsToRequest.put(OIDCClaimName.email.name(), null);
		claimsToRequest.put(OIDCClaimName.is_certified.name(), null);
		OIDCClaimsRequestDetails teamClaimRequestDetails = new OIDCClaimsRequestDetails();
		teamClaimRequestDetails.setValues(Collections.singletonList("2"));
		claimsToRequest.put(OIDCClaimName.team.name(), teamClaimRequestDetails);
		OIDCClaimsRequest claimsRequest = new OIDCClaimsRequest();
		claimsRequest.setId_token(claimsToRequest);
		claimsRequest.setUserinfo(claimsToRequest);
		authorizationRequest.setClaims(claimsRequest);

		String nonce = UUID.randomUUID().toString();
		authorizationRequest.setNonce(nonce);
		return authorizationRequest;
	}

	@Test
	public void testRefreshTokenGrantTypeRoundTrip() throws Exception {
		OAuthClient client = setUpOAuthClient(true);
		OAuthClientIdAndSecret secret = synapseOne.createOAuthClientSecret(client.getClient_id());

		OIDCAuthorizationRequest authorizationRequest = setUpAuthorizationRequest(client);

		OAuthAuthorizationResponse oauthAuthorizationResponse = synapseOne.authorizeClient(authorizationRequest);

		// Note, we use Basic auth to authorize the client when asking for an access token
		OIDCTokenResponse tokenResponse = null;
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			tokenResponse = synapseAnonymous.getTokenResponse(OAuthGrantType.authorization_code,
					oauthAuthorizationResponse.getAccess_code(), client.getRedirect_uris().get(0), null, null, null);
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

		// get another access token using our refresh token
		OIDCTokenResponse newTokenResponse = null;
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			newTokenResponse = synapseAnonymous.getTokenResponse(OAuthGrantType.refresh_token, null,
					client.getRedirect_uris().get(0), tokenResponse.getRefresh_token(), null, null);

		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
		// The refresh token should be rotated
		assertFalse(StringUtils.isBlank(newTokenResponse.getRefresh_token()));
		assertNotEquals(tokenResponse.getRefresh_token(), newTokenResponse.getRefresh_token());

		// New ID and access tokens should be retrieved
		assertFalse(StringUtils.isBlank(newTokenResponse.getId_token()));
		assertNotEquals(tokenResponse.getId_token(), newTokenResponse.getId_token());
		assertFalse(StringUtils.isBlank(newTokenResponse.getAccess_token()));
		assertNotEquals(tokenResponse.getAccess_token(), newTokenResponse.getAccess_token());

		// The old refresh token shouldn't work anymore.
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			String clientUri = client.getRedirect_uris().get(0);
			String oldRefreshToken = tokenResponse.getRefresh_token();
			assertThrows(SynapseBadRequestException.class, () ->
					synapseAnonymous.getTokenResponse(OAuthGrantType.refresh_token, null,
					clientUri, oldRefreshToken, null, null));
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

		// Both the old and the new access tokens should work
		try { // Get userInfo using old access token
			synapseAnonymous.setBearerAuthorizationToken(tokenResponse.getAccess_token());
			JSONObject userInfo = synapseAnonymous.getUserInfoAsJSON();
			assertTrue((Boolean)userInfo.get("is_certified"));
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

		// The old refresh token should not work
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			String clientUri = client.getRedirect_uris().get(0);
			String refreshToken = tokenResponse.getRefresh_token();
			assertThrows(SynapseBadRequestException.class, () ->
					synapseAnonymous.getTokenResponse(OAuthGrantType.refresh_token, null,
							clientUri, refreshToken, null, null)
			);
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}


		try { // Get userInfo using new access token
			synapseAnonymous.setBearerAuthorizationToken(newTokenResponse.getAccess_token());
			JSONObject userInfo = synapseAnonymous.getUserInfoAsJSON();
			assertTrue((Boolean)userInfo.get("is_certified"));
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

		// Audit clients -- there should only be one active client
		// Call under test
		OAuthClientAuthorizationHistoryList authzHistory = synapseOne.getClientAuthorizationHistory(null);
		assertEquals(1, authzHistory.getResults().size());
		assertNull(authzHistory.getNextPageToken());
		assertEquals(client.getClient_id(), authzHistory.getResults().get(0).getClient().getClient_id());

		// Audit tokens -- there should only be one token for the sole active client
		// Call under test
		OAuthRefreshTokenInformationList tokenList = synapseOne.getRefreshTokenMetadataForAuthorizedClient(client.getClient_id(), null);
		assertEquals(1, tokenList.getResults().size());
		assertNull(tokenList.getNextPageToken());
		assertEquals(synapseOne.getMyProfile().getOwnerId(), tokenList.getResults().get(0).getPrincipalId());
		assertEquals(client.getClient_id(), tokenList.getResults().get(0).getClientId());
		assertEquals(authorizationRequest.getClaims(), tokenList.getResults().get(0).getClaims());

		// Retrieving the refresh token metadata should yield the same result
		OAuthRefreshTokenInformation metadata = synapseOne.getRefreshTokenMetadata(tokenList.getResults().get(0).getTokenId());
		assertEquals(tokenList.getResults().get(0), metadata);

		// Rename the refresh token
		metadata.setName("a new refresh token name");
		// Call under test
		synapseOne.updateRefreshTokenMetadata(metadata);
		OAuthRefreshTokenInformation newMetadata = synapseOne.getRefreshTokenMetadata(tokenList.getResults().get(0).getTokenId());

		assertEquals(metadata.getName(), newMetadata.getName());

		// The OAuth client should be able to get the refresh token metadata as well
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			assertEquals(newMetadata, synapseAnonymous.getRefreshTokenMetadataAsOAuthClient(metadata.getTokenId()));
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

		// Revoke the refresh token
		synapseOne.revokeRefreshToken(metadata.getTokenId());

		// Client should be unable to use access token
		try {
			synapseAnonymous.setBearerAuthorizationToken(tokenResponse.getAccess_token());
			assertThrows(SynapseUnauthorizedException.class, () ->
					synapseAnonymous.getUserInfoAsJSONWebToken()
			);
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

		// Client should be unable to use refresh token
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			String clientUri = client.getRedirect_uris().get(0);
			String refreshToken = newTokenResponse.getRefresh_token();
			assertThrows(SynapseBadRequestException.class, () ->
					synapseAnonymous.getTokenResponse(OAuthGrantType.refresh_token, null,
							clientUri, refreshToken, null, null)
			);
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
	}

	@Test
	public void testRevokeAllTokensFromClient() throws Exception {
		// START Set up, use authorization code
		OAuthClient client = setUpOAuthClient(true);
		OAuthClientIdAndSecret secret = synapseOne.createOAuthClientSecret(client.getClient_id());

		OIDCAuthorizationRequest authorizationRequest = setUpAuthorizationRequest(client);

		OAuthAuthorizationResponse oauthAuthorizationResponse = synapseOne.authorizeClient(authorizationRequest);

		// Note, we use Basic auth to authorize the client when asking for an access token
		OIDCTokenResponse tokenResponse = null;
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			tokenResponse = synapseAnonymous.getTokenResponse(OAuthGrantType.authorization_code,
					oauthAuthorizationResponse.getAccess_code(), client.getRedirect_uris().get(0), null, null, null);
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
		// END Set up. We now have a refresh token

		OAuthRefreshTokenInformationList tokens = synapseOne.getRefreshTokenMetadataForAuthorizedClient(client.getClient_id(), null);
		String tokenId = tokens.getResults().get(0).getTokenId();

		// Test revoking all refresh tokens from a client
		assertTrue(synapseOne.hasUserAuthorizedClient(authorizationRequest));

		// Call under test
		synapseOne.revokeMyRefreshTokensFromClient(client.getClient_id());

		assertFalse(synapseOne.hasUserAuthorizedClient(authorizationRequest));

		assertThrows(SynapseNotFoundException.class, () ->
				synapseOne.getRefreshTokenMetadata(tokenId));
	}

	@Test
	public void testRevokeTokensViaClient() throws Exception {
		// START Set up, use authorization code
		OAuthClient client = setUpOAuthClient(true);
		OAuthClientIdAndSecret secret = synapseOne.createOAuthClientSecret(client.getClient_id());

		OIDCAuthorizationRequest authorizationRequest = setUpAuthorizationRequest(client);

		OAuthAuthorizationResponse oauthAuthorizationResponse = synapseOne.authorizeClient(authorizationRequest);

		// Note, we use Basic auth to authorize the client when asking for an access token
		OIDCTokenResponse tokenResponse = null;
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			tokenResponse = synapseAnonymous.getTokenResponse(OAuthGrantType.authorization_code,
					oauthAuthorizationResponse.getAccess_code(), client.getRedirect_uris().get(0), null, null, null);
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
		// END Set up. We now have a refresh token

		OAuthRefreshTokenInformationList tokens = synapseOne.getRefreshTokenMetadataForAuthorizedClient(client.getClient_id(), null);
		String tokenId = tokens.getResults().get(0).getTokenId();

		// Test revoking a token as a client
		OAuthTokenRevocationRequest revocationRequest = new OAuthTokenRevocationRequest();
		revocationRequest.setToken(tokenResponse.getRefresh_token());
		revocationRequest.setToken_type_hint(TokenTypeHint.refresh_token);
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret.getClient_secret());
			// Call under test
			synapseAnonymous.revokeToken(revocationRequest);
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

		assertThrows(SynapseNotFoundException.class, () ->
				synapseOne.getRefreshTokenMetadata(tokenId));
	}
}
