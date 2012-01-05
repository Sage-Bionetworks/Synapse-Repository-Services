package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBONodeTypeTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	private short id = 1000;
	
	@After
	public void after() throws DatastoreException{
		if(dboBasicDao != null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", id);
			dboBasicDao.deleteObjectById(DBONodeType.class, params);
		}
	}
	@Test
	public void testCRUD() throws Exception{
		// Create a new type
		DBONodeType nodeType = new DBONodeType();
		nodeType.setId(id);
		nodeType.setName("FakeType");
		
		// Create it
		DBONodeType clone = dboBasicDao.createNew(nodeType);
		assertNotNull(clone);
		assertEquals(nodeType, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", id);
		clone = dboBasicDao.getObjectById(DBONodeType.class, params);
		assertNotNull(clone);
		assertEquals(nodeType.getId(), clone.getId());
		assertEquals(nodeType.getName(), clone.getName());
		// Delete it
		boolean result = dboBasicDao.deleteObjectById(DBONodeType.class,  params);
		assertTrue("Failed to delete the type created", result);
		
	}

}
