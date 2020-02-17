package org.sagebionetworks.repo.model.dbo.trash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOTrashCanDaoImplAutowiredTest {

	@Autowired
	private TrashCanDao trashCanDao;

	@Autowired
	private DBOBasicDao basicDao;
	
	private String userId;

	@BeforeEach
	public void before() throws Exception {
		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();
		trashCanDao.truncate();
	}

	@AfterEach
	public void after() throws Exception {
		trashCanDao.truncate();
	}
	
	@Test
	public void testCreateItemLongNameTooLong() {
		char[] chars = new char[260];
		Arrays.fill(chars,  'x');
		final String nodeName = new String(chars);
		final String nodeId = KeyFactory.keyToString(999L);
		final String parentId = KeyFactory.keyToString(9L);
		
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashCanDao.create(userId, nodeId, nodeName, parentId, false);
		});
	}

	@Test 
	public void testCreateItemLongName() {
		char[] chars = new char[255];
		Arrays.fill(chars,  'x');
		final String nodeName = new String(chars);
		final String nodeId = KeyFactory.keyToString(999L);
		final String parentId = KeyFactory.keyToString(9L);
		trashCanDao.create(userId, nodeId, nodeName, parentId, false);
	}

	@Test
	public void doesEntityHaveTrashedChildren() {
		final String parentId = KeyFactory.keyToString(1L);
		final String otherParentId = KeyFactory.keyToString(2L);
		final String node3IdStr = KeyFactory.keyToString(3L);
		final long node4Id = 4L;
		final String node4IdStr = KeyFactory.keyToString(node4Id);

		// Initially empty, since we don't have any trash can items.
		assertFalse(trashCanDao.doesEntityHaveTrashedChildren(parentId));

		// Add an item to the otherParentId. parentId is still empty.
		trashCanDao.create(userId, node3IdStr, node3IdStr, otherParentId, false);
		assertFalse(trashCanDao.doesEntityHaveTrashedChildren(parentId));

		// Add an item to parentId. Now it has children.
		trashCanDao.create(userId, node4IdStr, node4IdStr, parentId, false);
		assertTrue(trashCanDao.doesEntityHaveTrashedChildren(parentId));

		// Delete node 4 from the trash can. parentId is empty again.
		trashCanDao.delete(ImmutableList.of(node4Id));
		assertFalse(trashCanDao.doesEntityHaveTrashedChildren(parentId));
	}

	@Test 
	public void testGetTrashLeavesNodesWithNoChildrenOlderThanNow(){
		final int numNodes = 5;
		final String nodeNameBase = "DBOTrashCanDaoImplAutowiredTest.testGetTrashLeavesNoChildrenOlderThanNow() Node:";
		final long nodeID = 9000L;
		final long parentID = 10L;
		
		assertEquals(0, trashCanDao.getCount());
		
		//create trash leaves
		for(int i = 0; i < numNodes; i++){
			String stringNodeID = KeyFactory.keyToString(nodeID + i);
			String stringParentID = KeyFactory.keyToString(parentID + i);
			Timestamp time = timeDaysAgo( numNodes - i);
			createTestNode(userId, stringNodeID, nodeNameBase + stringNodeID, stringParentID, time);
		}
		assertEquals(trashCanDao.getCount(), numNodes);
		
		//test all older than now
		List<Long> trashOlderThanNow = trashCanDao.getTrashLeavesIds(0, 100);
		assertEquals(numNodes,trashOlderThanNow.size());
		for(int i = 0; i < numNodes; i++){
			assertTrue(trashOlderThanNow.contains(nodeID + i));
		}
		
	}
	
	@Test 
	public void testGetTrashLeavesNodesWithNoChildren(){
		final String nodeNameBase = "DBOTrashCanDaoImplAutowiredTest.testGetTrashLeavesNoChildren() Node:";
		final long nodeID = 9000L;
		final long parentID = 10L;
		
		assertEquals(0, trashCanDao.getCount());
		
		//create trash leaves Node 1
		String stringNode1ID = KeyFactory.keyToString(nodeID + 1);
		String stringParent1ID = KeyFactory.keyToString(parentID + 1);
		Timestamp time1 = timeDaysAgo(3);//3 days old
		createTestNode(userId, stringNode1ID, nodeNameBase + stringNode1ID, stringParent1ID, time1);
		
		//create trash leaves Node 1
		String stringNode2ID = KeyFactory.keyToString(nodeID + 2);
		String stringParent2ID = KeyFactory.keyToString(parentID + 2);
		Timestamp time2 = timeDaysAgo(1);//1 day old
		createTestNode(userId, stringNode2ID, nodeNameBase + stringNode2ID, stringParent2ID, time2);
		
		assertEquals(2,trashCanDao.getCount());
		
		
		int trashBefore = 2; //look for trash older than 2 days
		int limit = 100; //arbitrary number. doesn't matter here
		List<Long> trashOlderThanNumDays = trashCanDao.getTrashLeavesIds(trashBefore, limit);
		assertEquals(1,trashOlderThanNumDays.size()); 
		
		assertTrue(trashOlderThanNumDays.contains(nodeID + 1));//contains node 1
		assertFalse(trashOlderThanNumDays.contains(nodeID + 2));//does not contains node 2
		
		
	}
	
	@Test
	public void testGetTrashLeavesNodesWithChildren(){
		/*
		 Create node with 2 children that have children

		           N0
		          /  \
		         N1  N2
		         |    |\
		         N3  N4 N5
		*/
		final String nodeNameBase = "DBOTrashCanDaoImplAutowiredTest.testGetTrashLeavesNodesWithChildren() Node:";
		final long nodeIdBase = 9000L;
		//N0
		final String N0Id = KeyFactory.keyToString(nodeIdBase + 0);
		final String N0ParentId = KeyFactory.keyToString(12345L); //some random value for parent
		createTestNode(userId, N0Id, nodeNameBase + N0Id, N0ParentId, timeDaysAgo(1) );
		
		//N1
		final String N1Id = KeyFactory.keyToString(nodeIdBase + 1);
		final String N1ParentId = N0Id; //some random value for parent
		createTestNode(userId, N1Id, nodeNameBase + N1Id, N1ParentId, timeDaysAgo(1) );
		
		//N2
		final String N2Id = KeyFactory.keyToString(nodeIdBase + 2);
		final String N2ParentId = N0Id; //some random value for parent
		createTestNode(userId, N2Id, nodeNameBase + N2Id, N2ParentId, timeDaysAgo(1) );
		
		//N3
		final String N3Id = KeyFactory.keyToString(nodeIdBase + 3);
		final String N3ParentId = N1Id; //some random value for parent
		createTestNode(userId, N3Id, nodeNameBase + N3Id, N3ParentId, timeDaysAgo(1) );
		
		//N4
		final String N4Id = KeyFactory.keyToString(nodeIdBase + 4);
		final String N4ParentId = N2Id; //some random value for parent
		createTestNode(userId, N4Id, nodeNameBase + N4Id, N4ParentId, timeDaysAgo(1) );
		
		//N5
		final String N5Id = KeyFactory.keyToString(nodeIdBase + 5);
		final String N5ParentId = N2Id; //some random value for parent
		createTestNode(userId, N5Id, nodeNameBase + N5Id, N5ParentId, timeDaysAgo(1) );
		
		//check that N3, N4, N5 are the only ones in the list
		List<Long> trashLeaves = trashCanDao.getTrashLeavesIds(0, 6);
		assertEquals(3, trashLeaves.size());
		assertTrue( trashLeaves.contains( KeyFactory.stringToKey(N3Id) ) );
		assertTrue( trashLeaves.contains( KeyFactory.stringToKey(N4Id) ) );
		assertTrue( trashLeaves.contains( KeyFactory.stringToKey(N5Id) ) );
		
		// Deletes the leaves
		trashCanDao.delete(trashLeaves);
		
		// Invoking the service again should return the middle layer
		trashLeaves = trashCanDao.getTrashLeavesIds(0, 6);
		
		assertEquals(2, trashLeaves.size());
		assertTrue( trashLeaves.contains( KeyFactory.stringToKey(N1Id) ) );
		assertTrue( trashLeaves.contains( KeyFactory.stringToKey(N2Id) ) );
		
	}
	
	@Test
	public void TestDeleteListNullList(){
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			trashCanDao.delete(null);
		});
	}
	
	@Test
	public void TestDeleteListEmptyList(){
		assertEquals(0, trashCanDao.delete(new ArrayList<Long>()));
	}
	
	@Test
	public void testDeleteListNonExistantTrash(){
		List<Long> nonExistantTrashList = new ArrayList<Long>();
		assertEquals(0, trashCanDao.delete(nonExistantTrashList) );
	}
	
	@Test
	public void testDeleteList(){
		final int numNodes = 2;
		final String nodeNameBase = "DBOTrashCanDaoImplAutowiredTest.testDeleteList() Node:";
		final long nodeIDBase = 9000L;
		final long parentID = 10L;
		
		List<Long> nodesToDelete =  new ArrayList<Long>();
		assertEquals(0, trashCanDao.getCount());
		for(int i = 0; i < numNodes; i++){
			long nodeID = nodeIDBase + i;
			String stringNodeID = KeyFactory.keyToString(nodeID);
			String stringParentID = KeyFactory.keyToString(parentID + i);
			
			trashCanDao.create(userId, stringNodeID, nodeNameBase + i, stringParentID, false);
			
			//delete the even value nodes later
			if(nodeID % 2 == 0){
				nodesToDelete.add(nodeID);
			}
		}
		assertEquals(numNodes, trashCanDao.getCount());
		
		trashCanDao.delete(nodesToDelete);
		
		assertEquals(numNodes/2 , trashCanDao.getCount());
		//check that the even nodes are all deleted
		for(int i = 0; i < numNodes; i++){
			long nodeID = nodeIDBase + i;
			assertTrue( trashCanDao.getTrashedEntity(KeyFactory.keyToString(nodeID)) != null != (nodeID % 2 == 0)); //Only one of these conditions is true
		}
	}
	
	@Test
	public void testCreateWithPriorityPurge() {
		String nodeName = "Node Name " + UUID.randomUUID().toString();
		Long nodeId = 999L;
		String stringNodeId = KeyFactory.keyToString(nodeId);
		String parentId = KeyFactory.keyToString(9L);
		
		boolean priorityPurge = true;
		
		// Call under test
		trashCanDao.create(userId, stringNodeId, nodeName, parentId, priorityPurge);
		
		// Use an old value 
		int numDays = 30;
		int limit = Integer.MAX_VALUE;
		
		// Count should return the flagged node
		assertEquals(1, trashCanDao.getCount());
		
		// Below should not include the flagged nodes
		assertNull(trashCanDao.getTrashedEntity(stringNodeId));
		assertTrue(trashCanDao.listTrashedEntities(userId, 0, limit).isEmpty());
		
		// We should find it through in the deleted items
		List<Long> trash = trashCanDao.getTrashLeavesIds(numDays, limit);
		
		assertEquals(1, trash.size());
		assertEquals(nodeId, trash.get(0));
	}
	
	@Test
	public void testFlagWithPriorityPurge() {
		
		String nodeName = "Node Name " + UUID.randomUUID().toString();
		Long nodeId = 999L;
		String stringNodeId = KeyFactory.keyToString(nodeId);
		String parentId = KeyFactory.keyToString(9L);
		
		boolean priorityPurge = false;
		
		// Create the node without the flag
		trashCanDao.create(userId, stringNodeId, nodeName, parentId, priorityPurge);
		
		// Use an old value 
		int numDays = 30;
		int limit = Integer.MAX_VALUE;
		
		// Count should include the node
		assertEquals(1, trashCanDao.getCount());
		
		// Below should include the flagged nodes
		assertNotNull(trashCanDao.getTrashedEntity(stringNodeId));
		assertFalse(trashCanDao.listTrashedEntities(userId, 0, limit).isEmpty());
		
		// Should not be in the trashed leaves as it has been deleted after the 30 days trehsold
		List<Long> trash = trashCanDao.getTrashLeavesIds(numDays, limit);
		
		assertTrue(trash.isEmpty());
		
		// Call under test
		trashCanDao.flagForPurge(Collections.singletonList(nodeId));
		
		// Count should include the flagged node
		assertEquals(1, trashCanDao.getCount());
		
		// Below should not include the flagged nodes
		assertNull(trashCanDao.getTrashedEntity(stringNodeId));
		assertTrue(trashCanDao.listTrashedEntities(userId, 0, limit).isEmpty());
		
		// We should now find it through in the deleted items
		trash =  trashCanDao.getTrashLeavesIds(numDays, limit);
		
		assertEquals(1, trash.size());
		assertEquals(nodeId, trash.get(0));
		
	}
	
	//time in milliseconds of numDays ago
	private Timestamp timeDaysAgo(int numDays){
		return new Timestamp(System.currentTimeMillis() - numDays * 24 * 60 * 60 * 1000);
	}
	
	//Basically same as create() in TrashCanDao but can specify the timestamp.
	private void createTestNode(String userGroupId, String nodeId, String nodeName, String parentId, Timestamp ts){
		DBOTrashedEntity dbo = new DBOTrashedEntity();
		dbo.setNodeId(KeyFactory.stringToKey(nodeId));
		dbo.setNodeName(nodeName);
		dbo.setDeletedBy(KeyFactory.stringToKey(userGroupId));
		dbo.setDeletedOn(ts);
		dbo.setParentId(KeyFactory.stringToKey(parentId));
		dbo.setEtag(UUID.randomUUID().toString());
		dbo.setPriorityPurge(false);
		basicDao.createNew(dbo);
	}

}
