package org.sagebionetworks;

import java.util.UUID;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Holds helpers for setting up integration tests
 */
public class SynapseClientHelper {
	public static void setEndpoints(SynapseClient client) {
		client.setAuthEndpoint(StackConfigurationSingleton.singleton().getAuthenticationServicePrivateEndpoint());
		client.setRepositoryEndpoint(StackConfigurationSingleton.singleton().getRepositoryServiceEndpoint());
		client.setFileEndpoint(StackConfigurationSingleton.singleton().getFileServiceEndpoint());
	}
	
	/**
	 * Creates a user that can login with a session token
	 * 
	 * @param newUserClient The client to log the new user in
	 * @return The ID of the user
	 */
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, UUID.randomUUID().toString());
	}

	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, String username) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, username, "password");
	}
	
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, String username, String password) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, username, password, UUID.randomUUID().toString() + "@sagebase.org");
	}

	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, String username, String password, String email) throws SynapseException, JSONObjectAdapterException {
		if (newUserClient == null) {
			newUserClient = new SynapseClientImpl();
		}
		setEndpoints(newUserClient);

		Session session = new Session();
		session.setAcceptsTermsOfUse(true);
		session.setSessionToken(UUID.randomUUID().toString());
		newUserClient.setSessionToken(session.getSessionToken());
		NewIntegrationTestUser nu = new NewIntegrationTestUser();
		nu.setSession(session);
		nu.setEmail(email);
		nu.setUsername(username);
		nu.setPassword(password);
		Long principalId = client.createUser(nu);
		client.setCertifiedUserStatus(principalId.toString(), true);
		return principalId;
	}
}
