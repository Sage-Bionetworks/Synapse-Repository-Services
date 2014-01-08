package org.sagebionetworks.repo.model.jdo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test that we can acquire a lock on a node.
 * 
 * @author jmhill
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class NodeLockTest {
	
	private static Logger log = Logger.getLogger(NodeLockTest.class.getName());
	/**
	 * The max amount of time to give any thread in this test.
	 */
	private static long timeout = 2000;
	
	@Autowired
	private JDONodeLockChecker nodeLockerA;
	
	@Autowired
	private JDONodeLockChecker nodeLockerB;
	
	@Autowired
	private NodeDAO nodeDao;
	
	private String nodeId;
	
	
	@Before
	public void before() throws Exception{
		assertNotNull(nodeLockerA);
		assertNotNull(nodeLockerB);
		assertNotNull(nodeDao);
		// Create a node
		Long creatorUserGroupId = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		Node theNode = NodeTestUtils.createNew("NodeLockTest", creatorUserGroupId);
		nodeId = nodeDao.createNew(theNode);
		assertNotNull(nodeId);
	}
	
	@After
	public void after(){
		// Must release any locks before we can delete the node.
		if(nodeLockerA != null){
			nodeLockerA.releaseLock();
		}
		if(nodeLockerB != null){
			nodeLockerB.releaseLock();
		}
		if(nodeDao != null && nodeId != null){
			try {
				nodeDao.delete(nodeId);
			} catch (NotFoundException e) {

			} catch (DatastoreException e) {
				
			}
		}
	}
	
	@Test
	public void testNodeLocking() throws InterruptedException, NotFoundException, DatastoreException{
		// First start a new thread and acquire the lock to the node
		final String theNodeId = nodeId;
		final String eTag = nodeDao.peekCurrentEtag(nodeId);
		Thread threadOne = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					nodeLockerA.aquireAndHoldLock(theNodeId, eTag);
				} catch (Exception e) {
					fail(e.getMessage());
				}
			}
		});
		// Start the thread
		threadOne.start();
		// Now wait for the thread to acquire the lock
		long start = System.currentTimeMillis();
		while(!nodeLockerA.isLockAcquired()){
			long current = System.currentTimeMillis();
			if((current-start) > timeout) fail("Test timed out trying to aqcuire a lock!");
			Thread.sleep(100);
		}
		assertTrue("Failed to acquire the lock on A",nodeLockerA.isLockAcquired());
		
		// Now have another thread try to acquire the same lock
		Thread threadTwo = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					nodeLockerB.aquireAndHoldLock(theNodeId, eTag);
				} catch (InterruptedException e) {
					fail(e.getMessage());
				} catch (NotFoundException e) {
					fail(e.getMessage());
				} catch (ConflictingUpdateException e) {
					fail(e.getMessage());
				} catch (DatastoreException e) {
					fail(e.getMessage());
				}
			}
		});
		// Start it up.
		threadTwo.start();
		// Make sure thread two cannot acquire the lock even after the timeout
		start = System.currentTimeMillis();
		while((System.currentTimeMillis()-start) < timeout){
			assertTrue("The first thread should be holding the lock.", nodeLockerA.isLockAcquired());
			assertFalse("The second thread should not have been able to acquire the lock while thread one was holding it.", nodeLockerB.isLockAcquired());
			Thread.sleep(100);
		}
		// Now release lock A and make sure B can acquire it
		nodeLockerA.releaseLock();
		// Now make sure B can acquire it
		start = System.currentTimeMillis();
		// Wait for B to fail due to conflicting eTag
		while(!nodeLockerB.failedDueToConflict()){
			long current = System.currentTimeMillis();
			if((current-start) > timeout) fail("Test timed out trying to aqcuire a lock!");
			Thread.sleep(100);
		}
		assertTrue("B should have failed due to a conflicting eTag",nodeLockerB.failedDueToConflict());
		
		// Release both locks
		nodeLockerA.releaseLock();
		nodeLockerB.releaseLock();
			
	}
	

}
