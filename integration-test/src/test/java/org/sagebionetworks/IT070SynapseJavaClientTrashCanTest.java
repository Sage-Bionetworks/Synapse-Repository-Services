package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.client.SynapseAdminClient;
import org.sagebionetworks.client.SynapseClient;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.client.exceptions.SynapseNotFoundException;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.repo.model.TrashedEntity;

@ExtendWith(ITTestExtension.class)
public class IT070SynapseJavaClientTrashCanTest {
	
	private Entity parent;
	private Entity child;
	
	private SynapseAdminClient adminSynapse;
	private SynapseClient synapse;
	
	public IT070SynapseJavaClientTrashCanTest(SynapseAdminClient adminSynapse, SynapseClient synapse) {
		this.adminSynapse = adminSynapse;
		this.synapse = synapse;
	}
	
	@BeforeEach
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

	@AfterEach
	public void after() throws SynapseException {
		
		// This is required so that the calls to viewTrash will not return the trashed entity
		boolean flagForPurge = true;
		
		try {
			synapse.deleteEntity(child, flagForPurge);
		}catch (SynapseException e){
			//do nothing if already deleted
		}
		try{
			synapse.deleteEntity(parent, flagForPurge);
		}catch(SynapseException e){
			//do nothing if already deleted
		}
	}

	@Test
	public void test() throws SynapseException {

		synapse.moveToTrash(parent.getId());
		
		assertThrows(SynapseNotFoundException.class, () -> {
			synapse.getEntityById(parent.getId());
		});
		
		assertThrows(SynapseNotFoundException.class, () -> {
			synapse.getEntityById(child.getId());
		});

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
	public void testFlagForPurge() throws SynapseException {
		synapse.moveToTrash(parent.getId());
		synapse.flagForPurge(parent.getId());
		
		assertThrows(SynapseNotFoundException.class, () -> {
			synapse.getEntityById(child.getId());
		});
		
		PaginatedResults<TrashedEntity> results = synapse.viewTrashForUser(0L, Long.MAX_VALUE);
		assertNotNull(results);
		assertEquals(0, results.getResults().size());
	}
}
