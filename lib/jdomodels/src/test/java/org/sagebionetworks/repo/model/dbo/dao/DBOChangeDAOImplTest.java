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
import org.sagebionetworks.repo.model.dbo.persistence.DBOChange;
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
		DBOChange change = new DBOChange();
		change.setObjectId(123l);
		change.setObjectEtag("myEtag");
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(ObjectType.ENTITY);
		DBOChange clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		System.out.println(clone);
		assertNotNull(clone.getChangeNumber());
		long firstChangeNumber = clone.getChangeNumber();
		assertEquals(change.getObjectId(), clone.getObjectId());
		assertEquals(change.getObjectEtag(), clone.getObjectEtag());
		assertEquals(change.getChangeTypeEnum(), clone.getChangeTypeEnum());
		assertEquals(change.getObjectTypeEnum(), clone.getObjectTypeEnum());
		// Now replace it again
		clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		long secondChangeNumber = clone.getChangeNumber();
		System.out.println(clone);
		assertTrue(secondChangeNumber > firstChangeNumber);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void tesNullId(){
		DBOChange change = new DBOChange();
		change.setObjectId(null);
		change.setObjectEtag("myEtag");
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(ObjectType.ENTITY);
		DBOChange clone = changeDAO.replaceChange(change);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void tesNullChangeType(){
		DBOChange change = new DBOChange();
		change.setObjectId(334L);
		change.setObjectEtag("myEtag");
		change.setChangeType(null);
		change.setObjectType(ObjectType.ENTITY);
		DBOChange clone = changeDAO.replaceChange(change);
	}
	@Test (expected=IllegalArgumentException.class)
	public void tesNullObjectTypeType(){
		DBOChange change = new DBOChange();
		change.setObjectId(334L);
		change.setObjectEtag("myEtag");
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(null);
		DBOChange clone = changeDAO.replaceChange(change);
	}
	
	/**
	 * Etag can be null for deletes.
	 */
	@Test
	public void tesNullEtagForDelete(){
		DBOChange change = new DBOChange();
		change.setObjectId(334L);
		change.setObjectEtag(null);
		change.setChangeType(ChangeType.DELETE);
		change.setObjectType(ObjectType.ENTITY);
		DBOChange clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		assertNull(clone.getObjectEtag());
	}
	
	/**
	 * Etag must not be null for create or update.
	 */
	@Test (expected=IllegalArgumentException.class)
	public void tesNullEtagForCreate(){
		DBOChange change = new DBOChange();
		change.setObjectId(334L);
		change.setObjectEtag(null);
		change.setChangeType(ChangeType.CREATE);
		change.setObjectType(ObjectType.ENTITY);
		DBOChange clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		assertNull(clone.getObjectEtag());
	}
	
	/**
	 * Etag must not be null for create or update.
	 */
	@Test (expected=IllegalArgumentException.class)
	public void tesNullEtagForUpdate(){
		DBOChange change = new DBOChange();
		change.setObjectId(334L);
		change.setObjectEtag(null);
		change.setChangeType(ChangeType.UPDATE);
		change.setObjectType(ObjectType.ENTITY);
		DBOChange clone = changeDAO.replaceChange(change);
		assertNotNull(clone);
		assertNull(clone.getObjectEtag());
	}
	
	@Test
	public void testSortByObjectId(){
		List<DBOChange> batch = new ArrayList<DBOChange>();
		for(int i=0; i<5; i++){
			DBOChange change = new DBOChange();
			change.setObjectId(new Long(i));
			change.setObjectEtag("etag"+i);
			change.setChangeType(ChangeType.UPDATE);
			change.setObjectType(ObjectType.ENTITY);
			batch.add(change);
		}
		// Start shuffled
		Collections.shuffle(batch);
		// Now sort
		batch = DBOChange.sortByObjectId(batch);
		assertNotNull(batch);
		long previous = -1;
		for(DBOChange change: batch){
			assertTrue(change.getObjectId() > previous);
			previous = change.getObjectId();
		}
	}
	
	@Test
	public void testSortByChangeNumber(){
		List<DBOChange> batch = new ArrayList<DBOChange>();
		for(int i=0; i<5; i++){
			DBOChange change = new DBOChange();
			change.setObjectId(0l);
			change.setChangeNumber(new Long(i*2));
			change.setObjectEtag("etag"+i);
			change.setChangeType(ChangeType.UPDATE);
			change.setObjectType(ObjectType.ENTITY);
			batch.add(change);
		}
		// Start shuffled
		Collections.shuffle(batch);
		// Now sort
		batch = DBOChange.sortByChangeNumber(batch);
		assertNotNull(batch);
		long previous = -1;
		for(DBOChange change: batch){
			assertTrue(change.getChangeNumber() > previous);
			previous = change.getChangeNumber();
		}
	}
	
	@Test
	public void testReplaceBatch(){
		// Get the current change number
		int numChangesInBatch = 5;
		long startChangeNumber = changeDAO.getCurrentChangeNumber();
		List<DBOChange> batch = new ArrayList<DBOChange>();
		for(int i=0; i<numChangesInBatch; i++){
			DBOChange change = new DBOChange();
			change.setObjectId(new Long(i));
			change.setObjectEtag("etag"+i);
			change.setChangeType(ChangeType.UPDATE);
			change.setObjectType(ObjectType.ENTITY);
			batch.add(change);
		}
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
		List<DBOChange> byChangeNumber = new LinkedList<DBOChange>(batch);
		byChangeNumber = DBOChange.sortByChangeNumber(byChangeNumber);
		// by object id
		List<DBOChange> byObjectId = new LinkedList<DBOChange>(batch);
		byObjectId = DBOChange.sortByObjectId(byObjectId);
		assertEquals("If the batch was sorted by objectID before replacing then the change number should be in the same order as the object ids", byChangeNumber, byObjectId);
		
		// Check the change numbers
		long endChangeNumber = changeDAO.getCurrentChangeNumber();
		assertEquals(startChangeNumber + numChangesInBatch , endChangeNumber);

	}

}
