package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBONode;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.jdo.NodeTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOAnnotationsDaoImplTest {

	@Autowired
	private DBOAnnotationsDao dboAnnotationsDao;
	
	@Autowired
	private NodeDAO nodeDao;
	
	private DBONode node;
	
	@After
	public void after() throws Exception {
		nodeDao.delete(node.getId().toString());
	}
	
	@Before
	public void before() throws Exception {
		Long createdBy = BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER.getPrincipalId();
		
		// Create a node to create revisions of.
		Node dto = NodeTestUtils.createNew("DBOAnnotationsDaoImplTest.baseNode", createdBy, createdBy);
		String nodeId = nodeDao.createNew(dto);
		node = new DBONode();
		node.setId(KeyFactory.stringToKey(nodeId));
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
		annos.addAnnotation("keyNaN", Double.NaN);
		// Replace the annotations
		dboAnnotationsDao.replaceAnnotations(annos);
		// Now get them back
		Annotations clone = dboAnnotationsDao.getAnnotations(node.getId());
		assertNotNull(clone);
		assertEquals(new Double(123.1), clone.getSingleValue("keyOne"));
		assertEquals(new Double(345.2), clone.getSingleValue("keyTwo"));
		assertEquals("NaN", clone.getSingleValue("keyNaN"));
		
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
		
		LoopingAnnotationsWorker workerOne = new LoopingAnnotationsWorker(dboAnnotationsDao, 10, annos);
		LoopingAnnotationsWorker workerTwo = new LoopingAnnotationsWorker(dboAnnotationsDao, 10, annos);
		// Start both workers
		ExecutorService pool = Executors.newFixedThreadPool(2);
		Future<Boolean> furtureOne = pool.submit(workerOne);
		Future<Boolean> furtureTwo = pool.submit(workerTwo);
		// Wait for the threads to finish.

		assertTrue(furtureOne.get());
		assertTrue(furtureTwo.get());

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
	private static class LoopingAnnotationsWorker implements Callable<Boolean>{
		DBOAnnotationsDao dboAnnotationsDao;
		int count;
		Annotations annos;
		
		public LoopingAnnotationsWorker(DBOAnnotationsDao dboAnnotationsDao,
				int count, Annotations annos) {
			super();
			this.dboAnnotationsDao = dboAnnotationsDao;
			this.count = count;
			this.annos = annos;
		}

		@Override
		public Boolean call() throws Exception {
			// Attempt to update the annotations 10 times
			int count = 10;
			int deadlockCount = 0;
			while (count > 0) {
				assertTrue("Too many deadlock exceptions", deadlockCount < 10);
				try {
					// Replace the annotations
					dboAnnotationsDao.replaceAnnotations(annos);
				} catch (Exception e) {
					// Deadlock is the only acceptable exception here, just retry
					if (e.getMessage().indexOf("Deadlock found when trying to get lock") > -1) {
						deadlockCount++;
						continue;
					} else {
						throw e;
					}
				}
				count--;
			}
			return true;
		}
	}
}
