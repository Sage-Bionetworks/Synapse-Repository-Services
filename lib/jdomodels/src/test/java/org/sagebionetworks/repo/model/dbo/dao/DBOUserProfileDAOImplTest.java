package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.ids.UuidETagGenerator;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.SchemaCache;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserGroupInt;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.beans.factory.annotation.Autowired;
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
			individualGroup.setIsIndividual(true);
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
		userProfile.setEtag(UuidETagGenerator.ZERO_E_TAG);
		
		long initialCount = userProfileDAO.getCount();
		// Create it
		String id = userProfileDAO.create(userProfile, schema);
		assertNotNull(id);
		
		assertEquals(1+initialCount, userProfileDAO.getCount());
		
		// Fetch it
		UserProfile clone = userProfileDAO.get(id, schema);
		assertNotNull(clone);
		assertEquals(userProfile, clone);

		// Update it
		clone.setDisplayName("Mr. Foo Bar");
		UserProfile updatedProfile = userProfileDAO.update(clone, schema);
		assertEquals(clone.getDisplayName(), updatedProfile.getDisplayName());
		assertTrue("etags should be different after an update", !clone.getEtag().equals(updatedProfile.getEtag()));

		try {
			clone.setDisplayName("This Should Fail");
			userProfileDAO.update(clone, schema);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e) {
			// We expected this exception
		}

		try {
			// Update from a backup.
			updatedProfile = userProfileDAO.updateFromBackup(clone, schema);
			assertEquals(clone.getEtag(), updatedProfile.getEtag());
		}
		catch(ConflictingUpdateException e) {
			fail("Update from backup should not generate exception even if the e-tag is different.");
		}

		// Delete it
		userProfileDAO.delete(id);

		assertEquals(initialCount, userProfileDAO.getCount());
	}
	
	@Test
	public void testBootstrapUsers() throws DatastoreException, NotFoundException{
		List<UserGroupInt> boots = this.userGroupDAO.getBootstrapUsers();
		assertNotNull(boots);
		assertTrue(boots.size() >0);
		// Each should exist
		ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
		for(UserGroupInt bootUg: boots){
			if(bootUg.getIsIndividual()){
				UserProfile profile = userProfileDAO.get(bootUg.getId(), schema);
				UserGroup ug = userGroupDAO.get(bootUg.getId());
				assertEquals(bootUg.getId(), profile.getOwnerId());
			}
		}
	}
}
