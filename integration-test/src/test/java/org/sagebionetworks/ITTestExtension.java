package org.sagebionetworks;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.LoginResponse;

public class ITTestExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {
	
	// Admin client for the admin user
	private static SynapseAdminClient adminSynapse;
	// Synpase client for a test user
	private static SynapseClient synapse;
	// The id of the test user
	private static Long userToDelete;
	// The stack configuration
	private static StackConfiguration config; 

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Class<?> paramType = parameterContext.getParameter().getType();
		return paramType == SynapseClient.class || paramType == SynapseAdminClient.class || paramType == StackConfiguration.class;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		Class<?> paramType = parameterContext.getParameter().getType();
		if (paramType == SynapseAdminClient.class) {
			return adminSynapse;
		} else if (paramType == SynapseClient.class) {
			// We don't always need a test user so we dynamically create it
			if (synapse == null) {
				synapse = new SynapseClientImpl();
				try {
					userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
				} catch (Exception e) {
					throw new ParameterResolutionException(e.getMessage(), e);
				}
			}
			return synapse;
		} else if (paramType == StackConfiguration.class) {
			return config;
		}
		return null;
	}
	
	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		// Always setup an admin client before each test
		config = StackConfigurationSingleton.singleton();
		
		adminSynapse = new SynapseAdminClientImpl();
		
		SynapseClientHelper.setEndpoints(adminSynapse);
		
		// Authenticate to the admin services using basic auth
		String adminServiceKey = config.getServiceAuthKey(StackConfiguration.SERVICE_ADMIN);
		String adminServiceSecret = config.getServiceAuthSecret(StackConfiguration.SERVICE_ADMIN);
		
		adminSynapse.setBasicAuthorizationCredentials(adminServiceKey, adminServiceSecret);
		adminSynapse.clearAllLocks();
		
		// Now obtains the admin user access token through the admin service
		LoginResponse response = adminSynapse.getUserAccessToken(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId());
		
		// Clear the auth header to use the bearer token instead with the access token
		adminSynapse.removeAuthorizationHeader();
		adminSynapse.setBearerAuthorizationToken(response.getAccessToken());
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		if (synapse != null) {
			try {
				adminSynapse.deleteUser(userToDelete);
			} catch (SynapseException e) {
				
			}
		}
	}

}
