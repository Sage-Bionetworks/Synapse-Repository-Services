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
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
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
@ContextConfiguration(locations = { "classpath:jdomodles-test-context.xml" })
public class NodeLockTest {
	
	private static Logger log = Logger.getLogger(NodeLockTest.class.getName());
	/**
	 * The max amount of time to give any thread in this test.
	 */
	private static long timeout = 2000;
	
	@Autowired
	JDONodeLockChecker nodeLockerA;
	@Autowired
	JDONodeLockChecker nodeLockerB;
	@Autowired
	NodeDAO nodeDao;
	
	private String nodeId;
	
	
	@Before
	public void before(){
		assertNotNull(nodeLockerA);
		assertNotNull(nodeLockerB);
		assertNotNull(nodeDao);
		// Create a node
		Node theNode = Node.createNew("NodeLockTest");
		nodeId = nodeDao.createNew(null, theNode);
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
			nodeDao.delete(nodeId);
		}
	}
	
	@Test
	public void testNodeLocking() throws InterruptedException{
		// Locking does not work with in-memory database, so only run this 
		// test when we are using MySQL
		if(PMFConfigUtils.getJdbcConnectionString() == null){
			log.info("Skipping NodeLockTest.testNodeLocking() because HyperSQL does not support locking");
			return;
		}else{
			log.info("Running NodeLockTest.testNodeLocking() against: "+PMFConfigUtils.getJdbcConnectionString());
		}
		// First start a new thread and acquire the lock to the node
		final String theNodeId = nodeId;
		Thread threadOne = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					nodeLockerA.aquireAndHoldLock(theNodeId);
				} catch (InterruptedException e) {
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
					nodeLockerB.aquireAndHoldLock(theNodeId);
				} catch (InterruptedException e) {
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
		while(!nodeLockerB.isLockAcquired()){
			long current = System.currentTimeMillis();
			if((current-start) > timeout) fail("Test timed out trying to aqcuire a lock!");
			Thread.sleep(100);
		}
		assertTrue("Failed to acquire the lock on B",nodeLockerB.isLockAcquired());
		
		// Release both locks
		nodeLockerA.releaseLock();
		nodeLockerB.releaseLock();
			
	}
	

}
