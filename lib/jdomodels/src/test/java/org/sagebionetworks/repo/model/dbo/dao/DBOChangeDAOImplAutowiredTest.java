package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOChangeDAOImplAutowiredTest {
	
	@Autowired
	DBOChangeDAO changeDAO;
	
	@After
	public void after(){
		if(changeDAO != null){
			changeDAO.deleteAllChanges();
		}
	}
	
	@Test
	public void testReplace(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId("syn123");
		change.setObjectEtag("myEtag");
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(ObjectType.ENTITY);
		ChangeMessage clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		System.out.println(clone);
		assertNotNull(clone.getChangeNumber());
		assertNotNull(clone.getTimestamp());
		long firstChangeNumber = clone.getChangeNumber();
		assertEquals(change.getObjectId(), clone.getObjectId());
		assertEquals(change.getObjectEtag(), clone.getObjectEtag());
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
		changeOne.setObjectEtag("myEtag");
		changeOne.setChangeType(ChangeType.CREATE);
		changeOne.setObjectType(ObjectType.ACTIVITY);
		ChangeMessage clone = changeDAO.replaceChange(changeOne);
		// Now create a second change with the same id but different type.
		ChangeMessage changeTwo = new ChangeMessage();
		changeTwo.setObjectId(changeOne.getObjectId());
		changeTwo.setObjectEtag("myEtag");
		changeTwo.setChangeType(ChangeType.CREATE);
		changeTwo.setObjectType(ObjectType.PRINCIPAL);
		ChangeMessage clonetwo = changeDAO.replaceChange(changeTwo);
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
		change.setObjectEtag("myEtag");
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(ObjectType.ENTITY);
		ChangeMessage clone = changeDAO.replaceChange(change);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void tesNullChangeType(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId("syn334");
		change.setObjectEtag("myEtag");
		change.setChangeType(null);
		change.setObjectType(ObjectType.ENTITY);
		ChangeMessage clone = changeDAO.replaceChange(change);
	}
	@Test (expected=IllegalArgumentException.class)
	public void tesNullObjectTypeType(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId("syn123");
		change.setObjectEtag("myEtag");
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(null);
		ChangeMessage clone = changeDAO.replaceChange(change);
	}
	
	/**
	 * Etag can be null for deletes.
	 */
	@Test
	public void tesNullEtagForDelete(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId("syn223");
		change.setObjectEtag(null);
		change.setChangeType(ChangeType.DELETE);
		change.setObjectType(ObjectType.ENTITY);
		ChangeMessage clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		assertNull(clone.getObjectEtag());
	}
	
	/**
	 * Etag must not be null for create or update.
	 */
	@Test (expected=IllegalArgumentException.class)
	public void tesNullEtagForCreate(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId("syn334");
		change.setObjectEtag(null);
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(ObjectType.ENTITY);
		ChangeMessage clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		assertNull(clone.getObjectEtag());
	}
	
	/**
	 * Etag must not be null for create or update.
	 */
	@Test (expected=IllegalArgumentException.class)
	public void tesNullEtagForUpdate(){
		ChangeMessage change = new ChangeMessage();
		change.setObjectId("syn334");
		change.setObjectEtag(null);
		change.setChangeType(ChangeType.UPDATE);
		change.setObjectType(ObjectType.ENTITY);
		ChangeMessage clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		assertNull(clone.getObjectEtag());
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
			change.setObjectEtag("etag"+i);
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
		long startChangeNumber = changeDAO.getCurrentChangeNumber();
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
		assertEquals(startChangeNumber + numChangesInBatch , endChangeNumber);

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
			change.setObjectEtag("etag"+i);
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

	// TODO: PLFM-1631 for a load test framework outside this package
	// TODO: PLFM-1659 the test is failing occasionally for deadlocks on the primary index
	@Ignore
	public void testForDeadlocks() throws Exception {
		final int numOfTasks = 1000;
		final int numOfThreads = 10;
		// Create the list of tasks
		List<ReplaceChange> taskList = new ArrayList<ReplaceChange>(numOfTasks);
		for (int i = 0; i < 100; i++) {
			ChangeMessage change = new ChangeMessage();
			change.setObjectId("829165202913" + (i % 3)); // Simulate gap lock on the OBJECT_ID column
			change.setObjectType(ObjectType.ENTITY);
			change.setObjectEtag(Long.toString(System.currentTimeMillis()));
			change.setChangeType(ChangeType.UPDATE);
			change.setTimestamp(new Date());
			taskList.add(new ReplaceChange(change));
		}
		// Send the list of changes to a pool of threads
		ExecutorService exe = Executors.newFixedThreadPool(numOfThreads);
		try {
			List<Future<Boolean>> results = exe.invokeAll(taskList, 60, TimeUnit.SECONDS);
			for (Future<Boolean> future : results) {
				if (!future.get()) {
					Assert.fail("Deadlock detected.");
				}
			}
		} finally {
			exe.shutdown();
			exe.awaitTermination(60, TimeUnit.SECONDS);
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
				change.setObjectId("syn"+i);
			}else{
				change.setObjectId(""+i);
			}
			change.setObjectEtag("etag"+i);
			change.setChangeType(ChangeType.UPDATE);
			change.setObjectType(type);
			batch.add(change);
		}
		return batch;
	}

	private class ReplaceChange implements Callable<Boolean> {
		private final ChangeMessage change;
		private ReplaceChange(ChangeMessage change) {
			this.change = change;
		}
		@Override
		public Boolean call() throws Exception {
			try {
				changeDAO.replaceChange(change);
				return Boolean.TRUE;
			} catch (DeadlockLoserDataAccessException e) {
				return Boolean.FALSE;
			}
		}
	}
}
