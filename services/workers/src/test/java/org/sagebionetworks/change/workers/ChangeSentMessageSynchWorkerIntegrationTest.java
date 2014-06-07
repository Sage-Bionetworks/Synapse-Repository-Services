package org.sagebionetworks.change.workers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdGenerator.TYPE;
import org.sagebionetworks.repo.manager.SemaphoreManager;
import org.sagebionetworks.repo.manager.message.RepositoryMessagePublisher;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.dao.DBOChangeDAO;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.ClockProvider;
import org.sagebionetworks.util.DefaultClockProvider;
import org.sagebionetworks.util.TimeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.base.Predicate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class ChangeSentMessageSynchWorkerIntegrationTest {
	
	static private Logger log = LogManager.getLogger(ChangeSentMessageSynchWorkerIntegrationTest.class);
	/**
	 * The max amount of time we wait for the worker to complete the task.
	 */
	public static final int MAX_PUBLISH_WAIT_MS = 5*1000;
	
	@Autowired
	IdGenerator idGenerator;
	@Autowired
	DBOChangeDAO changeDao;
	@Autowired
	SemaphoreManager semphoreManager;
	@Autowired
	ChangeSentMessageSynchWorker changeSentMessageSynchWorker;
	@Autowired
	RepositoryMessagePublisher repositoryMessagePublisher;
	
	private int objectIdSequence;
	private int batchSize;
	
	@Before
	public void before(){
		// If there are any pending messages make sure they are sent before we start this test
		repositoryMessagePublisher.timerFired();
		changeDao.deleteAllChanges();
		changeDao.resetLastChangeNumber();
		semphoreManager.releaseAllLocksAsAdmin(new UserInfo(true));
		objectIdSequence = 0;
		batchSize = 3;
		changeSentMessageSynchWorker.setBatchSize(batchSize);
		ReflectionTestUtils.setField(changeSentMessageSynchWorker, "clockProvider", new ClockProvider() {
			@Override
			public void sleep(long millis) throws InterruptedException {
				throw new UnsupportedOperationException();
			}
			
			@Override
			public long currentTimeMillis() {
				// always way in the future.
				return Long.MAX_VALUE;
			}
		});
	}
	
	@After
	public void after(){
		if(changeSentMessageSynchWorker != null){
			ReflectionTestUtils.setField(changeSentMessageSynchWorker, "clockProvider", new DefaultClockProvider());
		}
	}
	
	@Test
	public void testMissing() throws InterruptedException{
		// At the start the last change number should be -1 after the worker has a chance to run.
		waitForSynchState();
		// Create some change messages
		List<ChangeMessage> changes = createList(3, ObjectType.ACTIVITY);
		// set the changes
		changes = changeDao.replaceChange(changes);
		waitForSynchState();
		assertEquals(changes.get(2).getChangeNumber(), changeDao.getLastSynchedChangeNumber());
		
		// create more changes
		changes = createList(3, ObjectType.ACTIVITY);
		changes = changeDao.replaceChange(changes);
		waitForSynchState();
		assertEquals(changes.get(2).getChangeNumber(), changeDao.getLastSynchedChangeNumber());

		// By replacing changes the LastSynchedChangeNumber will no longer exist in the sent table.
		changes = changeDao.replaceChange(changes);
		// At this point the LastSynchedChangeNumber will not longer exist in the sent table.
		// There are also two changes that have not been sent that have change numbers less than the max sent change number
		// the worker must be able to find this changes and send them.
		waitForSynchState();
		assertEquals(changes.get(2).getChangeNumber(), changeDao.getLastSynchedChangeNumber());
	}
	
	@Test
	public void testPLFM_2814() throws Exception {
		// At the start the last change number should be -1 after the worker has a chance to run.
		waitForSynchState();
		List<ChangeMessage> changes = createList(3, ObjectType.ACTIVITY);
		// We need to save these change numbers such that their change numbers have gaps larger than the batch size
		ChangeMessage last = null;
		for(ChangeMessage change: changes){
			change = changeDao.replaceChange(change);
			last = change;
			// Move the ID generator forward
			idGenerator.reserveId(change.getChangeNumber()+batchSize+10, TYPE.CHANGE_ID);
		}
		// The worker should skip over all gaps and complete the synch.
		waitForSynchState();
		assertEquals(last.getChangeNumber(), changeDao.getLastSynchedChangeNumber());
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
		boolean passed = TimeUtils.waitFor(MAX_PUBLISH_WAIT_MS, 1000, null, new Predicate<Void>() {
			@Override
			public boolean apply(Void input) {
				List<ChangeMessage> notSynchded = changeDao.listUnsentMessages(0, Long.MAX_VALUE, new Timestamp(System.currentTimeMillis()));
				return notSynchded.isEmpty();
			}
		});
		assertTrue("Timed out waiting for ChangeSentMessageSynchWorker to synchronize",passed);
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
