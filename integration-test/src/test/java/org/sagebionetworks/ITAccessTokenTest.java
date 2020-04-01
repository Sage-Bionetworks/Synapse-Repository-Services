package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.oauth.OAuthAuthorizationResponse;
import org.sagebionetworks.repo.model.oauth.OAuthClient;
import org.sagebionetworks.repo.model.oauth.OAuthGrantType;
import org.sagebionetworks.repo.model.oauth.OAuthResponseType;
import org.sagebionetworks.repo.model.oauth.OIDCAuthorizationRequest;
import org.sagebionetworks.repo.model.oauth.OIDCTokenResponse;

/*
 * This is a simple test to make sure that the OAuth access token authorization is connected properly.
 */
public class ITAccessTokenTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapseOne;
	private static SynapseClient synapseAnonymous;
	private static Long user1ToDelete;
	private static Long user2ToDelete;

	private OAuthClient client;
	private String secret;

	private Project project;

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

		synapseAnonymous = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseAnonymous);
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

	@BeforeEach
	public void before() throws Exception {
		// create the OAuth client
		client = new OAuthClient();
		client.setClient_name(UUID.randomUUID().toString());
		client.setRedirect_uris(Collections.singletonList("https://foo.bar.com"));
		client = synapseOne.createOAuthClient(client);
		// Sets the verified status of the client (only admins and ACT can do this)
		client = adminSynapse.updateOAuthClientVerifiedStatus(client.getClient_id(), client.getEtag(), true);
		secret = synapseOne.createOAuthClientSecret(client.getClient_id()).getClient_secret();
	}

	@AfterEach
	public void after() throws Exception {
		try {
			if (project!=null) adminSynapse.deleteEntity(project);
		} catch (SynapseException e) { }
		try {
			if (client!=null) {
				synapseOne.deleteOAuthClient(client.getClient_id());
			}
		} catch (SynapseException e) {
			e.printStackTrace();
		}
	}

	private String getAccessToken(String scopes) throws Exception {
		OIDCAuthorizationRequest authorizationRequest = new OIDCAuthorizationRequest();
		authorizationRequest.setClientId(client.getClient_id());
		authorizationRequest.setRedirectUri(client.getRedirect_uris().get(0));
		authorizationRequest.setResponseType(OAuthResponseType.code);
		authorizationRequest.setScope(scopes);
		authorizationRequest.setClaims("{\"id_token\":{},\"userinfo\":{}}");
		String nonce = UUID.randomUUID().toString();
		authorizationRequest.setNonce(nonce);		
		OAuthAuthorizationResponse oauthAuthorizationResponse = synapseOne.authorizeClient(authorizationRequest);

		// Note, we use Basic auth to authorize the client when asking for an access token
		OIDCTokenResponse tokenResponse = null;
		try {
			synapseAnonymous.setBasicAuthorizationCredentials(client.getClient_id(), secret);
			tokenResponse = synapseAnonymous.getTokenResponse(OAuthGrantType.authorization_code, 
					oauthAuthorizationResponse.getAccess_code(), client.getRedirect_uris().get(0), null, null, null);
			return tokenResponse.getAccess_token();
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

	}


	@Test
	public void testAccessToken() throws Exception {
		String accessToken = getAccessToken("openid modify view download");

		try {
			// We use the bearer token to authorize the client 
			synapseAnonymous.setBearerAuthorizationToken(accessToken);
			project = new Project();
			project.setName("access token test");
			project = synapseAnonymous.createEntity(project);
			assertNotNull(project.getId());
			project = synapseAnonymous.getEntity(project.getId(), Project.class);
			
			// But if we don't have 'view' scope we can't get the entity
			String accessToken2 = getAccessToken("openid modify download");
			synapseAnonymous.setBearerAuthorizationToken(accessToken2);
			Assertions.assertThrows(SynapseForbiddenException.class, () -> {
				project = synapseAnonymous.getEntity(project.getId(), Project.class);				
			});
			
		} finally {
			synapseAnonymous.removeAuthorizationHeader();
		}

	}

}
