package org.sagebionetworks;

import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.auth.JSONWebTokenHelper;
import org.sagebionetworks.repo.model.auth.LoginResponse;
import org.sagebionetworks.repo.model.auth.NewIntegrationTestUser;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

import java.util.UUID;

/**
 * Holds helpers for setting up integration tests
 */
public class SynapseClientHelper {
		
	public static void setEndpoints(SynapseClient client) {
		client.setAuthEndpoint(StackConfigurationSingleton.singleton().getAuthenticationServicePrivateEndpoint());
		client.setRepositoryEndpoint(StackConfigurationSingleton.singleton().getRepositoryServiceEndpoint());
		client.setFileEndpoint(StackConfigurationSingleton.singleton().getFileServiceEndpoint());
		client.setDrsEndpoint(StackConfigurationSingleton.singleton().getDrsServiceEndpoint());
	}
	
	/**
	 * Creates a user that can login with a session token
	 * 
	 * @param newUserClient The client to log the new user in
	 * @return The ID of the user
	 */
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, UUID.randomUUID().toString(), true, false);
	}

	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, boolean acceptsTermsOfUse, boolean validateUser) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, UUID.randomUUID().toString(), acceptsTermsOfUse, validateUser);
	}

	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, String username, boolean acceptsTermsOfUse, boolean validateUser) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, username, "password"+UUID.randomUUID().toString(), acceptsTermsOfUse, validateUser);
	}
	
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, String username, String password, boolean acceptsTermsOfUse, boolean validateUser) throws SynapseException, JSONObjectAdapterException {
		return createUser(client, newUserClient, username, password, UUID.randomUUID().toString() + "@sagebase.org", acceptsTermsOfUse, validateUser);
	}
	
	public static Long createUser(SynapseAdminClient client, SynapseClient newUserClient, 
			String username, String password, String email, boolean acceptsTermsOfUse, boolean validateUser) throws SynapseException, JSONObjectAdapterException {
		if (newUserClient == null) {
			newUserClient = new SynapseClientImpl();
		}
		setEndpoints(newUserClient);

		NewIntegrationTestUser nu = new NewIntegrationTestUser();
		nu.setTou(acceptsTermsOfUse);
		nu.setEmail(email);
		nu.setUsername(username);
		nu.setPassword(password);
		nu.setValidatedUser(validateUser);
		LoginResponse loginResponse = client.createIntegrationTestUser(nu);
		
		String accessTokenSubject = JSONWebTokenHelper.getSubjectFromJWTAccessToken(loginResponse.getAccessToken());
		Long principalId = Long.parseLong(accessTokenSubject);
		
		newUserClient.setBearerAuthorizationToken(loginResponse.getAccessToken());
		
		newUserClient.setAcceptsTermsOfUse(loginResponse.getAcceptsTermsOfUse());
		client.setCertifiedUserStatus(accessTokenSubject, true);
		return principalId;
	}
}
