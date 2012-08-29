package org.sagebionetworks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.SynapseAdministration;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Folder;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.tool.migration.Constants;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.MigrationQueryRunner;
import org.sagebionetworks.tool.migration.dao.QueryRunnerImpl;

/**
 * Integration test for the QueryRunnerImpl
 * @author jmhill
 *
 */
public class ITMigrationQueryRunner {
	
	private static SynapseAdministration synapse;
	private List<Entity> toDelete = null;
	
	@Before
	public void before() throws SynapseException{
		toDelete = new ArrayList<Entity>();
		synapse = new SynapseAdministration();
		synapse.setAuthEndpoint(StackConfiguration
				.getAuthenticationServicePrivateEndpoint());
		synapse.setRepositoryEndpoint(StackConfiguration
				.getRepositoryServiceEndpoint());
		// This test depends on being an admin user.
		synapse.login(StackConfiguration.getIntegrationTestUserAdminName(),
				StackConfiguration.getIntegrationTestUserAdminPassword());
	}
	
	@After
	public void after() throws Exception {
		if(synapse != null && toDelete != null){
			for(Entity e: toDelete){
				synapse.deleteEntity(e);
			}
		}
	}
	
	@Test
	public void testQueryForAllMigratableData() throws Exception {
		MigrationQueryRunner mqr = new MigrationQueryRunner(synapse, true);
		mqr.getAllEntityData(null);
	}
	
	@Test
	public void testGetTotalEntityCount() throws Exception {
		MigrationQueryRunner mqr = new MigrationQueryRunner(synapse, true);
		mqr.getTotalEntityCount();
	}
	
	@Test
	public void testQueryForRoot() throws SynapseException, JSONException{
		// Make sure we can get the root Entity
		QueryRunnerImpl queryRunner = new QueryRunnerImpl(synapse);
		EntityData root = queryRunner.getRootEntity();
		assertNotNull(root);
		System.out.println(root);
		assertNotNull(root.getEntityId());
		assertNotNull(root.geteTag());
		assertEquals(null, root.getParentId());
	}
	
	@Test
	public void testQueryForAllPages() throws Exception {
		// For this test create a parent and make sure we can get all children;
		Project parent = new Project();
		parent = synapse.createEntity(parent);
		toDelete.add(parent);
		int children = 11;
		QueryRunnerImpl queryRunner = new QueryRunnerImpl(synapse);
		List<EntityData> expectedList = new ArrayList<EntityData>();
		for(int i=0; i<children; i++){
			Folder child = new Folder();
			child.setParentId(parent.getId());
			child = synapse.createEntity(child);
			// PLFM-1122: synapse prefix will be stripped by QueryRunner, preporcess expected result
			EntityData e = new EntityData(child.getId(), child.getEtag(), child.getParentId());
			queryRunner.preProcessEntityData(e);
			expectedList.add(e);
		}
		// Now make sure we can find all of the children
		String query = QueryRunnerImpl.QUERY_CHILDREN_OF_ENTITY1 + "\"" + parent.getId() + "\"";
		List<EntityData> results = 	queryRunner.queryForAllPages(query, Constants.ENTITY, 1L, null);
		assertEquals(expectedList, results);
		// Try various page sizes.
		results = queryRunner.queryForAllPages(query, Constants.ENTITY, 2L, null);
		assertEquals(expectedList, results);
		results = queryRunner.queryForAllPages(query, Constants.ENTITY, 3L, null);
		assertEquals(expectedList, results);
		results = queryRunner.queryForAllPages(query, Constants.ENTITY, children, null);
		assertEquals(expectedList, results);
		// Also make sure we can run the real query
		results = queryRunner.getAllChildrenOfEntity(parent.getId());
		assertEquals(expectedList, results);
	}
	
	@Test
	public void testGetAllEntities() throws Exception {
		// First build up some hierarchy
		// Get the root
		List<EntityData> expectedOrder = new ArrayList<EntityData>();
		QueryRunnerImpl queryRunner = new QueryRunnerImpl(synapse);
		EntityData root = queryRunner.getRootEntity();
		assertNotNull(root);
		expectedOrder.add(root);
		// Now add a child
		Project parentProject = new Project();
		parentProject.setParentId(root.getEntityId());
		parentProject = synapse.createEntity(parentProject);
		// PLFM-1122
		EntityData e = new EntityData(parentProject.getId(), parentProject.getEtag(), parentProject.getParentId());
		queryRunner.preProcessEntityData(e);
		expectedOrder.add(e);
		// We want to delete this node
		toDelete.add(parentProject);
		// Now add some grand children
		Folder childFolder = new Folder();
		childFolder.setParentId(parentProject.getId());
		childFolder = synapse.createEntity(childFolder);
		e = new EntityData(childFolder.getId(), childFolder.getEtag(), childFolder.getParentId());
		queryRunner.preProcessEntityData(e);
		expectedOrder.add(e);
		// add one more level
		Folder grandChild = new Folder();
		grandChild.setParentId(childFolder.getId());
		grandChild = synapse.createEntity(grandChild);
		e = new EntityData(grandChild.getId(), grandChild.getEtag(), grandChild.getParentId());
		queryRunner.preProcessEntityData(e);
		expectedOrder.add(e);
		// Now query for all nodes should put them in order.
		List<EntityData> results = queryRunner.getAllEntityData(null);
		assertNotNull(results);
		// Check this against the total count
		long totalCount = queryRunner.getTotalEntityCount();
		System.out.println("Total entity count: "+totalCount);
		assertEquals(totalCount, results.size());
		// Now we might have other nodes but we must have the nodes in order
		// Find the index of each node
		int rootIndex = -1;
		int testRooIndex = -1;
		int childIndex = -1;
		int grandChildIndex = -1;
		for(int i=0; i<results.size(); i++){
			EntityData entity = results.get(i);
			if(entity.getEntityId().equals(root.getEntityId())){
				rootIndex = i;
				continue;
			}else if(entity.getEntityId().equals(parentProject.getId())){
				testRooIndex = i;
				continue;
			}else if(entity.getEntityId().equals(childFolder.getId())){
				childIndex = i;
				continue;
			}else if(entity.getEntityId().equals(grandChild.getId())){
				grandChildIndex = i;
				continue;
			}
		}
		// Root should always be first
		assertEquals(0, rootIndex);
		assertTrue(rootIndex < testRooIndex);
		assertTrue(testRooIndex < childIndex);
		assertTrue(childIndex < grandChildIndex);
		
	}

}
