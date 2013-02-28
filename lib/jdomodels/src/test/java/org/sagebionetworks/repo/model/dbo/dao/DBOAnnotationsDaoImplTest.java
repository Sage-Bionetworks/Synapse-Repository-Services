package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.sql.BatchUpdateException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.ETagGenerator;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAnnotationsDaoImplTest {

	@Autowired
	DBOAnnotationsDao dboAnnotationsDao;
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired
	UserGroupDAO userGroupDAO;
	
	@Autowired
	private IdGenerator idGenerator;
	@Autowired
	private ETagGenerator eTagGenerator;
	
	List<Long> toDelete = null;
	
	DBONode node;
	
	@After
	public void after() throws DatastoreException {
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				MapSqlParameterSource params = new MapSqlParameterSource();
				params.addValue("id", id);
				dboBasicDao.deleteObjectById(DBONode.class, params);
				dboAnnotationsDao.deleteAnnotationsByOwnerId(id);
			}
		}
	}
	
	@Before
	public void before() throws DatastoreException, UnsupportedEncodingException{
		String createdBy = userGroupDAO.findGroup(AuthorizationConstants.BOOTSTRAP_USER_GROUP_NAME, false).getId();
		toDelete = new LinkedList<Long>();
		// Create a node to create revisions of.
		node = new DBONode();
		node.setId(idGenerator.generateNewId());
		toDelete.add(node.getId());
		node.setBenefactorId(node.getId());
		node.setCreatedBy(Long.parseLong(createdBy));
		node.setCreatedOn(System.currentTimeMillis());
		node.setCurrentRevNumber(null);
		node.setDescription("A basic description".getBytes("UTF-8"));
		node.seteTag(eTagGenerator.generateETag());
		node.setName("DBOAnnotationsDaoImplTest.baseNode");
		node.setParentId(null);
		node.setNodeType(EntityType.project.getId());
		dboBasicDao.createNew(node);
	}
	
	@Test
	public void testStringAnnotations() throws DatastoreException{
		Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyOne", "valueOne");
		annos.addAnnotation("keyTwo", "valueTwo");
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals("valueOne", clone.getSingleValue("keyOne"));
		assertEquals("valueTwo", clone.getSingleValue("keyTwo"));
		
		// Make sure we can replace them
		annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyTwo", "valueTwo");
		annos.addAnnotation("keyThree", "valueThree");
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		// this was removed.
		assertEquals(null, clone.getSingleValue("keyOne"));
		assertEquals("valueTwo", clone.getSingleValue("keyTwo"));
		assertEquals("valueThree", clone.getSingleValue("keyThree"));

		// Remove the annotations associated with the node
		dboAnnotationsDao.deleteAnnotationsByOwnerId(node.getId());
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertNull(clone.getSingleValue("keyOne"));
		assertNull(clone.getSingleValue("keyTwo"));
		assertNull(clone.getSingleValue("keyThree"));
	}
	
	@Test
	public void testLongAnnotations() throws DatastoreException{
		Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyOne", new Long(123));
		annos.addAnnotation("keyTwo", new Long(345));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals(new Long(123), clone.getSingleValue("keyOne"));
		assertEquals(new Long(345), clone.getSingleValue("keyTwo"));
		
		// Make sure we can replace them
		annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyTwo", new Long(345));
		annos.addAnnotation("keyThree", new Long(789));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		// this was removed.
		assertEquals(null, clone.getSingleValue("keyOne"));
		assertEquals(new Long(345), clone.getSingleValue("keyTwo"));
		assertEquals(new Long(789), clone.getSingleValue("keyThree"));

		// Remove the annotations associated with the node
		dboAnnotationsDao.deleteAnnotationsByOwnerId(node.getId());
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertNull(clone.getSingleValue("keyOne"));
		assertNull(clone.getSingleValue("keyTwo"));
		assertNull(clone.getSingleValue("keyThree"));
	}
	
	@Test
	public void testDoubleAnnotations() throws DatastoreException{
		Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyOne", new Double(123.1));
		annos.addAnnotation("keyTwo", new Double(345.2));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals(new Double(123.1), clone.getSingleValue("keyOne"));
		assertEquals(new Double(345.2), clone.getSingleValue("keyTwo"));
		
		// Make sure we can replace them
		annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyTwo", new Double(345.2));
		annos.addAnnotation("keyThree", new Double(789.3));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		// this was removed.
		assertEquals(null, clone.getSingleValue("keyOne"));
		assertEquals(new Double(345.2), clone.getSingleValue("keyTwo"));
		assertEquals(new Double(789.3), clone.getSingleValue("keyThree"));

		// Remove the annotations associated with the node
		dboAnnotationsDao.deleteAnnotationsByOwnerId(node.getId());
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertNull(clone.getSingleValue("keyOne"));
		assertNull(clone.getSingleValue("keyTwo"));
		assertNull(clone.getSingleValue("keyThree"));
	}
	
	@Test
	public void testDateAnnotations() throws DatastoreException{
		Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyOne", new Date(1000));
		annos.addAnnotation("keyTwo", new Date(2000));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals(new Date(1000), clone.getSingleValue("keyOne"));
		assertEquals(new Date(2000), clone.getSingleValue("keyTwo"));
		
		// Make sure we can replace them
		annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("keyTwo", new Date(2000));
		annos.addAnnotation("keyThree", new Date(3000));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		// this was removed.
		assertEquals(null, clone.getSingleValue("keyOne"));
		assertEquals(new Date(2000), clone.getSingleValue("keyTwo"));
		assertEquals(new Date(3000), clone.getSingleValue("keyThree"));

		// Remove the annotations associated with the node
		dboAnnotationsDao.deleteAnnotationsByOwnerId(node.getId());
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertNull(clone.getSingleValue("keyOne"));
		assertNull(clone.getSingleValue("keyTwo"));
		assertNull(clone.getSingleValue("keyThree"));
	}
	
	@Test
	public void testOneOfEachAnnotations() throws DatastoreException{
		Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("stringKey", "String");
		annos.addAnnotation("longKey", new Long(123));
		annos.addAnnotation("doubleKey", new Double(1.11));
		annos.addAnnotation("dateKey", new Date(2000));
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals("String", clone.getSingleValue("stringKey"));
		assertEquals(new Long(123), clone.getSingleValue("longKey"));
		assertEquals(new Double(1.11), clone.getSingleValue("doubleKey"));
		assertEquals(new Date(2000), clone.getSingleValue("dateKey"));
		// Remove the annotations associated with the node
		dboAnnotationsDao.deleteAnnotationsByOwnerId(node.getId());
		clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertNull(clone.getSingleValue("stringKey"));
		assertNull(clone.getSingleValue("longKey"));
		assertNull(clone.getSingleValue("doubleKey"));
		assertNull(clone.getSingleValue("dateKey"));
	}
	
	@Test
	public void testDistict(){
		final Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("stringKey", "one");
		annos.addAnnotation("stringKey", "one");
		try{
			// This should fail due to a uniqueness constraint violation.
			dboAnnotationsDao.replaceAnnotations(annos);
			fail("should have failed");
		}catch(Exception e){
			assertTrue(e.getMessage().indexOf("Duplicate entry") > 0);
			assertTrue(e.getMessage().indexOf("for key 'STRING_ANNO_UNIQUE'") > 0);
		}
	}
	
	/**
	 * This test was added as part of the fix for PLFM-1696
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	@Test
	public void testConcurentUpdate() throws InterruptedException, ExecutionException{
		final Annotations annos = new Annotations();
		// Apply these annos to this node.
		annos.setId(KeyFactory.keyToString(node.getId()));
		annos.addAnnotation("stringKey", "String");
		
		LoopingAnnotaionsWoker workerOne = new LoopingAnnotaionsWoker(dboAnnotationsDao, 10, annos);
		LoopingAnnotaionsWoker workerTwo = new LoopingAnnotaionsWoker(dboAnnotationsDao, 10, annos);
		// Start both workers
		ExecutorService pool = Executors.newFixedThreadPool(2);
		Future<Boolean> furtureOne = pool.submit(workerOne);
		Future<Boolean> furtureTwo = pool.submit(workerTwo);
		// Wait for the threads to finish.
		try{
			assertTrue(furtureOne.get());
			assertTrue(furtureTwo.get());
		}catch(ExecutionException e){
			System.out.println(e.getLocalizedMessage());
			// Deadlock is an acceptable out for this test.
			assertTrue("Deadlock is the only acceptable exception for this test.",e.getMessage().indexOf("Deadlock found when trying to get lock" )> -1);
		}	
		// There should be no duplication.
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		// There should be no duplication.
		assertNotNull(clone);
		Collection list = clone.getAllValues("stringKey");
		assertNotNull(list);
		assertEquals("There should only be one value for this annotations. That means multiple threads caused duplication!", 1, list.size());
		assertEquals("String", list.iterator().next() );
	}
	
	/**
	 * This is a simple worker that will attempt to update the same annotations over and over 
	 * again in a loop.
	 * @author John
	 *
	 */
	private static class LoopingAnnotaionsWoker implements Callable<Boolean>{
		DBOAnnotationsDao dboAnnotationsDao;
		int count;
		Annotations annos;
		
		public LoopingAnnotaionsWoker(DBOAnnotationsDao dboAnnotationsDao,
				int count, Annotations annos) {
			super();
			this.dboAnnotationsDao = dboAnnotationsDao;
			this.count = count;
			this.annos = annos;
		}

		@Override
		public Boolean call() throws Exception {
			// Attempt to update the annotations 10 times
			for(int i=0; i<count; i++){
				// Replace the annotations
				dboAnnotationsDao.replaceAnnotations(annos);
			}
			return true;
		}
		
	}
	
}
