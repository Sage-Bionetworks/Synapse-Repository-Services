package org.sagebionetworks;

import java.util.UUID;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
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
	 * @return The session token
	 */
	public static String createUser(SynapseAdminClient client) throws SynapseException, JSONObjectAdapterException {
		Session session = new Session();
		session.setAcceptsTermsOfUse(true);
		session.setSessionToken(UUID.randomUUID().toString());
		client.createUser(UUID.randomUUID().toString() + "@sagebase.org", null, null, null, session);
		return session.getSessionToken();
	}
}
