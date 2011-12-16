package org.sagebionetworks;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.Project;
import org.sagebionetworks.tool.migration.dao.EntityData;
import org.sagebionetworks.tool.migration.dao.QueryRunnerImpl;

/**
 * Integration test for the QueryRunnerImpl
 * @author jmhill
 *
 */
public class ITMigrationQueryRunner {
	
	private static Synapse synapse;
	private List<Entity> toDelete = null;
	
	@Before
	public void before() throws SynapseException{
		toDelete = new ArrayList<Entity>();
		synapse = new Synapse();
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
	public void testQueryForRoot() throws SynapseException, JSONException{
		// Make sure we can get the root Entity
		QueryRunnerImpl queryRunner = new QueryRunnerImpl();
		EntityData root = queryRunner.getRootEntity(synapse);
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
		List<EntityData> expectedList = new ArrayList<EntityData>();
		for(int i=0; i<children; i++){
			Project child = new Project();
			child.setParentId(parent.getId());
			child = synapse.createEntity(child);
			expectedList.add(new EntityData(child.getId(), child.getEtag(), child.getParentId()));
		}
		// Now make sure we can find all of the children
		QueryRunnerImpl queryRunner = new QueryRunnerImpl();
		String query = QueryRunnerImpl.QUERY_CHILDREN_OF_ENTITY1+parent.getId();
		List<EntityData> results = 	queryRunner.queryForAllPages(synapse, query, 1, null);
		assertEquals(expectedList, results);
		// Try various page sizes.
		results = queryRunner.queryForAllPages(synapse, query, 2, null);
		assertEquals(expectedList, results);
		results = queryRunner.queryForAllPages(synapse, query, 3, null);
		assertEquals(expectedList, results);
		results = queryRunner.queryForAllPages(synapse, query, children, null);
		assertEquals(expectedList, results);
		// Also make sure we can run the real query
		results = queryRunner.getAllAllChildrenOfEntity(synapse, parent.getId());
		assertEquals(expectedList, results);
	}
	
	@Test
	public void testGetAllEntites() throws Exception {
		// First build up some hierarchy
		// Get the root
		List<EntityData> expectedOrder = new ArrayList<EntityData>();
		QueryRunnerImpl queryRunner = new QueryRunnerImpl();
		EntityData root = queryRunner.getRootEntity(synapse);
		assertNotNull(root);
		expectedOrder.add(root);
		// Now add a child
		Project testRoot = new Project();
		testRoot.setParentId(root.getEntityId());
		testRoot = synapse.createEntity(testRoot);
		expectedOrder.add(new EntityData(testRoot.getId(), testRoot.getEtag(), testRoot.getParentId()));
		// We want to delete this node
		toDelete.add(testRoot);
		// Now add some grand children
		Project child = new Project();
		child.setParentId(testRoot.getId());
		child = synapse.createEntity(child);
		expectedOrder.add(new EntityData(child.getId(), child.getEtag(), child.getParentId()));
		// add one more level
		Project grandChild = new Project();
		grandChild.setParentId(child.getId());
		grandChild = synapse.createEntity(grandChild);
		expectedOrder.add(new EntityData(grandChild.getId(), grandChild.getEtag(), grandChild.getParentId()));
		// Now query for all nodes should put them in order.
		List<EntityData> results = queryRunner.getAllEntityData(synapse, null);
		assertNotNull(results);
		// Check this against the total count
		long totalCount = queryRunner.getTotalEntityCount(synapse);
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
			}else if(entity.getEntityId().equals(testRoot.getId())){
				testRooIndex = i;
				continue;
			}else if(entity.getEntityId().equals(child.getId())){
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
		
		// the parent of all entities should proceed each entity.
		Set<String> existingSet = new HashSet<String>();
		for(int i=0; i<results.size(); i++){
			EntityData entity = results.get(i);
			existingSet.add(entity.getEntityId());

			if(entity.getParentId() != null){
				// the parent ID of every entity should be on the set (except root).
				assertTrue(existingSet.contains(entity.getParentId()));
			}
		}
	}

}
