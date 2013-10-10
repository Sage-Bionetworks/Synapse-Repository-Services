package org.sagebionetworks;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.SynapseProfileProxy;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.ColumnType;

/**
 * Tests for TableEntity and ColumnModel services.
 * 
 * @author jmhill
 *
 */
public class IT100TableControllerTest {

	private static SynapseClient synapse = null;
	
	private static SynapseClient createSynapseClient(String user, String pw) throws SynapseException {
		SynapseClientImpl synapse = new SynapseClientImpl();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(user, pw);
		// Return a proxy
		return SynapseProfileProxy.createProfileProxy(synapse);
	}
	
	/**
	 * @throws Exception
	 * 
	 */
	@BeforeClass
	public static void beforeClass() throws Exception {

		synapse = createSynapseClient(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());
	}
	
	@Test
	public void testCreateGetColumn() throws SynapseException{
		ColumnModel cm = new ColumnModel();
		cm.setName("ExampleColumn");
		cm.setColumnType(ColumnType.STRING);
		cm = synapse.createColumnModel(cm);
		assertNotNull(cm);
		assertNotNull(cm.getId());
		ColumnModel clone = synapse.getColumnModel(cm.getId());
		assertNotNull(clone);
		assertEquals(cm, clone);
	}
	
}
