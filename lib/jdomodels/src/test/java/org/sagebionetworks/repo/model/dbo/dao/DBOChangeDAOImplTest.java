package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeMessageUtils;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOChangeDAOImplTest {
	
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

}
