package org.sagebionetworks;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.dynamo.dao.nodetree.DboNodeLineage;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;

public class IT050SynapseJavaClientDynamo {

	public static final long MAX_WAIT_TIME_MS = 5 * 60 * 1000; // 5 min

	private static SynapseAdministration synapseAdmin = null;
	private static Synapse synapse = null;

	private static Entity parent;
	private static Entity child;

	@BeforeClass
	public static void beforeClass() throws Exception {

		synapseAdmin = new SynapseAdministration();
		synapseAdmin.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapseAdmin.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapseAdmin.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());

		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());

		String tableName = DboNodeLineage.TABLE_NAME;
		String hashKeyName = DboNodeLineage.HASH_KEY_NAME;
		String rangeKeyName = DboNodeLineage.RANGE_KEY_NAME;
		synapseAdmin.clearDynamoTable(tableName, hashKeyName, rangeKeyName);

		// Setup all of the objects for this test.
		parent = new Project();
		parent.setName(IT050SynapseJavaClientDynamo.class.getName() + "parent");
		parent = synapse.createEntity(parent);
		child = new Study();
		child.setName(IT050SynapseJavaClientDynamo.class.getName() + "child");
		child.setParentId(parent.getId());
		child = synapse.createEntity(child);
	}

	@AfterClass
	public static void afterClass() throws SynapseException{
		if (synapse != null) {
			if (child != null) {
				synapse.deleteAndPurgeEntity(child);
			}
			if (parent != null) {
				synapse.deleteAndPurgeEntity(parent);
			}
		}
	}

	@Test
	public void test() throws Exception {
		EntityIdList descList = null;
		long start = System.currentTimeMillis();
		while ((descList == null
				|| descList.getIdList() == null
				|| descList.getIdList().size() == 0)
				&& (System.currentTimeMillis() - start < MAX_WAIT_TIME_MS)) {
			descList = synapse.getDescendants(parent.getId(), 10, null);
			Thread.sleep(2000);
		}
		System.out.println(System.currentTimeMillis() - start);
		Assert.assertNotNull(descList);
		Assert.assertNotNull(descList.getIdList());
		Assert.assertEquals(1, descList.getIdList().size());
		Assert.assertEquals(child.getId(), descList.getIdList().get(0).getId());
	}
}
