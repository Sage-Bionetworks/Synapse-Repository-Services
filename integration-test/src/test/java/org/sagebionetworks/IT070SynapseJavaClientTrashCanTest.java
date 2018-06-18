package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseAdminClientImpl;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.SynapseClientImpl;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.TrashedEntity;

public class IT070SynapseJavaClientTrashCanTest {

	private static SynapseAdminClient adminSynapse;
	private static SynapseClient synapse;
	private static Long userToDelete;
	
	private Entity parent;
	private Entity child;
	
	@BeforeClass 
	public static void beforeClass() throws Exception {
		// Create a user
		adminSynapse = new SynapseAdminClientImpl();
		SynapseClientHelper.setEndpoints(adminSynapse);
		adminSynapse.setUsername(StackConfigurationSingleton.singleton().getMigrationAdminUsername());
		adminSynapse.setApiKey(StackConfigurationSingleton.singleton().getMigrationAdminAPIKey());
		adminSynapse.clearAllLocks();
		synapse = new SynapseClientImpl();
		userToDelete = SynapseClientHelper.createUser(adminSynapse, synapse);
	}

	@Before
	public void before() throws SynapseException {
		adminSynapse.clearAllLocks();
		parent = new Project();
		parent.setName("IT070SynapseJavaClientTrashCanTest.parent");
		parent = synapse.createEntity(parent);
		assertNotNull(parent);

		child = new Folder();
		child.setName("IT070SynapseJavaClientTrashCanTest.child");
		child.setParentId(parent.getId());
		child = synapse.createEntity(child);
		assertNotNull(child);
	}

	@After
	public void after() throws SynapseException {
		try {
			synapse.deleteAndPurgeEntityById(child.getId());
		}catch (SynapseException e){
			//do nothing if already deleted
		}
		try{
			synapse.deleteAndPurgeEntityById(parent.getId());
		}catch(SynapseException e){
			//do nothing if already deleted
		}
	}
	
	@AfterClass
	public static void afterClass() throws Exception {
		adminSynapse.deleteUser(userToDelete);
	}

	@Test
	public void test() throws SynapseException {

		synapse.moveToTrash(parent.getId());
		try {
			synapse.getEntityById(parent.getId());
			fail();
		} catch (SynapseNotFoundException e) {
			assertTrue(true);
		} catch (Exception e) {
			fail();
		}
		try {
			synapse.getEntityById(child.getId());
			fail();
		} catch (SynapseNotFoundException e) {
			assertTrue(true);
		} catch (Exception e) {
			fail();
		}

		PaginatedResults<TrashedEntity> results = synapse.viewTrashForUser(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(1, results.getResults().size());

		synapse.restoreFromTrash(parent.getId(), null);
		Entity entity = synapse.getEntityById(parent.getId());
		assertNotNull(entity);
		entity = synapse.getEntityById(child.getId());
		assertNotNull(entity);

		results = synapse.viewTrashForUser(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(0, results.getResults().size());
	}

	@Test
	public void testPurge() throws SynapseException {
		synapse.moveToTrash(parent.getId());
		synapse.purgeTrashForUser(parent.getId());
		try {
			synapse.getEntityById(child.getId());
			fail();
		} catch (SynapseNotFoundException e) {
			assertTrue(true);
		} catch (Throwable e) {
			fail();
		}
		PaginatedResults<TrashedEntity> results = synapse.viewTrashForUser(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(0, results.getResults().size());
	}

	@Test
	public void testPurgeAll() throws SynapseException {
		synapse.moveToTrash(parent.getId());
		synapse.purgeTrashForUser();
		try {
			synapse.getEntityById(child.getId());
			fail();
		} catch (SynapseNotFoundException e) {
			assertTrue(true);
		} catch (Throwable e) {
			fail();
		}
		try {
			synapse.getEntityById(parent.getId());
			fail();
		} catch (SynapseNotFoundException e) {
			assertTrue(true);
		} catch (Throwable e) {
			fail();
		}
		PaginatedResults<TrashedEntity> results = synapse.viewTrashForUser(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(0, results.getResults().size());
		// Already purged, no need to clean
	}

	@Test
	public void testAdmin() throws SynapseException {
		adminSynapse.purgeTrash();
		synapse.moveToTrash(parent.getId());
		PaginatedResults<TrashedEntity> results = adminSynapse.viewTrash(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(1, results.getResults().size());
		assertEquals(1, results.getTotalNumberOfResults());
		adminSynapse.purgeTrash();
		try {
			synapse.getEntityById(child.getId());
			fail();
		} catch (SynapseNotFoundException e) {
			//expected
		} catch (Throwable e) {
			fail();
		}
		try {
			synapse.getEntityById(parent.getId());
			fail();
		} catch (SynapseNotFoundException e) {
			//expected
		} catch (Throwable e) {
			fail();
		}
		results = adminSynapse.viewTrashForUser(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(0, results.getResults().size());
		// Already purged, no need to clean
	}
	
	@Test
	public void testAdminPurgeLeaves() throws SynapseException{
		final long limit = Long.MAX_VALUE;
		final long offset = 0;
		final long daysInTrash = 0;
		
		//reset trash can
		adminSynapse.purgeTrash();
		
		//move the 2 nodes to the trash
		synapse.moveToTrash(parent.getId());
		PaginatedResults<TrashedEntity> results = adminSynapse.viewTrash(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(1, results.getResults().size());
		assertEquals(1, results.getTotalNumberOfResults());
		
		//purge the trash leaves (child node)
		adminSynapse.purgeTrashLeaves(daysInTrash, limit);
		
		//check parent still in trash
		results = adminSynapse.viewTrash(offset, limit);
		assertNotNull(results);
		assertEquals(0, results.getResults().size());//only 1 item in trash
	}
}
