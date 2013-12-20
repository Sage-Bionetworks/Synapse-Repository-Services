package org.sagebionetworks;

import java.util.UUID;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.auth.Session;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Holds helpers for setting up integration tests
 */
public class SynapseClientHelper {
	public static void setEndpoints(SynapseClient client) {
		client.setAuthEndpoint(StackConfiguration.getAuthenticationServicePrivateEndpoint());
		client.setRepositoryEndpoint(StackConfiguration.getRepositoryServiceEndpoint());
		client.setFileEndpoint(StackConfiguration.getFileServiceEndpoint());
	}
	
	/**
	 * Creates a user that can login with a session token
	 * 
	 * @param newUserClient The client to log the new user in
	 * @return The ID of the user
	 */
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient) throws SynapseException, JSONObjectAdapterException {
		if (newUserClient == null) {
			newUserClient = new SynapseClientImpl();
		}
		setEndpoints(newUserClient);
		
		Session session = new Session();
		session.setAcceptsTermsOfUse(true);
		session.setSessionToken(UUID.randomUUID().toString());
		newUserClient.setSessionToken(session.getSessionToken());
		
		return client.createUser(UUID.randomUUID().toString() + "@sagebase.org", null, null, session);
	}
}
