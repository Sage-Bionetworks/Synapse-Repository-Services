package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
			dboBasicDao.deleteObjectByPrimaryKey(DBONodeType.class, params);
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
		clone = dboBasicDao.getObjectByPrimaryKey(DBONodeType.class, params);
		assertNotNull(clone);
		assertEquals(nodeType.getId(), clone.getId());
		assertEquals(nodeType.getName(), clone.getName());
		// Delete it
		boolean result = dboBasicDao.deleteObjectByPrimaryKey(DBONodeType.class,  params);
		assertTrue("Failed to delete the type created", result);
		
	}
	
	@Test
	public void testAliasCRUD() throws Exception{
		// Create a new type
		DBONodeType nodeType = new DBONodeType();
		nodeType.setId(id);
		nodeType.setName("FakeType");
		// Create it
		DBONodeType clone = dboBasicDao.createNew(nodeType);
		assertNotNull(clone);
		assertEquals(nodeType, clone);
		// Add some aliases
		for(int i=0; i<12; i++){
			DBONodeTypeAlias alias = new DBONodeTypeAlias();
			alias.setTypeOwner(nodeType.getId());
			String aliasValue = "alias-"+i;
			alias.setAlias(aliasValue);
			dboBasicDao.createNew(alias);
			// Make sure we can fetch it.
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("typeOwner", id);
			params.addValue("alias", aliasValue);
			DBONodeTypeAlias aliasClone = dboBasicDao.getObjectByPrimaryKey(DBONodeTypeAlias.class, params);
			assertEquals(alias, aliasClone);
		}
		
	}

}
