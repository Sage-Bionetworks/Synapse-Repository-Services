package org.sagebionetworks.repo.model.dbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:dboExample-test-context.xml" })
public class DBOExampleTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	List<Long> toDelete;
	
	@Before
	public void before(){
		assertNotNull(dboBasicDao);
		toDelete = new LinkedList<Long>();
	}
	
	@After
	public void after(){
		if(dboBasicDao != null && toDelete != null){
			for(Long id: toDelete){
				try {
					MapSqlParameterSource params = new MapSqlParameterSource();
					params.addValue("id", id);
					dboBasicDao.deleteObjectByPrimaryKey(DBOExample.class, params);
				} catch (DatastoreException e) {}
			}
		}
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testInsertMissingValues() throws DatastoreException{
		DBOExample example = new DBOExample();
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
	}
	
	@Test
	public void testInsert() throws DatastoreException, UnsupportedEncodingException{
		DBOExample example = new DBOExample();
		example.setNumber(new Long(45));
		example.setModifiedBy("you");
		example.setModifiedOn(System.currentTimeMillis());
		example.setBlob("This string converts to a blob".getBytes("UTF-8"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		// This class is auto-increment so it should have an id now
		assertNotNull(example.getId());
	}
	
	@Test
	public void testInsertBatch() throws DatastoreException, UnsupportedEncodingException, NotFoundException{
		int batchSize = 6;
		List<DBOExample> batch = new ArrayList<DBOExample>();
		for(int i=0; i<batchSize; i++){
			DBOExample example = new DBOExample();
			example.setNumber(new Long(i));
			example.setModifiedBy("name"+i);
			example.setModifiedOn(System.currentTimeMillis());
			example.setBlob("This string converts to a blob".getBytes("UTF-8"));
			batch.add(example);
		}

		batch = dboBasicDao.createBatch(batch);
		// Make sure each has an id
		for(DBOExample created: batch){
			assertNotNull(created.getId());
			toDelete.add(created.getId());
			// Check the results
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", created.getId());
			DBOExample clone = dboBasicDao.getObjectByPrimaryKey(DBOExample.class, params);
			assertNotNull(clone);
			assertEquals(created, clone);
		}
	}
	
	@Test
	public void testGetById() throws Exception{
		DBOExample example = new DBOExample();
		example.setNumber(new Long(103));
		example.setModifiedBy("nobodyKnows");
		example.setModifiedOn(System.currentTimeMillis());
		example.setBlob("This string converts to a blob".getBytes("UTF-8"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		toDelete.add(example.getId());
		// This class is auto-increment so it should have an id now
		assertNotNull(example.getId());
		// Make sure we can get a clone
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", example.getId());
		DBOExample clone = dboBasicDao.getObjectByPrimaryKey(DBOExample.class, params);
		assertNotNull(clone);
		assertEquals(example, clone);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetByDoesNotExist() throws Exception{
		// This should fail with NotFoundException
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", new Long(33344));
		dboBasicDao.getObjectByPrimaryKey(DBOExample.class, params);
	}
	
	@Test
	public void testDeleteId() throws DatastoreException, UnsupportedEncodingException{
		DBOExample example = new DBOExample();
		example.setNumber(new Long(133));
		example.setModifiedBy("nobodyKnows");
		example.setModifiedOn(System.currentTimeMillis());
		example.setBlob("This string converts to a blob".getBytes("UTF-8"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		toDelete.add(example.getId());
		// This class is auto-increment so it should have an id now
		assertNotNull(example.getId());
		// delete it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", example.getId());
		boolean result = dboBasicDao.deleteObjectByPrimaryKey(DBOExample.class, params);
		assertTrue(result);
	}
	
	@Test
	public void testUpdate() throws Exception{
		// First create the object.
		DBOExample example = new DBOExample();
		example.setNumber(new Long(456));
		example.setModifiedBy("snoopy");
		example.setModifiedOn(System.currentTimeMillis());
		example.setBlob("This is the starting string".getBytes("UTF-8"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		toDelete.add(example.getId());
		// Get it back from the DB
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", example.getId());
		DBOExample fetched = dboBasicDao.getObjectByPrimaryKey(DBOExample.class, params);
		assertEquals(example, fetched);
		// Now change the value
		fetched.setBlob("I am the new string for the blob!".getBytes("UTF-8"));
		fetched.setModifiedOn(fetched.getModifiedOn()+10);
		fetched.setModifiedBy("Sombody else");
		// Now update it.
		boolean result = dboBasicDao.update(fetched);
		assertTrue(result);
		// Fetch it back
		DBOExample clone = dboBasicDao.getObjectByPrimaryKey(DBOExample.class, params);
		assertEquals(fetched, clone);
		
	}
	
	

}
