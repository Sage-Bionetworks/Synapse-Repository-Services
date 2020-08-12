package org.sagebionetworks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.repo.model.feature.Feature;

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
		
		assertThrows(SynapseForbiddenException.class, () -> {			
			adminSynapse.getFeatureStatus(Feature.DATA_ACCESS_AUTO_REVOCATION);
		});
		
		// Set service basic auth
		String key = stackConfig.getServiceAuthKey(StackConfiguration.SERVICE_ADMIN);
		String secret = stackConfig.getServiceAuthSecret(StackConfiguration.SERVICE_ADMIN);
		
		adminSynapse.setBasicAuthorizationCredentials(key, secret);
		
		// This should now work
		assertNotNull(adminSynapse.getMigrationTypes());
		
		// Additionally admin services should work as well
		assertNotNull(adminSynapse.getFeatureStatus(Feature.DATA_ACCESS_AUTO_REVOCATION));
		
		// Clear basic auth
		adminSynapse.removeAuthorizationHeader();
	
		// Should still work with user/api key auth
		
		String migrationUser = stackConfig.getMigrationAdminUsername();
		String migrationKey = stackConfig.getMigrationAdminAPIKey();
		
		adminSynapse.setUsername(migrationUser);
		adminSynapse.setApiKey(migrationKey);
		
		// Both the migration and admin services should still work with the API key
		assertNotNull(adminSynapse.getMigrationTypes());
		assertNotNull(adminSynapse.getFeatureStatus(Feature.DATA_ACCESS_AUTO_REVOCATION));
	}
	

}
