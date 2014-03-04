package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.jdo.JDONodeLockChecker;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.message.TransactionalMessengerObserver;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Tests that messages are fired as expected.
 * @author John
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class NodeDaoMessageTest {
	
	@Autowired
	private NodeDAO nodeDao;
	
	@Autowired
	private JDONodeLockChecker nodeLockerA;
	
	@Autowired
	private TransactionalMessenger transactionalMessanger;
	
	@Autowired
	private DBOChangeDAO changeDAO;
	
	private TransactionalMessengerObserver mockObserver;
	
	// delete anything at the end
	private List<String> toDelete = new ArrayList<String>();
	private Long creatorUserGroupId;	
	
	@Before
	public void before() throws NumberFormatException, DatastoreException{
		creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		mockObserver = Mockito.mock(TransactionalMessengerObserver.class);
		// Add a mock observer
		transactionalMessanger.registerObserver(mockObserver);
		toDelete = new ArrayList<String>();
	}
	
	@After
	public void after() throws DatastoreException{
		// remove the observer
		transactionalMessanger.removeObserver(mockObserver);
		if(toDelete != null && nodeDao != null){
			for(String id:  toDelete){
				// Delete each
				try{
					nodeDao.delete(id);
				}catch (NotFoundException e) {
					// happens if the object no longer exists.
				}
			}
		}
	}
	
	@Test
	public void testCreateNode() throws NotFoundException, DatastoreException, InvalidModelException{
		// When we create a node a create message should get fired
		Node node = NodeTestUtils.createNew("createTest", creatorUserGroupId);
		// Get the current change number
		long startChangeNumber = changeDAO.getCurrentChangeNumber();
		// Now create it
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		node = nodeDao.getNode(id);
		
		ChangeMessage expectedMessage = new ChangeMessage();
		expectedMessage.setChangeType(ChangeType.CREATE);
		expectedMessage.setObjectType(ObjectType.ENTITY);
		expectedMessage.setObjectEtag(node.getETag());
		expectedMessage.setObjectId(id);
		expectedMessage.setChangeNumber(startChangeNumber+1);
		
		// Make sure our change was recorded in the database
		List<ChangeMessage> converted = changeDAO.listChanges(startChangeNumber+1, ObjectType.ENTITY, 1);
		assertNotNull(converted);
		assertEquals(1, converted.size());
		ChangeMessage result = converted.get(0);
		assertNotNull(result.getChangeNumber());
		assertNotNull(result.getTimestamp());
		assertEquals(expectedMessage.getObjectEtag(), result.getObjectEtag());
		assertEquals(expectedMessage.getObjectId(), result.getObjectId());
		assertEquals(expectedMessage.getObjectType(), result.getObjectType());
		
		// copy over the timestamp for the expected test.
		expectedMessage.setTimestamp(result.getTimestamp());
		
		// The message should have been fired once.
		verify(mockObserver, times(1)).fireChangeMessage(expectedMessage);
	}
	
	@Test
	public void testUpdate() throws NotFoundException, DatastoreException, InvalidModelException, ConflictingUpdateException, NumberFormatException, InterruptedException{
		Node node = NodeTestUtils.createNew("updateTest", creatorUserGroupId);
		
		// Get the current change number
		long startChangeNumber = changeDAO.getCurrentChangeNumber();
		// Now create it
		final String id = nodeDao.createNew(node);
		toDelete.add(id);
		node = nodeDao.getNode(id);
		final String eTag = node.getETag();
		// Create a new observer for the update
		TransactionalMessengerObserver updateObserver = Mockito.mock(TransactionalMessengerObserver.class);
		transactionalMessanger.registerObserver(updateObserver);
		
		// The act of locking and updating an etag is what triggers an update message
		// however, the message should not fire until, we release the transaction.
		Thread threadOne = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					nodeLockerA.aquireAndHoldLock(id, eTag);
				} catch (Exception e) {
					fail(e.getMessage());
				}
			}
		});
		// Start the thread
		try{
			threadOne.start();
			// Give the thread plenty of time to start
			Thread.sleep(1000);
			assertTrue("Failed to acquire the lock on A",nodeLockerA.isLockAcquired());
			
			// At this point the message should not have fired.
			verify(updateObserver, never()).fireChangeMessage(any(ChangeMessage.class));
			
			// Now release the lock which will commit the transaction and fire the message.
			nodeLockerA.releaseLock();
			// Wait for the transaction to commit
			Thread.sleep(1000);
			
			String newEtag = nodeLockerA.getEtag();
			assertFalse("The etag should have changed.",eTag.equals(newEtag));
			
			List<ChangeMessage> converted = changeDAO.listChanges(startChangeNumber+1, ObjectType.ENTITY, 1);
			assertNotNull(converted);
			assertEquals(1, converted.size());
			ChangeMessage result = converted.get(0);
			
			ChangeMessage expectedMessage = new ChangeMessage();
			expectedMessage.setChangeType(ChangeType.UPDATE);
			expectedMessage.setObjectType(ObjectType.ENTITY);
			expectedMessage.setObjectEtag(newEtag);
			expectedMessage.setObjectId(id);
			expectedMessage.setChangeNumber(result.getChangeNumber());
			expectedMessage.setTimestamp(result.getTimestamp());
			// The message should have been fired once.
			verify(updateObserver, times(1)).fireChangeMessage(expectedMessage);
		}finally{
			// Release the lock even if we fail.
			nodeLockerA.releaseLock();
		}
	}
	
	/**
	 * If a transaction is rolled-back a message should not fire.
	 */
	@Test
	public void testUpdateRollback() throws NotFoundException, DatastoreException, InvalidModelException, ConflictingUpdateException, NumberFormatException, InterruptedException{
		Node node = NodeTestUtils.createNew("updateTest", creatorUserGroupId);
		// Now create it
		final String id = nodeDao.createNew(node);
		toDelete.add(id);
		node = nodeDao.getNode(id);
		final String eTag = node.getETag();
		// Create a new observer for the update
		TransactionalMessengerObserver updateObserver = Mockito.mock(TransactionalMessengerObserver.class);
		transactionalMessanger.registerObserver(updateObserver);
		
		// The act of locking and updating an etag is what triggers an update message
		// however, the message should not fire until, we release the transaction.
		Thread threadOne = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					nodeLockerA.aquireAndHoldLock(id, eTag);
				} catch (Exception e) {
					fail(e.getMessage());
				}
			}
		});
		// Start the thread
		try{
			threadOne.start();
			// Give the thread plenty of time to start
			Thread.sleep(1000);
			assertTrue("Failed to acquire the lock on A",nodeLockerA.isLockAcquired());
			
			// At this point the message should not have fired.
			verify(updateObserver, never()).fireChangeMessage(any(ChangeMessage.class));
			
			// Now throw an exception.
			nodeLockerA.throwException(new DatastoreException("Triggered exception"));
			
			// make sure a rollback actually occurred and the etag has not changed.
			Node current = nodeDao.getNode(id);
			assertEquals("Rolling back the update transaction should have restored the previous etag", eTag, current.getETag());
			// Wait for the transaction to rollback
			Thread.sleep(1000);
			
			// At this point the message should not have fired.
			verify(updateObserver, never()).fireChangeMessage(any(ChangeMessage.class));
			
		}finally{
			// Release the lock even if we fail.
			nodeLockerA.releaseLock();
		}
	}
	
	@Test
	public void testDelete() throws DatastoreException, InvalidModelException, NotFoundException{
		// Make sure a delete message is sent.
		
		// When we create a node a create message should get fired
		Node node = NodeTestUtils.createNew("createTest", creatorUserGroupId);
		// Now create it
		String id = nodeDao.createNew(node);
		toDelete.add(id);
		nodeDao.delete(id);
		
		// Get the current change number
		long chagneNumber = changeDAO.getCurrentChangeNumber();
		List<ChangeMessage> converted = changeDAO.listChanges(chagneNumber, ObjectType.ENTITY, 1);
		assertNotNull(converted);
		assertEquals(1, converted.size());
		ChangeMessage result = converted.get(0);
		
		ChangeMessage expectedMessage = new ChangeMessage();
		expectedMessage.setChangeType(ChangeType.DELETE);
		expectedMessage.setObjectType(ObjectType.ENTITY);
		expectedMessage.setObjectId(id);
		expectedMessage.setTimestamp(result.getTimestamp());
		expectedMessage.setChangeNumber(chagneNumber);
		// The message should have been fired once.
		verify(mockObserver, times(1)).fireChangeMessage(expectedMessage);
	}
	

}
