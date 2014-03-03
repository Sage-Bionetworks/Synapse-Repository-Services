package org.sagebionetworks;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.repo.model.DomainType;

public class IT620BridgeSessionAndTOU {
	
	private SynapseAdminClient adminClient;
	private SynapseClient synapseClient;
	private static List<Long> usersToDelete = new ArrayList<Long>();
	
	@Before
	public void before() throws Exception {
		adminClient = new SynapseAdminClientImpl();
		synapseClient = new SynapseClientImpl();
		SynapseClientHelper.setEndpoints(adminClient);
		SynapseClientHelper.setEndpoints(synapseClient);
		
		adminClient.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminClient.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		synapseClient.setUserName(StackConfiguration.getMigrationAdminUsername());
		synapseClient.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		
		long userId = SynapseClientHelper.createUser(adminClient, synapseClient, "test_user");
		usersToDelete.add(userId);
	}
	
	@After
	public void after() throws Exception {
		for (Long id : usersToDelete) {
			adminClient.deleteUser(id);
		}
	}
	
	@Test
	public void loginsAreSeparate() throws Exception {
		synapseClient.getSharedClientConnection().setDomain(DomainType.BRIDGE);
		synapseClient.login("test_user", "password");
		String token1 = synapseClient.getCurrentSessionToken();

		synapseClient.getSharedClientConnection().setDomain(DomainType.SYNAPSE);
		synapseClient.login("test_user", "password");
		String token2 = synapseClient.getCurrentSessionToken();
		
		Assert.assertFalse(token1.equals(token2));
	}

}
