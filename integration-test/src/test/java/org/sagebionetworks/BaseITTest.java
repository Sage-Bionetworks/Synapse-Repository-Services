package org.sagebionetworks;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.auth.LoginResponse;

public abstract class BaseITTest {
	
	// Admin client for the admin user
	protected static SynapseAdminClient adminSynapse;
	// Synpase client for a test user
	protected static SynapseClient synapse;
	// The id of the test user
	protected static Long userToDelete;
	// The stack configuration
	protected static StackConfiguration config; 

	@BeforeAll
	public static void beforeAll() throws Exception {
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
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}
	
	@AfterAll
	public static void afterAll() throws SynapseException {
		if (userToDelete != null) {
			try {
				adminSynapse.deleteUser(userToDelete);
			} catch (SynapseException e) { 
				
			}
		}
	}

}
