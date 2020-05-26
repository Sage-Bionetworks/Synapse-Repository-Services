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
import org.sagebionetworks.repo.model.oauth.OAuthClient;

/*
 * This is a simple test to make sure that the OAuth access token authorization is connected properly.
 */
public class ITAccessTokenTest {

	private static SynapseAdminClient synapseClientForAdmin;
	private static SynapseClient synapseClientForUser1;
	private static SynapseClient synapseClientLackingCredentials;
	
	private static Long user1ToDelete;
	
	private OAuthClient oauthClient;
	private String oauthClientSecret;

	private Project project;

	@BeforeAll
	public static void beforeClass() throws Exception {
		// Create 2 users
		synapseClientForAdmin = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(synapseClientForAdmin);
		synapseClientForAdmin.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		synapseClientForAdmin.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		synapseClientForAdmin.clearAllLocks();
		synapseClientForUser1 = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseClientForUser1);
		user1ToDelete = SynapseClientHelper.createUser(synapseClientForAdmin, synapseClientForUser1);

		synapseClientLackingCredentials = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(synapseClientLackingCredentials);
	}

	@AfterAll
	public static void afterClass() throws Exception {
		try {
			if (user1ToDelete!=null) synapseClientForAdmin.deleteUser(user1ToDelete);
		} catch (SynapseException e) { }
	}

	@BeforeEach
	public void before() throws Exception {
		// create the OAuth client
		oauthClient = new OAuthClient();
		oauthClient.setClient_name(UUID.randomUUID().toString());
		oauthClient.setRedirect_uris(Collections.singletonList("https://foo.bar.com"));
		oauthClient = synapseClientForUser1.createOAuthClient(oauthClient);
		// Sets the verified status of the client (only admins and ACT can do this)
		oauthClient = synapseClientForAdmin.updateOAuthClientVerifiedStatus(oauthClient.getClient_id(), oauthClient.getEtag(), true);
		oauthClientSecret = synapseClientForUser1.createOAuthClientSecret(oauthClient.getClient_id()).getClient_secret();
	}

	@AfterEach
	public void after() throws Exception {
		try {
			if (project!=null) synapseClientForAdmin.deleteEntity(project);
		} catch (SynapseException e) { }
		try {
			if (oauthClient!=null) {
				synapseClientForUser1.deleteOAuthClient(oauthClient.getClient_id());
			}
		} catch (SynapseException e) {
			e.printStackTrace();
		}
	}
	
	private String getAccessTokenForUser1(String scope) throws SynapseException {
		return OAuthHelper.getAccessToken(
					synapseClientForUser1, 
					synapseClientLackingCredentials, 
					oauthClient.getClient_id(), 
					oauthClientSecret, 
					oauthClient.getRedirect_uris().get(0),
					scope
				);
	}

	@Test
	public void testAccessToken() throws Exception {
		String accessTokenForUser1 = getAccessTokenForUser1("openid modify view download");

		try {
			// We use the bearer token to authorize the client 
			synapseClientLackingCredentials.setBearerAuthorizationToken(accessTokenForUser1);
			// Now the calls made by 'synapseClientLackingCredentials' are authenticated/authorized
			// as User1.

			project = new Project();
			project.setName("access token test");
			project = synapseClientLackingCredentials.createEntity(project);
			assertNotNull(project.getId());
			project = synapseClientLackingCredentials.getEntity(project.getId(), Project.class);
			
			// But if we don't have 'view' scope we can't get the entity
			String accessToken2 = getAccessTokenForUser1("openid modify download");
			synapseClientLackingCredentials.setBearerAuthorizationToken(accessToken2);
			Assertions.assertThrows(SynapseForbiddenException.class, () -> {
				project = synapseClientLackingCredentials.getEntity(project.getId(), Project.class);				
			});
			
			// Same result if we make the call anonymously (no access token)
			synapseClientLackingCredentials.removeAuthorizationHeader();
			Assertions.assertThrows(SynapseForbiddenException.class, () -> {
				project = synapseClientLackingCredentials.getEntity(project.getId(), Project.class);				
			});
			// note that we CAN make anonymous requests that require no authentication
			assertNotNull(synapseClientLackingCredentials.getVersionInfo());
		} finally {
			synapseClientLackingCredentials.removeAuthorizationHeader();
		}

	}
	
	@Test
	public void testAccessTokenWithSpecificallyScopedController() throws Exception {
		String accessTokenForUser1 = getAccessTokenForUser1("openid");
		try {
			// We use the bearer token to authorize the client 
			synapseClientLackingCredentials.setBearerAuthorizationToken(accessTokenForUser1);
			// Now the calls made by 'synapseClientLackingCredentials' are authenticated/authorized
			// as User1.

			// We can make this request, even though we don't have full OAuth scope (just 'openid')
			// since the service requires just openid
			synapseClientLackingCredentials.getUserInfoAsJSON();
		} finally {
			synapseClientLackingCredentials.removeAuthorizationHeader();
		}		
	}

}
