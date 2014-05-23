package org.sagebionetworks.change.workers;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ChangeSentMessageSynchWorkerIntegrationTest {
	/**
	 * The max amount of time we wait for the worker to complete the task.
	 */
	public static final int MAX_PUBLISH_WAIT_MS = 5*1000;
	
	@Autowired
	DBOChangeDAO changeDao;
	@Autowired
	SemaphoreManager semphoreManager;
	@Autowired
	ChangeSentMessageSynchWorker changeSentMessageSynchWorker;
	@Autowired
	RepositoryMessagePublisher repositoryMessagePublisher;
	Object monitor;
	
	private int objectIdSequence;
	
	@Before
	public void before(){
		// If there are any pending messages make sure they are sent before we start this test
		repositoryMessagePublisher.timerFired();
		changeDao.deleteAllChanges();
		changeDao.resetLastChangeNumber();
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		objectIdSequence = 0;
		monitor = new Object();
		changeSentMessageSynchWorker.setMonitor(monitor);
	}
	
	@Test
	public void testMissing() throws InterruptedException{
		// Create some change messages
		List<ChangeMessage> changes = createList(3, ObjectType.ACTIVITY);
		// set the changes
		changes = changeDao.replaceChange(changes);
		// they should be synched by the time the worker is done.
		waitForSynchState();
		assertEquals("The last synched changed number should match the last change added.",changes.get(2).getChangeNumber(), changeDao.getLastSynchedChangeNumber());
		// create more changes
		changes = createList(3, ObjectType.ACTIVITY);
		changes = changeDao.replaceChange(changes);
		// they should be synched by the time the worker is done.
		waitForSynchState();
		assertEquals("The last synched changed number should match the last change added.",changes.get(2).getChangeNumber(), changeDao.getLastSynchedChangeNumber());
		// By replacing changes the LastSynchedChangeNumber will no longer exist in the sent table.
		changes = changeDao.replaceChange(changes);
		// Set the last message as sent.
		changeDao.registerMessageSent(changes.get(2));
		// At this point the LastSynchedChangeNumber will not longer exist in the sent table.
		// There are also two changes that have not been sent that have change numbers less than the max sent change number
		// the worker must be able to find this changes and send them.
		waitForSynchState();
		assertEquals("The last synched changed number should match the last change added.",changes.get(2).getChangeNumber(), changeDao.getLastSynchedChangeNumber());
	}
	
	@Ignore // this is a long running test that does not always need to be run.
	@Test
	public void testMissingMultipleTimes() throws InterruptedException{
		// Now matter how many times we run the testMissing test it should still pass
		for(int i=0; i<100; i++){
			System.out.println("Run: "+i);
			testMissing();
		}
	}
	/**
	 * Wait for all of the messages to be synched.
	 * @throws InterruptedException 
	 */
	private void waitForSynchState() throws InterruptedException {
		// Wait for the worker to run once
		long start = System.currentTimeMillis();
		List<ChangeMessage> notSynched = null;
		while (notSynched == null || !notSynched.isEmpty()) {
			System.out.println("Waiting for ChangeSentMessageSynchWorker to run...");
			synchronized (monitor) {
				monitor.wait(MAX_PUBLISH_WAIT_MS);
			}
			notSynched = changeDao.listUnsentMessages(0, Long.MAX_VALUE);
			assertTrue("Timed out waiting for ChangeSentMessageSynchWorker", System.currentTimeMillis() - start < MAX_PUBLISH_WAIT_MS);
		}
	}
	
	/**
	 * Helper to build up a list of changes.
	 * @param numChangesInBatch
	 * @return
	 */
	private List<ChangeMessage> createList(int numChangesInBatch, ObjectType type) {
		List<ChangeMessage> batch = new ArrayList<ChangeMessage>();
		for(int i=0; i<numChangesInBatch; i++){
			ChangeMessage change = new ChangeMessage();
			if(ObjectType.ENTITY == type){
				change.setObjectId("syn"+objectIdSequence++);
			}else{
				change.setObjectId(""+objectIdSequence++);
			}
			change.setObjectEtag(UUID.randomUUID().toString());
			change.setChangeType(ChangeType.UPDATE);
			change.setObjectType(type);
			batch.add(change);
		}
		return batch;
	}
}
