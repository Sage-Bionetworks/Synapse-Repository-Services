package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseForbiddenException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.client.exceptions.SynapseUnauthorizedException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.Study;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserSessionData;

public class IT070SynapseJavaClientTrashCanTest {

	private Synapse synapse;
	private Entity parent;
	private Entity child;

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

		parent = new Project();
		parent.setName("IT530SynapseJavaClientTrashCanTest.parent");
		parent = synapse.createEntity(parent);
		assertNotNull(parent);

		child = new Study();
		child.setName("IT530SynapseJavaClientTrashCanTest.child");
		child.setParentId(parent.getId());
		child = synapse.createEntity(child);
		assertNotNull(child);
	}

	@After
	public void after() throws SynapseException {
		if (child != null) {
			synapse.deleteEntityById(child.getId());
		}
		if (parent != null) {
			synapse.deleteEntityById(parent.getId());
		}
	}

	@Test
	public void test() throws SynapseException {

		Entity entity = synapse.getEntityById(parent.getId());
		assertNotNull(entity);
		entity = synapse.getEntityById(child.getId());
		assertNotNull(entity);

		synapse.moveToTrash(parent.getId());
		try {
			synapse.getEntityById(parent.getId());
		} catch (SynapseUnauthorizedException e) {
			assertTrue(true);
		} catch (SynapseForbiddenException e) {
			assertTrue(true);
		} catch (Exception e) {
			fail();
		}
		try {
			synapse.getEntityById(child.getId());
		} catch (SynapseUnauthorizedException e) {
			assertTrue(true);
		} catch (SynapseForbiddenException e) {
			assertTrue(true);
		} catch (Exception e) {
			fail();
		}

		PaginatedResults<TrashedEntity> results = synapse.viewTrash(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(2, results.getResults().size());

		synapse.restoreFromTrash(parent.getId(), null);
		entity = synapse.getEntityById(parent.getId());
		assertNotNull(entity);
		entity = synapse.getEntityById(child.getId());
		assertNotNull(entity);

		results = synapse.viewTrash(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(0, results.getResults().size());
	}

	@Test
	public void testPurge() throws SynapseException {
		synapse.moveToTrash(parent.getId());
		synapse.purge(child.getId());
		try {
			synapse.getEntityById(child.getId());
		} catch (SynapseNotFoundException e) {
			assertTrue(true);
		} catch (Throwable e) {
			fail();
		}
		PaginatedResults<TrashedEntity> results = synapse.viewTrash(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(1, results.getResults().size());
		assertEquals(parent.getId(), results.getResults().get(0).getEntityId());
		synapse.purge(parent.getId());
		try {
			synapse.getEntityById(parent.getId());
		} catch (SynapseNotFoundException e) {
			assertTrue(true);
		} catch (Throwable e) {
			fail();
		}
		results = synapse.viewTrash(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(0, results.getResults().size());
		// Already purged, no need to clean
		child = null;
		parent = null;
	}

	@Test
	public void testPurgeAll() throws SynapseException {
		synapse.moveToTrash(parent.getId());
		synapse.purge();
		try {
			synapse.getEntityById(child.getId());
		} catch (SynapseNotFoundException e) {
			assertTrue(true);
		} catch (Throwable e) {
			fail();
		}
		try {
			synapse.getEntityById(parent.getId());
		} catch (SynapseNotFoundException e) {
			assertTrue(true);
		} catch (Throwable e) {
			fail();
		}
		PaginatedResults<TrashedEntity> results = synapse.viewTrash(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(0, results.getResults().size());
		// Already purged, no need to clean
		child = null;
		parent = null;
	}
}
