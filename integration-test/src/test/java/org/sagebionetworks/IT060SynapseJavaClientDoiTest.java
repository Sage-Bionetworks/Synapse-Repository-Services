package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.doi.DoiClient;
import org.sagebionetworks.doi.EzidClient;
import org.sagebionetworks.ids.UuidETagGenerator;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.UserSessionData;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.doi.DoiObjectType;
import org.sagebionetworks.repo.model.doi.DoiStatus;

public class IT060SynapseJavaClientDoiTest {

	/** Max wait time for the DOI status to turn green */
	private static long MAX_WAIT = 12000; // 12 seconds
	private static long PAUSE = 2000;     // Pause between waits is 2 seconds
	private static SynapseAdministration synapseAdmin;
	private Synapse synapse;
	private Entity entity;

	@Before
	public void before() throws SynapseException {

		synapse = new Synapse();
		synapse.setAuthEndpoint(
				StackConfiguration.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(
				StackConfiguration.getRepositoryServiceEndpoint());

		String user = StackConfiguration.getIntegrationTestUserOneName();
		String pw = StackConfiguration.getIntegrationTestUserOnePassword();

		UserSessionData session = synapse.login(user, pw);
		assertNotNull(session);
		assertNotNull(session.getProfile().getUserName());
		assertNotNull(session.getSessionToken());

		synapseAdmin = new SynapseAdministration();
		synapseAdmin.setAuthEndpoint(
				StackConfiguration.getAuthenticationServicePrivateEndpoint());
		synapseAdmin.setRepositoryEndpoint(
				StackConfiguration.getRepositoryServiceEndpoint());

		String adminUsr = StackConfiguration.getIntegrationTestUserAdminName();
		String adminPwd = StackConfiguration.getIntegrationTestUserAdminPassword();

		UserSessionData adminSession = synapseAdmin.login(adminUsr, adminPwd);
		assertNotNull(adminSession);
		assertNotNull(adminSession.getProfile().getUserName());
		assertNotNull(adminSession.getSessionToken());

		entity = new Project();
		entity.setName("IT060SynapseJavaClientDoiTest");
		entity = synapse.createEntity(entity);
		assertNotNull(entity);
	}

	@After
	public void after() throws SynapseException {
		if (entity != null) {
			synapse.deleteAndPurgeEntityById(entity.getId());
		}
		synapseAdmin.clearDoi();
		try {
			synapse.getEntityDoi(entity.getId(), null);
		} catch (SynapseNotFoundException e) {
			assertTrue(true);
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
		assertFalse(UuidETagGenerator.ZERO_E_TAG.equals(doi.getEtag()));
		assertEquals(entity.getId(), doi.getObjectId());
		assertNull(doi.getObjectVersion());
		assertEquals(DoiObjectType.ENTITY, doi.getDoiObjectType());
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
		assertFalse(UuidETagGenerator.ZERO_E_TAG.equals(doi.getEtag()));
		assertEquals(entity.getId(), doi.getObjectId());
		assertEquals(Long.valueOf(1L), doi.getObjectVersion());
		assertEquals(DoiObjectType.ENTITY, doi.getDoiObjectType());
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
