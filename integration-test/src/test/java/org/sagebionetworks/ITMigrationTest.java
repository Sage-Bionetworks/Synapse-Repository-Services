package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;

public class ITMigrationTest {
	
	private static StackConfiguration stackConfig = StackConfigurationSingleton.singleton();
	private static SynapseAdminClient adminSynapse;
	
	@BeforeEach
	public void before() {
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
	}
	
	@Test
	public void testMigrationWithServiceAuth() throws SynapseException {

		assertThrows(SynapseForbiddenException.class, () -> {
			// No authentication
			adminSynapse.getMigrationTypes();
		});
		
		// Set service basic auth
		String key = stackConfig.getServiceAuthKey(StackConfiguration.SERVICE_MIGRATION);
		String secret = stackConfig.getServiceAuthSecret(StackConfiguration.SERVICE_MIGRATION);
		
		adminSynapse.setBasicAuthorizationCredentials(key, secret);
		
		// This should now work
		assertNotNull(adminSynapse.getMigrationTypes());
		
		// Clear basic auth
		adminSynapse.removeAuthorizationHeader();
	
		// Should still work with user/api key auth
		
		String migrationUser = stackConfig.getMigrationAdminUsername();
		String migrationKey = stackConfig.getMigrationAdminAPIKey();
		
		adminSynapse.setUsername(migrationUser);
		adminSynapse.setApiKey(migrationKey);
		
		// This should still work
		assertNotNull(adminSynapse.getMigrationTypes());
	}
	

}
