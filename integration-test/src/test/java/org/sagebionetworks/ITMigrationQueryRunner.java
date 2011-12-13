package org.sagebionetworks;

import static org.junit.Assert.*;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.tool.migration.EntityData;
import org.sagebionetworks.tool.migration.QueryRunnerImpl;

/**
 * Integration test for the QueryRunnerImpl
 * @author jmhill
 *
 */
public class ITMigrationQueryRunner {
	
	private static Synapse synapse;
	
	@Before
	public void before() throws SynapseException{
		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		// This test depends on being an admin user.
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());
	}
	
	@Test
	public void testQueryForRoot() throws SynapseException, JSONException{
		// Make sure we can get the root Entity
		QueryRunnerImpl queryRunner = new QueryRunnerImpl();
		EntityData root = queryRunner.getRootEntity(synapse);
		assertNotNull(root);
		System.out.println(root);
		assertNotNull(root.getEntityId());
		assertNotNull(root.geteTag());
		assertEquals(null, root.getParentId());
	}

}
