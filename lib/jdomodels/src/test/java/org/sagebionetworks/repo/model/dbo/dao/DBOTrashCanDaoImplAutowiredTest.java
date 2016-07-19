package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
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
		Timestamp timestamp1 = new Timestamp(System.currentTimeMillis());
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
	public void testGetTrashLeavesBeforeNegativeNumDays(){//TODO: move to unit test?
		trashCanDao.getTrashLeavesBefore(-1, 123);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetTrashLeavesBeforeNegativeLimit(){//TODO: move to unit test?
		trashCanDao.getTrashLeavesBefore(123, -1);
	}
	
	@Test 
	public void testGetTrashLeavesBefore(){
		final int numNodes = 5;
		final String nodeNameBase = "DBOTrashCanDaoImplAutowiredTest.testGetTrashLeavesBefore() Node:";
		final long nodeID = 9000L;
		final long parentID = 10L;
		
		assertTrue(trashCanDao.getCount() == 0);
		
		//create trash nodes
		for(int i = 0; i < numNodes; i++){
			String stringNodeID = KeyFactory.keyToString(nodeID + i);
			String stringParentID = KeyFactory.keyToString(parentID + i);
			createTestNode(userId, stringNodeID, nodeNameBase + i, stringParentID, new Timestamp());//TODO: finish this
		}
		
		
		
		
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
