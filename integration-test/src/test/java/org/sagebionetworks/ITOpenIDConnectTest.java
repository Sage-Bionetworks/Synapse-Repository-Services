package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
import org.sagebionetworks.repo.model.oauth.JsonWebKeySet;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthClientIdAndSecret;
import org.sagebionetworks.repo.model.oauth.OAuthClientList;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequestDescription;
import org.sagebionetworks.repo.model.oauth.OIDCSigningAlgorithm;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;
import org.sagebionetworks.repo.model.oauth.OIDConnectConfiguration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;

class ITOpenIDConnectTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static SynapseAdminClient synapseAnonymous;
	private static Long user1ToDelete;
	private static Long user2ToDelete;
	
	private String clientToDelete;
	
	@BeforeAll
	public static void beforeClass() throws Exception {
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
	}
	
	@AfterAll
	public static void afterClass() throws Exception {
		try {
			adminSynapse.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }
		try {
			adminSynapse.deleteUser(user2ToDelete);
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

	
	@Test
	void testRoundTrip() throws Exception {
		OIDConnectConfiguration connectConfig = synapseAnonymous.getOIDConnectConfiguration();
		assertNotNull(connectConfig.getIssuer());
		
		JsonWebKeySet jsonWebKeySet = synapseAnonymous.getOIDCJsonWebKeySet();
		assertFalse(jsonWebKeySet.getKeys().isEmpty());
		
		OAuthClient client = new OAuthClient();
		client.setClient_name("some client");
		client.setRedirect_uris(Collections.singletonList("https://foo.bar.com"));
		client = synapseOne.createOAuthClient(client);
		clientToDelete = client.getClientId();
		
		OAuthClient retrieved = synapseOne.getOAuthClient(client.getClientId());
		assertEquals(client, retrieved);
		
		OAuthClientList clientList = synapseOne.listOAuthClients(null);
		assertEquals(client, clientList.getResults().get(0));
		
		OAuthClientIdAndSecret secret = synapseOne.createOAuthClientSecret(client.getClientId());
		assertEquals(client.getClientId(), secret.getClientId());
		assertNotNull(secret.getClientSecret());
				
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(client.getClientId());
		authorizationRequest.setRedirectUri(client.getRedirect_uris().get(0));
		authorizationRequest.setResponseType(OAuthResponseType.code);
		authorizationRequest.setScope("openid");
		authorizationRequest.setClaims("{\"id_token\":{\"userid\":\"null\"},\"userinfo\":{\"userid\":\"null\"}}");
		
		// Note, we get the authorization description anonymously
		OIDCAuthorizationRequestDescription description = 
				synapseAnonymous.getAuthenticationRequestDescription(authorizationRequest);
		// make sure we got something back
		assertFalse(description.getScope().isEmpty());
		
		OAuthAuthorizationResponse oauthAuthorizationResponse = synapseOne.authorizeClient(authorizationRequest);
		
		// Note, we use Basic auth to authorize the client when asking for an access token
		OIDCTokenResponse tokenResponse = null;
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClientId(), secret.getClientSecret());
			tokenResponse = synapseAnonymous.getTokenResponse(OAuthGrantType.authorization_code, 
					oauthAuthorizationResponse.getAccess_code(), client.getRedirect_uris().get(0), null, null, null);
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
		
		Jwt<JwsHeader, Claims> parsedIdToken = JSONWebTokenHelper.parseJWT(tokenResponse.getId_token(), jsonWebKeySet);
		String myId = synapseOne.getMyProfile().getOwnerId();
		assertEquals(myId, parsedIdToken.getBody().get("userid", String.class));

		// Note, we use a bearer token to authorize the client 
		try {
			synapseAnonymous.setBearerAuthorizationToken(tokenResponse.getAccess_token());
			JSONObject userInfo = synapseAnonymous.getUserInfoAsJSON();
			// spot check verify userInfo
			assertEquals(myId, (String)userInfo.get("userid"), userInfo.toString());
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
		
		retrieved.setUserinfo_signed_response_alg(OIDCSigningAlgorithm.RS256);
		OAuthClient updated = synapseOne.updateOAuthClient(retrieved);
		assertEquals(retrieved.getClient_name(), updated.getClient_name());
		
		// Note, we use a bearer token to authorize the client 
		try {
			synapseAnonymous.setBearerAuthorizationToken(tokenResponse.getAccess_token());
			Jwt<JwsHeader,Claims> userInfo = synapseAnonymous.getUserInfoAsJSONWebToken();
			assertEquals(myId, userInfo.getBody().get("userid", String.class));
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}
		
		synapseOne.deleteOAuthClient(client.getClientId());
		clientToDelete=null;
	}

}
