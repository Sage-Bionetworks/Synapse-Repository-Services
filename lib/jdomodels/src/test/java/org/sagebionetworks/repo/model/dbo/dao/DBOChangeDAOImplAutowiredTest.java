package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.ProcessedMessageDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOSentMessage;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.Callback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Sets;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOChangeDAOImplAutowiredTest {
	
	@Autowired
	DBOChangeDAO changeDAO;
	
	@Autowired
	ProcessedMessageDAO processedMessageDAO;
	
	@Before
	public void before(){
		if(changeDAO != null){
			changeDAO.deleteAllChanges();
		}
	}
	
	@After
	public void after(){
		if(changeDAO != null){
			changeDAO.deleteAllChanges();
		}
	}
	
	@Test
	public void testGetCurrentChangeNumberEmpty() {
		long ccn = changeDAO.getCurrentChangeNumber();
		assertEquals(0L, ccn);
	}
	
	@Test
	public void testGetMinimumChangeNumberEmpty() {
		long mcn = changeDAO.getMinimumChangeNumber();
		assertEquals(0L, mcn);
	}
	
	@Test
	public void testGetCountEmpty() {
		long count = changeDAO.getCount();
		assertEquals(0L, count);
	}
	
	@Test
	public void testDoesChangeNumberExistNot(){
		long doesNotExist = -1;
		// call under test
		boolean exists = changeDAO.doesChangeNumberExist(doesNotExist);
		assertFalse(exists);
	}
	
	@Test
	public void testReplace(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId("syn123");
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(ObjectType.ENTITY);
		ChangeMessage clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		System.out.println(clone);
		assertNotNull(clone.getChangeNumber());
		assertNotNull(clone.getTimestamp());
		// The change number should exist
		assertTrue(changeDAO.doesChangeNumberExist(clone.getChangeNumber()));
		long firstChangeNumber = clone.getChangeNumber();
		assertEquals(change.getObjectId(), clone.getObjectId());
		assertEquals(change.getChangeType(), clone.getChangeType());
		assertEquals(change.getObjectType(), clone.getObjectType());
		// Now replace it again
		clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		long secondChangeNumber = clone.getChangeNumber();
		System.out.println(clone);
		assertTrue(secondChangeNumber > firstChangeNumber);
	}
	
	/**
	 * ObjectIds can be duplicated, so make sure replace uses a composite key.
	 */
	@Test
	public void testReplaceDuplicateObjectId(){
		ChangeMessage changeOne = new ChangeMessage();
		changeOne.setObjectId("123");
		changeOne.setChangeType(ChangeType.CREATE);
		changeOne.setObjectType(ObjectType.ACTIVITY);
		changeDAO.replaceChange(changeOne);
		
		// Now create a second change with the same id but different type.
		ChangeMessage changeTwo = new ChangeMessage();
		changeTwo.setObjectId(changeOne.getObjectId());
		changeTwo.setChangeType(ChangeType.CREATE);
		changeTwo.setObjectType(ObjectType.PRINCIPAL);
		changeDAO.replaceChange(changeTwo);
		
		// Now we should see both changes listed
		List<ChangeMessage> list = changeDAO.listChanges(0, ObjectType.ACTIVITY, 100);
		assertNotNull(list);
		assertEquals(1, list.size());
		ChangeMessage message =  list.get(0);
		assertEquals(ObjectType.ACTIVITY, message.getObjectType());
		// Check the principal list
		list = changeDAO.listChanges(0, ObjectType.PRINCIPAL, 100);
		assertNotNull(list);
		assertEquals(1, list.size());
		message =  list.get(0);
		assertEquals(ObjectType.PRINCIPAL, message.getObjectType());
		
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void tesNullId(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId(null);
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(ObjectType.ENTITY);
		changeDAO.replaceChange(change);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void tesNullChangeType(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId("syn334");
		change.setChangeType(null);
		change.setObjectType(ObjectType.ENTITY);
		changeDAO.replaceChange(change);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void tesNullObjectTypeType(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId("syn123");
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(null);
		changeDAO.replaceChange(change);
	}
	
	/**
	 * Etag can be null for deletes.
	 */
	@Test
	public void tesNullEtagForDelete(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId("syn223");
		change.setChangeType(ChangeType.DELETE);
		change.setObjectType(ObjectType.ENTITY);
		ChangeMessage clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
	}

	
	@Test
	public void testSortByObjectId(){
		List<ChangeMessage> batch = createList(5, ObjectType.PRINCIPAL);
		// Start shuffled
		Collections.shuffle(batch);
		// Now sort
		batch = ChangeMessageUtils.sortByObjectId(batch);
		assertNotNull(batch);
		long previous = -1;
		for(ChangeMessage change: batch){
			Long id = Long.parseLong(change.getObjectId());
			assertTrue(id > previous);
			previous = id;
		}
	}
	
	@Test
	public void testSortByIdSameIdDifferentType(){
		List<ChangeMessage> batch = new LinkedList<ChangeMessage>();
		ChangeMessage message = new ChangeMessage();
		message.setObjectId("123");
		message.setObjectType(ObjectType.PRINCIPAL);
		batch.add(message);
		message = new ChangeMessage();
		message.setObjectId("123");
		message.setObjectType(ObjectType.ACTIVITY);
		batch.add(message);
		// Now sort
		batch = ChangeMessageUtils.sortByObjectId(batch);
		assertNotNull(batch);
		// the activity should be first now
		assertEquals("Activity should have been placed before principal",ObjectType.ACTIVITY, batch.get(0).getObjectType());
		assertEquals("Activity should have been placed before principal",ObjectType.PRINCIPAL, batch.get(1).getObjectType());
	}
	
	@Test
	public void testSortByChangeNumber(){
		List<ChangeMessage> batch = new ArrayList<ChangeMessage>();
		for(int i=0; i<5; i++){
			ChangeMessage change = new ChangeMessage();
			change.setObjectId("syn"+i);
			change.setChangeType(ChangeType.UPDATE);
			change.setObjectType(ObjectType.ENTITY);
			change.setChangeNumber(new Long(i));
			batch.add(change);
		}
		// Start shuffled
		Collections.shuffle(batch);
		// Now sort
		batch = ChangeMessageUtils.sortByChangeNumber(batch);
		assertNotNull(batch);
		long previous = -1;
		for(ChangeMessage change: batch){
			assertTrue(change.getChangeNumber() > previous);
			previous = change.getChangeNumber();
		}
	}
	
	@Test
	public void testReplaceBatch(){
		// Get the current change number
		int numChangesInBatch = 5;
		long startChangeNumber = startChangeNumber();
		List<ChangeMessage> batch = createList(5, ObjectType.PRINCIPAL);
		// We want to start with an unordered list of changes
		// because the batch replace must sort the list by object id
		// to ensure a consistent update order to prevent deadlock.
		Collections.shuffle(batch);
		System.out.println(batch);
		// Pass the batch.
		batch  = changeDAO.replaceChange(batch);
		// The resulting list 
		assertNotNull(batch);
		
		// If the changes were sorted before replaced, then sorting by change number should
		// give use the same order as sorting by objectId.
		// by change number
		List<ChangeMessage> byChangeNumber = new LinkedList<ChangeMessage>(batch);
		byChangeNumber = ChangeMessageUtils.sortByChangeNumber(byChangeNumber);
		// by object id
		List<ChangeMessage> byObjectId = new LinkedList<ChangeMessage>(batch);
		byObjectId = ChangeMessageUtils.sortByObjectId(byObjectId);
		assertEquals("If the batch was sorted by objectID before replacing then the change number should be in the same order as the object ids", byChangeNumber, byObjectId);
		
		// Check the change numbers
		long endChangeNumber = changeDAO.getCurrentChangeNumber();
		assertEquals(startChangeNumber + numChangesInBatch, endChangeNumber);
		
		// The element inserted by startChangeNumber() will be replaced by an element created in createList(5, ...)
		long minChangeNumber = changeDAO.getMinimumChangeNumber();
		assertEquals(startChangeNumber + 1, minChangeNumber);
		
		long countChangeNumber = changeDAO.getCount();
		assertEquals(numChangesInBatch, countChangeNumber);
	}
	
	@Test
	public void testListChangesNullType(){
		// Get the current change number
		List<ChangeMessage> batch = createList(2, ObjectType.ENTITY);
		// Pass the batch.
		batch  = changeDAO.replaceChange(batch);
		// The resulting list 
		assertNotNull(batch);
		// Now listing this should return the same as the batch
		List<ChangeMessage> list = changeDAO.listChanges(batch.get(0).getChangeNumber(), null, 10);
		assertEquals(batch, list);
	}
	
	@Test
	public void testListChangesType(){
		// Get the current change number
		List<ChangeMessage> batch = new ArrayList<ChangeMessage>();
		List<ChangeMessage> expectedFiltered = new ArrayList<ChangeMessage>();
		for(int i=0; i<5; i++){
			ChangeMessage change = new ChangeMessage();
			change.setChangeType(ChangeType.UPDATE);
			if(i%2 > 0){
				change.setObjectType(ObjectType.ENTITY);
				change.setObjectId("syn"+i);
				expectedFiltered.add(change);
			}else{
				change.setObjectType(ObjectType.PRINCIPAL);
				change.setObjectId(""+i);
			}
			batch.add(change);
		}
		// Pass the batch.
		batch  = changeDAO.replaceChange(batch);
		// Now listing this should return the same as the batch
		List<ChangeMessage> list = changeDAO.listChanges(batch.get(0).getChangeNumber(), ObjectType.ENTITY, 10);
		// Clear the timestamps and changeNumber before we do the compare
		for(ChangeMessage cm: list){
			assertNotNull(cm.getTimestamp());
			assertNotNull(cm.getChangeNumber());
			cm.setTimestamp(null);
			cm.setChangeNumber(null);
		}
		assertEquals(expectedFiltered, list);
	}
	
	@Test
	public void testRegisterSentAndListUnsent(){
		// Create a few messages.
		List<ChangeMessage> batch = createList(2, ObjectType.ENTITY);
		// Pass the batch.
		batch  = changeDAO.replaceChange(batch);
		// Both should be listed as unsent.
		List<ChangeMessage> unSent = changeDAO.listUnsentMessages(3); 
		assertEquals(batch, unSent);
		// Now register one
		changeDAO.registerMessageSent(batch.get(1));
		unSent = changeDAO.listUnsentMessages(3);
		assertNotNull(unSent);
		assertEquals(1, unSent.size());
		assertEquals(batch.get(0).getChangeNumber(), unSent.get(0).getChangeNumber());
		// Register the second
		changeDAO.registerMessageSent(batch.get(0));
		unSent = changeDAO.listUnsentMessages(3);
		assertNotNull(unSent);
		assertEquals(0, unSent.size());
	}
	
	@Test
	public void testReplaceDeleteSent(){
		List<ChangeMessage> batch = createList(1, ObjectType.ENTITY);
		// Pass the batch.
		batch  = changeDAO.replaceChange(batch);
		// Send the batch
		changeDAO.registerMessageSent(batch.get(0));
		// Replace the batch again
		batch  = changeDAO.replaceChange(batch);
		// This will fail if we did not delete the sent message.
		changeDAO.registerMessageSent(batch.get(0));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testRegisterMessageSentMixedBatch(){
		List<ChangeMessage> mixed = createList(1, ObjectType.ENTITY);
		mixed  = changeDAO.replaceChange(mixed);
		mixed.addAll(createList(1, ObjectType.ACTIVITY));
		changeDAO.registerMessageSent(ObjectType.ENTITY, mixed);
	}
	
	@Test
	public void testRegisterMessageSentBatch(){
		List<ChangeMessage> batch = createList(5, ObjectType.ENTITY);
		ChangeMessage fistMessage = batch.get(0);
		fistMessage.setObjectVersion(-1L);
		batch  = changeDAO.replaceChange(batch);
		changeDAO.registerMessageSent(ObjectType.ENTITY, batch);
		List<ChangeMessage> unsent = changeDAO.listUnsentMessages(10L);
		assertEquals(0, unsent.size());
		
		// check the first sent (PLFM-3739).
		DBOSentMessage sent = changeDAO.getSentMessage(fistMessage.getObjectId(), fistMessage.getObjectVersion(), fistMessage.getObjectType());
		assertNotNull(sent);
		assertNotNull(sent.getTimeStamp());
		assertEquals(KeyFactory.stringToKey(fistMessage.getObjectId()), sent.getObjectId());
		assertEquals(fistMessage.getObjectType().name(), sent.getObjectType());
		assertEquals(fistMessage.getObjectVersion(), sent.getObjectVersion());
	}
	
	
	@Test
	public void testGetMaxSentChangeNumber(){
		assertEquals("When the sent messages is empty, the max sent change number should be -1",new Long(-1), changeDAO.getMaxSentChangeNumber(Long.MAX_VALUE));
		List<ChangeMessage> batch = createList(3, ObjectType.ENTITY);
		// Add all three to changes
		batch  = changeDAO.replaceChange(batch);
		// Only add the first and last to sent.
		changeDAO.registerMessageSent(batch.get(0));
		changeDAO.registerMessageSent(batch.get(2));
		Long firstChangeNumber = batch.get(0).getChangeNumber();
		Long secondChangeNumber = batch.get(1).getChangeNumber();
		Long thirdChangeNumber = batch.get(2).getChangeNumber();
		assertEquals(firstChangeNumber, changeDAO.getMaxSentChangeNumber(firstChangeNumber));
		assertEquals("Since the second change number was not sent, the max sent change number less than or equals to the second should be the first.",firstChangeNumber, changeDAO.getMaxSentChangeNumber(secondChangeNumber));
		assertEquals(thirdChangeNumber, changeDAO.getMaxSentChangeNumber(thirdChangeNumber));
		assertEquals(new Long(-1), changeDAO.getMaxSentChangeNumber(new Long(-1)));
	}
	
	@Test
	public void testUpdateSentMessageSameIdDifferentType(){
		// Create a few messages.
		List<ChangeMessage> batch = createList(2, ObjectType.ENTITY);
		ChangeMessage zero = batch.get(0);
		zero.setObjectId("123");
		zero.setObjectType(ObjectType.TABLE);
		ChangeMessage one = batch.get(1);
		one.setObjectId("123");
		one.setObjectType(ObjectType.ENTITY);
		// Pass the batch.
		batch  = changeDAO.replaceChange(batch);
		// Register as sent
		changeDAO.registerMessageSent(batch.get(0));
		changeDAO.registerMessageSent(batch.get(1));
		// Pass the batch.
		batch  = changeDAO.replaceChange(batch);
		// again
		changeDAO.registerMessageSent(batch.get(0));
		changeDAO.registerMessageSent(batch.get(1));
	}
	
	@Test
	public void testRegisterProcessedAndListNotProcessed() throws Exception{
		// Create msgs
		List<ChangeMessage> batch = createList(3, ObjectType.ENTITY);
		batch = changeDAO.replaceChange(batch);
		List<ChangeMessage> notProcessed = processedMessageDAO.listNotProcessedMessages("Q", 3);
		assertEquals(0, notProcessed.size());
		// Register sent msgs
		changeDAO.registerMessageSent(batch.get(0));
		Thread.sleep(500);
		changeDAO.registerMessageSent(batch.get(1));
		Thread.sleep(500);
		changeDAO.registerMessageSent(batch.get(2));
		Thread.sleep(500);
		List<ChangeMessage> notSent = changeDAO.listUnsentMessages(3);
		assertEquals(0, notSent.size());
		notProcessed = processedMessageDAO.listNotProcessedMessages("Q1", 3);
		assertEquals(3, notProcessed.size());
		// Register a processed msg for queue Q
		processedMessageDAO.registerMessageProcessed(batch.get(1).getChangeNumber(), "Q1");
		notProcessed = processedMessageDAO.listNotProcessedMessages("Q1", 3);
		assertEquals(2, notProcessed.size());
		// Register another processed msg for queue Q
		processedMessageDAO.registerMessageProcessed(batch.get(0).getChangeNumber(), "Q1");
		notProcessed = processedMessageDAO.listNotProcessedMessages("Q1", 3);
		assertEquals(1, notProcessed.size());
		notProcessed = processedMessageDAO.listNotProcessedMessages("Q2", 3);
		assertEquals(3, notProcessed.size());
		// Register same msg as processed for queue Q2
		processedMessageDAO.registerMessageProcessed(batch.get(0).getChangeNumber(), "Q2");
		notProcessed = processedMessageDAO.listNotProcessedMessages("Q2", 3);
		assertEquals(2, notProcessed.size());
	}
	
	@Test
	public void testListUnsentRange() {
		// Create a set of changes with change numbers like:
		// 0 _ 2 3 _ 5 6
		List<ChangeMessage> batch = createList(5, ObjectType.ENTITY);
		batch = changeDAO.replaceChange(batch);
		batch.add(changeDAO.replaceChange(batch.remove(1)));
		batch.add(changeDAO.replaceChange(batch.remove(3)));
		
		long min = changeDAO.getMinimumChangeNumber();
		long max = changeDAO.getCurrentChangeNumber();
		
		// Get everything
		List<ChangeMessage> unSent = changeDAO.listUnsentMessages(min, max, new Timestamp(System.currentTimeMillis())); 
		assertEquals(batch, unSent);
		
		// Shrink the range and check each iteration for correctness
		for (int i = 0; i < batch.size(); i++) {
			if (i % 2 == 0) {
				ChangeMessage removed = batch.remove(0);
				min = removed.getChangeNumber() + 1;
			} else {
				ChangeMessage removed = batch.remove(batch.size() - 1);
				max = removed.getChangeNumber() - 1;
			}
			unSent = changeDAO.listUnsentMessages(min, max, new Timestamp(System.currentTimeMillis())); 
			assertEquals(batch, unSent);
		}
	}
	
	/**
	 * Duplicate entry for key 'CHANGES_UKEY_OID_OTYPE' can occur with fast concurrent updates
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	@Test
	public void testPLFM2756() throws InterruptedException, ExecutionException{
		final int timesToRun = 100;
		Callable<Integer> callable = new Callable<Integer>(){
			@Override
			public Integer call() throws Exception {
				List<ChangeMessage> toSpam = createList(1, ObjectType.TABLE);
				for(int i=0; i<timesToRun; i++){
					changeDAO.replaceChange(toSpam);
				}
				return timesToRun;
		}};
		// Run multiple threads at the same time
		ExecutorService pool = Executors.newFixedThreadPool(2);
		// Submit twice
		Future<Integer> one = pool.submit(callable);
		// Submit again
		Future<Integer> two = pool.submit(callable);
		// There should be no errors
		Integer oneResult = one.get();
		assertEquals(new Integer(timesToRun), oneResult);
		Integer twoResult = two.get();
		assertEquals(new Integer(timesToRun), twoResult);
	}
	
	/**
	 * This test creates, then updates change messages on two separate threads.
	 * One thread works with even ObjectIds while the other works with odd. 
	 * This test would cause deadlock 4 out of 5 times before we fixed PLFM-3329.
	 * 
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void testPLFM3329() throws InterruptedException, ExecutionException{
		final int number = 25;
		final int timesToRun = 100;
		Callable<Integer> even = new Callable<Integer>(){
			@Override
			public Integer call() throws Exception {
				// create new changes for each run.
				for(int t=0; t<timesToRun; t++){
					List<ChangeMessage> batch = new LinkedList<ChangeMessage>();
					// start at zero for even ids.
					for(int i=0; i< number; i+=2){
						batch.add(createChange(ObjectType.FILE, i));
					}
					Collections.shuffle(batch);
					changeDAO.replaceChange(batch);
				}
				return timesToRun;
		}};
		
		Callable<Integer> odd = new Callable<Integer>(){
			@Override
			public Integer call() throws Exception {
				// create new changes for each run.
				for(int t=0; t<timesToRun; t++){
					List<ChangeMessage> batch = new LinkedList<ChangeMessage>();
					// start at one for odd ids.
					for(int i=1; i< number; i+=2){
						batch.add(createChange(ObjectType.FILE, i));
					}
					Collections.shuffle(batch);
					changeDAO.replaceChange(batch);
				}
				return timesToRun;
		}};
		// Run multiple threads at the same time
		ExecutorService pool = Executors.newFixedThreadPool(2);
		// Submit twice
		Future<Integer> one = pool.submit(even);
		// Submit again
		Future<Integer> two = pool.submit(odd);
		// There should be no errors
		try {
			Integer oneResult = one.get();
			Integer twoResult = two.get();
			assertEquals(new Integer(timesToRun), oneResult);
			assertEquals(new Integer(timesToRun), twoResult);
		} catch (ExecutionException e) {
			if(e.getCause() instanceof DeadlockLoserDataAccessException){
				fail("Failed to make non-conflicting additions to the changes table without causing deadlock");
			}else{
				// unknown error
				throw e;
			}
		}
	}
	
	@Test
	public void testCheckUnsentMessageByCheckSumForRange(){
		List<ChangeMessage> starting = createList(5, ObjectType.PRINCIPAL);
		starting = changeDAO.replaceChange(starting);
		assertFalse("The check-sums should not match",changeDAO.checkUnsentMessageByCheckSumForRange(0L, Long.MAX_VALUE));
		// Send each
		for(ChangeMessage toSend: starting){
			assertFalse("The check-sums should not match",changeDAO.checkUnsentMessageByCheckSumForRange(0L, Long.MAX_VALUE));
			changeDAO.registerMessageSent(toSend);
		}
		// The should match now that all are sent
		assertTrue("Change and sent are in-synch so their check-sums should match",changeDAO.checkUnsentMessageByCheckSumForRange(0L, Long.MAX_VALUE));
	}
	
	/**
	 * Will add a row to start the a test.
	 * @return
	 */
	public Long startChangeNumber(){
		List<ChangeMessage> starting = createList(1, ObjectType.PRINCIPAL);
		starting = changeDAO.replaceChange(starting);
		return starting.get(0).getChangeNumber();
	}

	/**
	 * Helper to build up a list of changes.
	 * @param numChangesInBatch
	 * @return
	 */
	private List<ChangeMessage> createList(int numChangesInBatch, ObjectType type) {
		List<ChangeMessage> batch = new ArrayList<ChangeMessage>();
		for(int i=0; i<numChangesInBatch; i++){
			ChangeMessage change = createChange(type, i);
			batch.add(change);
		}
		return batch;
	}

	/**
	 * Create a test change number
	 * @param type
	 * @param i
	 * @return
	 */
	public ChangeMessage createChange(ObjectType type, int i) {
		ChangeMessage change = new ChangeMessage();
		if(ObjectType.ENTITY == type){
			change.setObjectId("syn"+i);
		}else{
			change.setObjectId(""+i);
		}
		change.setChangeType(ChangeType.UPDATE);
		change.setObjectType(type);
		return change;
	}
	
	@Test
	public void testGetChangesForObjectIds(){
		List<ChangeMessage> entityBatch = createList(3, ObjectType.ENTITY);
		changeDAO.replaceChange(entityBatch);
		List<ChangeMessage> principalBatch = createList(3, ObjectType.PRINCIPAL);
		changeDAO.replaceChange(principalBatch);
		
		Long entityIdOne = KeyFactory.stringToKey(entityBatch.get(0).getObjectId());
		Long entityIdTwo = KeyFactory.stringToKey(entityBatch.get(1).getObjectId());
		Set<Long> idsToFetch = Sets.newHashSet(entityIdOne, entityIdTwo);
		// call under test
		List<ChangeMessage> results = changeDAO.getChangesForObjectIds(ObjectType.ENTITY, idsToFetch);
		assertNotNull(results);
		assertEquals(2, results.size());
		for(ChangeMessage message: results){
			if(entityBatch.get(0).getObjectId().equals(message.getObjectId())){
				assertEquals(ObjectType.ENTITY, message.getObjectType());
			}else if(entityBatch.get(1).getObjectId().equals(message.getObjectId())){
				assertEquals(ObjectType.ENTITY, message.getObjectType());
			}else{
				fail("unexpected result: "+message);
			}
		}
	}
	
	@Test
	public void testGetChangesForObjectIdsEmpty(){
		Set<Long> idsToFetch = new HashSet<>();
		// call under test
		List<ChangeMessage> results = changeDAO.getChangesForObjectIds(ObjectType.ENTITY, idsToFetch);
		assertNotNull(results);
		assertTrue(results.isEmpty());
	}
}
