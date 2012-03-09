package org.sagebionetworks.repo.model.dbo.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.dbo.DBOBasicDao;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUserProfileTest {
	
	@Autowired
	DBOBasicDao dboBasicDao;
	
	@Autowired 
	UserGroupDAO userGroupDAO;
	
	private static final String TEST_USER_NAME = "test-user";
	
	private UserGroup individualGroup = null;
	
	
	@Before
	public void setUp() throws Exception {
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup == null) {
			individualGroup = new UserGroup();
			individualGroup.setName(TEST_USER_NAME);
			individualGroup.setIndividual(true);
			individualGroup.setCreationDate(new Date());
			individualGroup.setId(userGroupDAO.create(individualGroup));
		}
		deleteUserProfile();
	}
	
	private void deleteUserProfile() throws DatastoreException {
		if(dboBasicDao != null && individualGroup!=null){
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("ownerId", individualGroup.getId());
			dboBasicDao.deleteObjectById(DBOUserProfile.class, params);
		}		
	}
		
	
	@After
	public void tearDown() throws Exception{
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup != null) {
			// this will delete the user profile too
			userGroupDAO.delete(individualGroup.getId());
		}
	}
	
	@Test
	public void testCRUD() throws Exception{
		// Create a new type
		DBOUserProfile userProfile = new DBOUserProfile();
		userProfile.setOwnerId(KeyFactory.stringToKey(individualGroup.getId()));
		userProfile.seteTag(10L);
		userProfile.setProperties("My dog has fleas.".getBytes());
		
		// Create it
		DBOUserProfile clone = dboBasicDao.createNew(userProfile);
		assertNotNull(clone);
		assertEquals(userProfile, clone);
		// Fetch it
		MapSqlParameterSource params = new MapSqlParameterSource();
		params.addValue("ownerId", individualGroup.getId());
		clone = dboBasicDao.getObjectById(DBOUserProfile.class, params);
		assertNotNull(clone);
		assertEquals(userProfile.getOwnerId(), clone.getOwnerId());
		// Delete it
		boolean result = dboBasicDao.deleteObjectById(DBOUserProfile.class,  params);
		assertTrue("Failed to delete the type created", result);
		
	}

}
