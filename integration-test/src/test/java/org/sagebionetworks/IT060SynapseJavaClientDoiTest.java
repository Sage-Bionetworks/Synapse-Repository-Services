package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.doi.DoiClient;
import org.sagebionetworks.doi.EzidClient;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiStatus;

public class IT060SynapseJavaClientDoiTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;

	/** Max wait time for the DOI status to turn green */
	private static final long MAX_WAIT = 30000; // 30 seconds
	private static final long PAUSE = 2000;     // Pause between waits is 2 seconds

	private static Entity entity;

	@BeforeClass 
	public static void beforeClass() throws Exception {

		StackConfiguration config = StackConfigurationSingleton.singleton();
		Assume.assumeTrue(config.getDoiEnabled());

		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(config.getMigrationAdminUsername());
		adminSynapse.setApiKey(config.getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);

		entity = new Project();
		entity.setName("IT060SynapseJavaClientDoiTest");
		entity = synapse.createEntity(entity);
		assertNotNull(entity);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		StackConfiguration config = StackConfigurationSingleton.singleton();
		if (config.getDoiEnabled()) {
			if (entity != null) {
				synapse.deleteAndPurgeEntityById(entity.getId());
			}
			adminSynapse.clearDoi();
			try {
				synapse.getEntityDoi(entity.getId(), null);
			} catch (SynapseNotFoundException e) {
				assertTrue(true);
			}
			adminSynapse.deleteUser(userToDelete);
		}
	}

	@Test
	public void testCreateGet() throws SynapseException {

		// Skip the test in case the EZID server is down
		DoiClient doiClient = new EzidClient();
		if (!doiClient.isStatusOk()) {
			return;
		}

		synapse.createEntityDoi(entity.getId());

		Doi doi = synapse.getEntityDoi(entity.getId());
		assertNotNull(doi);
		// Wait for the status to turn green
		try {
			DoiStatus status = doi.getDoiStatus();
			long time = 0;
			while (time < MAX_WAIT && DoiStatus.IN_PROCESS.equals(status)) {
				Thread.sleep(PAUSE);
				time = time + PAUSE;
				doi = synapse.getEntityDoi(entity.getId());
				status = doi.getDoiStatus();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		assertEquals(DoiStatus.CREATED, doi.getDoiStatus());
		assertTrue(Long.parseLong(doi.getId()) > 0L);
		assertFalse(NodeConstants.ZERO_E_TAG.equals(doi.getEtag()));
		assertEquals(entity.getId(), doi.getObjectId());
		assertNull(doi.getObjectVersion());
		assertEquals(ObjectType.ENTITY, doi.getObjectType());
		assertNotNull(doi.getCreatedBy());
		assertNotNull(doi.getCreatedOn());
		assertNotNull(doi.getUpdatedOn());
	}

	@Test
	public void testCreateGetWithVersion() throws SynapseException {

		// Skip the test in case the EZID server is down
		DoiClient doiClient = new EzidClient();
		if (!doiClient.isStatusOk()) {
			return;
		}

		synapse.createEntityDoi(entity.getId(), 1L);

		Doi doi = synapse.getEntityDoi(entity.getId(), 1L);
		assertNotNull(doi);
		// Wait for the status to turn green
		try {
			DoiStatus status = doi.getDoiStatus();
			long time = 0;
			while (time < MAX_WAIT && DoiStatus.IN_PROCESS.equals(status)) {
				Thread.sleep(PAUSE);
				time = time + PAUSE;
				doi = synapse.getEntityDoi(entity.getId(), 1L);
				status = doi.getDoiStatus();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		assertEquals(DoiStatus.CREATED, doi.getDoiStatus());
		assertTrue(Long.parseLong(doi.getId()) > 0L);
		assertFalse(NodeConstants.ZERO_E_TAG.equals(doi.getEtag()));
		assertEquals(entity.getId(), doi.getObjectId());
		assertEquals(Long.valueOf(1L), doi.getObjectVersion());
		assertEquals(ObjectType.ENTITY, doi.getObjectType());
		assertNotNull(doi.getCreatedBy());
		assertNotNull(doi.getCreatedOn());
		assertNotNull(doi.getUpdatedOn());
	}

	@Test(expected=SynapseNotFoundException.class)
	public void testGetNotFoundException() throws SynapseException {
		// Skip the test in case the EZID server is down
		DoiClient doiClient = new EzidClient();
		if (!doiClient.isStatusOk()) {
			return;
		}
		synapse.getEntityDoi("syn372861388593");
	}
}
