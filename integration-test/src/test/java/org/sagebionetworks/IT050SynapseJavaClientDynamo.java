package org.sagebionetworks;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;

public class IT050SynapseJavaClientDynamo {

	public static final long MAX_WAIT_TIME_MS = 15 * 60 * 1000; // 15 min

	private static Synapse synapse = null;

	private static Entity parent;
	private static Entity child;

	@BeforeClass
	public static void beforeClass() throws Exception {

		synapse = new Synapse();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		synapse.login(StackConfiguration.getIntegrationTestUserOneName(),
				StackConfiguration.getIntegrationTestUserOnePassword());

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
				synapse.deleteEntity(child);
			}
			if (parent != null) {
				synapse.deleteEntity(parent);
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
