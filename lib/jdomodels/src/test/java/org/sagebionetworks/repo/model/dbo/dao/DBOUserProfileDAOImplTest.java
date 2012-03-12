package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class DBOUserProfileDAOImplTest {

	@Autowired 
	UserGroupDAO userGroupDAO;
	
	@Autowired
	UserProfileDAO userProfileDAO;
	
	private static final String TEST_USER_NAME = "test-user";
	
	private UserGroup individualGroup = null;
	
	private ObjectSchema schema = null;
	
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
		String jsonString = (String) UserProfile.class.getField(JSONEntity.EFFECTIVE_SCHEMA).get(null);
		schema = new ObjectSchema(new JSONObjectAdapterImpl(jsonString));

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
		UserProfile userProfile = new UserProfile();
		userProfile.setOwnerId(individualGroup.getId());
		userProfile.setFirstName("foo");
		userProfile.setLastName("bar");
		userProfile.setRStudioUrl("http://rstudio.com");
		userProfile.setDisplayName("foo bar");
		userProfile.setEtag("0");
		
		// Create it
		String id = userProfileDAO.create(userProfile, schema);
		assertNotNull(id);
		// Fetch it
		UserProfile clone = userProfileDAO.get(id, schema);
		assertNotNull(clone);
		assertEquals(userProfile, clone);
		
		// update it
		userProfileDAO.update(userProfile, schema);
		
		// Delete it
		userProfileDAO.delete(id);
	}


}
