package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.dao.TrashCanDao;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOTrashedEntity;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOTrashCanDaoImplAutowiredTest {

	@Autowired
	private TrashCanDao trashCanDao;

	@Autowired
	private DBOBasicDao basicDao;
	
	private String userId;

	@Before
	public void before() throws Exception {

		userId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId().toString();

		clear();

		List<TrashedEntity> trashList = trashCanDao.getInRange(false, 0L, Long.MAX_VALUE);
		assertTrue(trashList.size() == 0);
	}

	@After
	public void after() throws Exception {
		clear();
		List<TrashedEntity> trashList = trashCanDao.getInRange(false, 0L, Long.MAX_VALUE);
		assertTrue(trashList.size() == 0);
	}

	@Test
	public void testRoundTrip() throws Exception {

		int count = trashCanDao.getCount();
		assertEquals(0, count);
		count = trashCanDao.getCount(userId);
		assertEquals(0, count);
		List<TrashedEntity> trashList = trashCanDao.getInRange(false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(0, trashList.size());
		trashList = trashCanDao.getInRangeForUser(userId, false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(0, trashList.size());

		final String nodeName = "DBOTrashCanDaoImplAutowiredTest.testRoundTrip()";
		final String nodeId1 = KeyFactory.keyToString(555L);
		final String parentId1 = KeyFactory.keyToString(5L);
		TrashedEntity trash = trashCanDao.getTrashedEntity(userId, nodeId1);
		assertNull(trash);

		// Move node 1 to trash can
		trashCanDao.create(userId, nodeId1, nodeName, parentId1);

		count = trashCanDao.getCount();
		assertEquals(1, count);
		count = trashCanDao.getCount(userId);
		assertEquals(1, count);

		trashList = trashCanDao.getInRange(false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(1, trashList.size());

		trashList = trashCanDao.getInRangeForUser(userId, false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(1, trashList.size());
		trash = trashList.get(0);
		assertEquals(nodeId1, trash.getEntityId());
		assertEquals(nodeName, trash.getEntityName());
		assertEquals(userId, trash.getDeletedByPrincipalId());
		assertEquals(parentId1, trash.getOriginalParentId());
		assertNotNull(trash.getDeletedOn());

		Thread.sleep(1000);
		Timestamp timestamp1 = new Timestamp(System.currentTimeMillis()/1000*1000);
		assertTrue(trash.getDeletedOn().before(timestamp1));
		trashList = trashCanDao.getTrashBefore(timestamp1);
		assertNotNull(trashList);
		assertEquals(1, trashList.size());
		trash = trashList.get(0);
		assertEquals(nodeId1, trash.getEntityId());
		assertEquals(nodeName, trash.getEntityName());
		assertEquals(userId, trash.getDeletedByPrincipalId());
		assertEquals(parentId1, trash.getOriginalParentId());
		assertNotNull(trash.getDeletedOn());

		trash = trashCanDao.getTrashedEntity(userId, nodeId1);
		assertEquals(nodeId1, trash.getEntityId());
		assertEquals(nodeName, trash.getEntityName());
		assertEquals(userId, trash.getDeletedByPrincipalId());
		assertEquals(parentId1, trash.getOriginalParentId());
		assertNotNull(trash.getDeletedOn());

		trash = trashCanDao.getTrashedEntity(nodeId1);
		assertEquals(nodeId1, trash.getEntityId());
		assertEquals(nodeName, trash.getEntityName());
		assertEquals(userId, trash.getDeletedByPrincipalId());
		assertEquals(parentId1, trash.getOriginalParentId());
		assertNotNull(trash.getDeletedOn());
		trash = trashCanDao.getTrashedEntity("syn3829195");
		assertNull(trash);

		count = trashCanDao.getCount(userId);
		assertEquals(1, count);
		count = trashCanDao.getCount(KeyFactory.keyToString(837948837783838309L)); //a random, non-existing user
		assertEquals(0, count);
		boolean exists = trashCanDao.exists(userId, nodeId1);
		assertTrue(exists);
		exists = trashCanDao.exists(KeyFactory.keyToString(2839238478539L), nodeId1);
		assertFalse(exists);
		exists = trashCanDao.exists(userId, KeyFactory.keyToString(118493838393848L));
		assertFalse(exists);

		// Move node 2 to trash can
		final String nodeName2 = "DBOTrashCanDaoImplAutowiredTest.testRoundTrip() 2";
		final String nodeId2 = KeyFactory.keyToString(666L);
		final String parentId2 = KeyFactory.keyToString(6L);
		trashCanDao.create(userId, nodeId2, nodeName2, parentId2);

		trashList = trashCanDao.getInRangeForUser(userId, false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(2, trashList.size());
		trashList = trashCanDao.getInRange(false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(2, trashList.size());
		count = trashCanDao.getCount(userId);
		assertEquals(2, count);
		count = trashCanDao.getCount();
		assertEquals(2, count);
		exists = trashCanDao.exists(userId, nodeId2);
		assertTrue(exists);

		trashList = trashCanDao.getTrashBefore(timestamp1);
		assertNotNull(trashList);
		assertEquals(1, trashList.size());
		assertEquals(nodeId1, trashList.get(0).getEntityId());
		Thread.sleep(1000);
		Timestamp timestamp2 = new Timestamp(System.currentTimeMillis());
		trashList = trashCanDao.getTrashBefore(timestamp2);
		assertNotNull(trashList);
		assertEquals(2, trashList.size());

		trashCanDao.delete(userId, nodeId1);
		trashList = trashCanDao.getInRange(false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(1, trashList.size());
		trashList = trashCanDao.getInRangeForUser(userId, false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(1, trashList.size());
		trash = trashCanDao.getTrashedEntity(userId, nodeId1);
		assertNull(trash);
		trash = trashList.get(0);
		assertEquals(nodeId2, trash.getEntityId());
		assertEquals(nodeName2, trash.getEntityName());
		assertEquals(userId, trash.getDeletedByPrincipalId());
		assertEquals(parentId2, trash.getOriginalParentId());
		assertNotNull(trash.getDeletedOn());
		trash = trashCanDao.getTrashedEntity(userId, nodeId2);
		assertEquals(nodeId2, trash.getEntityId());
		assertEquals(nodeName2, trash.getEntityName());
		assertEquals(userId, trash.getDeletedByPrincipalId());
		assertEquals(parentId2, trash.getOriginalParentId());
		assertNotNull(trash.getDeletedOn());
		count = trashCanDao.getCount();
		assertEquals(1, count);
		count = trashCanDao.getCount(userId);
		assertEquals(1, count);
		exists = trashCanDao.exists(userId, nodeId2);
		assertTrue(exists);
		exists = trashCanDao.exists(userId, nodeId1);
		assertFalse(exists);

		trashCanDao.delete(userId, nodeId2);
		trashList = trashCanDao.getInRange(false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(0, trashList.size());
		trashList = trashCanDao.getInRangeForUser(userId, false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(0, trashList.size());
		count = trashCanDao.getCount();
		assertEquals(0, count);
		count = trashCanDao.getCount(userId);
		assertEquals(0, count);
		exists = trashCanDao.exists(userId, nodeId1);
		assertFalse(exists);
		exists = trashCanDao.exists(userId, nodeId2);
		assertFalse(exists);
		trash = trashCanDao.getTrashedEntity(userId, nodeId2);
		assertNull(trash);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateItemLongNameTooLong() {
		char[] chars = new char[260];
		Arrays.fill(chars,  'x');
		final String nodeName = new String(chars);
		final String nodeId = KeyFactory.keyToString(999L);
		final String parentId = KeyFactory.keyToString(9L);
		trashCanDao.create(userId, nodeId, nodeName, parentId);
	}

	@Test 
	public void testCreateItemLongName() {
		char[] chars = new char[255];
		Arrays.fill(chars,  'x');
		final String nodeName = new String(chars);
		final String nodeId = KeyFactory.keyToString(999L);
		final String parentId = KeyFactory.keyToString(9L);
		trashCanDao.create(userId, nodeId, nodeName, parentId);
		List<TrashedEntity> trashList = trashCanDao.getInRange(false, 0L, 100L);
		assertNotNull(trashList);
		assertEquals(1, trashList.size());
		assertEquals(nodeName, trashList.get(0).getEntityName());

	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetTrashLeavesNegativeNumDays(){//TODO: move to unit test?
		trashCanDao.getTrashLeaves(-1, 123);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetTrashLeavesNegativeLimit(){//TODO: move to unit test?
		trashCanDao.getTrashLeaves(123, -1);
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
		List<Long> trashOlderThanNow = trashCanDao.getTrashLeaves(0, 100);
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
		List<Long> trashOlderThanNumDays = trashCanDao.getTrashLeaves(trashBefore, limit);
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
		List<Long> trashLeaves = trashCanDao.getTrashLeaves(0, 6);
		assertEquals(3, trashLeaves.size());
		assertTrue( trashLeaves.contains( KeyFactory.stringToKey(N3Id) ) );
		assertTrue( trashLeaves.contains( KeyFactory.stringToKey(N4Id) ) );
		assertTrue( trashLeaves.contains( KeyFactory.stringToKey(N5Id) ) );
		
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void TestDeleteListNullList(){
		trashCanDao.delete(null);
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
			trashCanDao.create(userId, stringNodeID, nodeNameBase + i, stringParentID);
			
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
			assertTrue( trashCanDao.exists(userId, KeyFactory.keyToString(nodeID)) != (nodeID % 2 == 0) ); //Only one of these conditions is true
		}
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
		basicDao.createNew(dbo);
	}

	private void clear() throws Exception {
		List<TrashedEntity> trashList = trashCanDao.getInRangeForUser(userId, false, 0L, Long.MAX_VALUE);
		for (TrashedEntity trash : trashList) {
			trashCanDao.delete(userId, trash.getEntityId());
		}
	}
}
