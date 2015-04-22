package org.sagebionetworks;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.dynamo.dao.nodetree.DboNodeLineage;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;

public class IT050SynapseJavaClientDynamo {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;

	public static final long MAX_WAIT_TIME_MS = 5 * 60 * 1000; // 5 min

	private static Entity parent;
	private static Entity child;

	@BeforeClass
	public static void beforeClass() throws Exception {
		StackConfiguration config = new StackConfiguration();
		// These tests are not run if dynamo is disabled.
		Assume.assumeTrue(config.getDynamoEnabled());
		
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUserName(StackConfiguration.getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfiguration.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);

		String tableName = DboNodeLineage.TABLE_NAME;
		String hashKeyName = DboNodeLineage.HASH_KEY_NAME;
		String rangeKeyName = DboNodeLineage.RANGE_KEY_NAME;
		adminSynapse.clearDynamoTable(tableName, hashKeyName, rangeKeyName);

		// Setup all of the objects for this test.
		parent = new Project();
		parent.setName(IT050SynapseJavaClientDynamo.class.getName() + "parent");
		parent = synapse.createEntity(parent);
		child = new Folder();
		child.setName(IT050SynapseJavaClientDynamo.class.getName() + "child");
		child.setParentId(parent.getId());
		child = synapse.createEntity(child);
	}

	@AfterClass
	public static void afterClass() throws Exception{
		StackConfiguration config = new StackConfiguration();
		// There is nothing to do if dynamo is disabled
		if(!config.getDynamoEnabled()) return;
		
		if (synapse != null) {
			if (child != null) {
				synapse.deleteAndPurgeEntity(child);
			}
			if (parent != null) {
				synapse.deleteAndPurgeEntity(parent);
			}
		}
		
		adminSynapse.deleteUser(userToDelete);
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
