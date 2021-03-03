package org.sagebionetworks.repo.model.dbo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOAnnotatedExample.ExampleEnum;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.Lists;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:dboExample-test-context.xml" })
public class DBOAnnotatedExampleTest {
	
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
					dboBasicDao.deleteObjectByPrimaryKey(DBOAnnotatedExample.class, params);
				} catch (DatastoreException e) {}
			}
		}
	}

	@Test
	public void ddlTheSame() throws Exception{
		String annotatedDll = ((AutoTableMapping<DBOAnnotatedExample>)new DBOAnnotatedExample().getTableMapping()).getDDL();
		String fileDll = DDLUtilsImpl.loadSQLFromClasspath("AnnotatedExample.sql");
		assertEquals(fileDll.trim().replace("\r\n", "\n"), annotatedDll.trim());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testInsertMissingValues() throws DatastoreException{
		DBOAnnotatedExample example = new DBOAnnotatedExample();
		example.setModifiedOn(new Date());
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testInsertMissingValuesNullPointerException() throws DatastoreException {
		DBOAnnotatedExample example = new DBOAnnotatedExample();
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
	}

	@Test
	public void testInsert() throws Exception {
		DBOAnnotatedExample example = new DBOAnnotatedExample();
		example.setNumber(new Long(45));
		example.setNumberOrNull(new Long(46));
		example.setModifiedBy("you");
		// dates don't compare because of rounding
		example.setModifiedOn(new Date());
		example.setBlob("This string converts to a blob".getBytes("UTF-8"));
		example.setComment("no comment");
		example.setName("the name");
		example.setExampleEnum(ExampleEnum.bbb);
		example.setSerialized(Lists.newArrayList("aa", "bb"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		// This class is auto-increment so it should have an id now
		assertNotNull(example.getId());

		DBOAnnotatedExample clone = dboBasicDao.getObjectByPrimaryKey(DBOAnnotatedExample.class, new SinglePrimaryKeySqlParameterSource(
				example.getId()));
		assertEquals(example.toString(), clone.toString());
	}

	@Test
	public void testInsertNull() throws DatastoreException, UnsupportedEncodingException {
		DBOAnnotatedExample example = new DBOAnnotatedExample();
		example.setNumber(new Long(45));
		example.setNumberOrNull(null);
		example.setModifiedBy("you");
		// dates don't compare because of rounding
		example.setModifiedOn(new Date());
		example.setBlob("This string converts to a blob".getBytes("UTF-8"));
		example.setComment("no comment");
		example.setName("the name");
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		// This class is auto-increment so it should have an id now
		assertNotNull(example.getId());
	}

	@Test
	public void testInsertBatch() throws DatastoreException, UnsupportedEncodingException, NotFoundException{
		int batchSize = 6;
		List<DBOAnnotatedExample> batch = new ArrayList<DBOAnnotatedExample>();
		for(int i=0; i<batchSize; i++){
			DBOAnnotatedExample example = new DBOAnnotatedExample();
			example.setNumber(new Long(i));
			example.setModifiedBy("name"+i);
			// dates don't compare because of rounding
			example.setModifiedOn(new Date());
			example.setBlob("This string converts to a blob".getBytes("UTF-8"));
			batch.add(example);
		}

		batch = dboBasicDao.createBatch(batch);
		// Make sure each has an id
		for (DBOAnnotatedExample created : batch) {
			assertNotNull(created.getId());
			toDelete.add(created.getId());
			// Check the results
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", created.getId());
			DBOAnnotatedExample clone = dboBasicDao.getObjectByPrimaryKey(DBOAnnotatedExample.class, params);
			assertNotNull(clone);
			assertEquals(created, clone);
		}
	}
	
	@Test
	public void testGetById() throws Exception{
		DBOAnnotatedExample example = new DBOAnnotatedExample();
		example.setNumber(new Long(103));
		example.setModifiedBy("nobodyKnows");
		// dates don't compare because of rounding
		example.setModifiedOn(new Date());
		example.setBlob("This string converts to a blob".getBytes("UTF-8"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		toDelete.add(example.getId());
		// This class is auto-increment so it should have an id now
		assertNotNull(example.getId());
		// Make sure we can get a clone
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", example.getId());
		DBOAnnotatedExample clone = dboBasicDao.getObjectByPrimaryKey(DBOAnnotatedExample.class, params);
		assertNotNull(clone);
		assertEquals(example, clone);
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetByDoesNotExist() throws Exception{
		// This should fail with NotFoundException
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", new Long(33344));
		dboBasicDao.getObjectByPrimaryKey(DBOAnnotatedExample.class, params);
	}
	
	@Test
	public void testDeleteId() throws DatastoreException, UnsupportedEncodingException{
		DBOAnnotatedExample example = new DBOAnnotatedExample();
		example.setNumber(new Long(133));
		example.setModifiedBy("nobodyKnows");
		// dates don't compare because of rounding
		example.setModifiedOn(new Date());
		example.setBlob("This string converts to a blob".getBytes("UTF-8"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		toDelete.add(example.getId());
		// This class is auto-increment so it should have an id now
		assertNotNull(example.getId());
		// delete it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", example.getId());
		boolean result = dboBasicDao.deleteObjectByPrimaryKey(DBOAnnotatedExample.class, params);
		assertTrue(result);
	}
	
	@Test
	public void testUpdate() throws Exception{
		// First create the object.
		DBOAnnotatedExample example = new DBOAnnotatedExample();
		example.setNumber(new Long(456));
		example.setModifiedBy("snoopy");
		// dates don't compare because of rounding
		example.setModifiedOn(new Date());
		example.setBlob("This is the starting string".getBytes("UTF-8"));
		example = dboBasicDao.createNew(example);
		assertNotNull(example);
		toDelete.add(example.getId());
		// Get it back from the DB
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", example.getId());
		DBOAnnotatedExample fetched = dboBasicDao.getObjectByPrimaryKey(DBOAnnotatedExample.class, params);
		assertEquals(example, fetched);
		// Now change the value
		fetched.setBlob("I am the new string for the blob!".getBytes("UTF-8"));
		fetched.setModifiedOn(new Date(fetched.getModifiedOn().getTime() + 10));
		fetched.setModifiedBy("Sombody else");
		// Now update it.
		boolean result = dboBasicDao.update(fetched);
		assertTrue(result);
		// Fetch it back
		DBOAnnotatedExample clone = dboBasicDao.getObjectByPrimaryKey(DBOAnnotatedExample.class, params);
		assertEquals(fetched, clone);
		
	}
	
	@Test
	public void testSelfForeignKey() throws Exception {
		// First create the object.
		DBOAnnotatedExample example = new DBOAnnotatedExample();
		
		TableMapping<DBOAnnotatedExample> tm = example.getTableMapping();
		FieldColumn[] columns = tm.getFieldColumns();
		
		// PARENT_ID column should have isSelfForeignKey annotation set to true
		// and rest should be set to false
		for (FieldColumn column : columns) {
			if (column.getColumnName().equals("PARENT_ID")) {
				assertTrue(column.isSelfForeignKey());
			} else {
				assertFalse(column.isSelfForeignKey());
			}
		}
	}

	@Test
	public void testHasFileHandleRef() throws Exception {
		// First create the object.
		DBOAnnotatedExample example = new DBOAnnotatedExample();
		
		TableMapping<DBOAnnotatedExample> tm = example.getTableMapping();
		FieldColumn[] columns = tm.getFieldColumns();

		for (FieldColumn column : columns) {
			if (column.getColumnName().equals("FILE_HANDLE")) {
				assertTrue(column.hasFileHandleRef());
			} else {
				assertFalse(column.hasFileHandleRef());
			}
		}
	}

	
}
