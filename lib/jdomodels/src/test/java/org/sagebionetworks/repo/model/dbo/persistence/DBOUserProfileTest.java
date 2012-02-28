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
public class DBOUserProfileTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	private long id = 1000;
	
	@After
	public void after() throws DatastoreException{
		if(dboBasicDao != null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("id", id);
			dboBasicDao.deleteObjectById(DBOUserProfile.class, params);
		}
	}
	@Test
	public void testCRUD() throws Exception{
		// Create a new type
		DBOUserProfile userProfile = new DBOUserProfile();
		userProfile.setId(id);
		userProfile.setUserName("foo@bar.bas");
		
		// Create it
		DBOUserProfile clone = dboBasicDao.createNew(userProfile);
		assertNotNull(clone);
		assertEquals(userProfile, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("id", id);
		clone = dboBasicDao.getObjectById(DBOUserProfile.class, params);
		assertNotNull(clone);
		assertEquals(userProfile.getId(), clone.getId());
		assertEquals(userProfile.getUserName(), clone.getUserName());
		// Delete it
		boolean result = dboBasicDao.deleteObjectById(DBOUserProfile.class,  params);
		assertTrue("Failed to delete the type created", result);
		
	}

}
