package org.sagebionetworks.repo.model.dbo.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserPreference;
import org.sagebionetworks.repo.model.UserPreferenceBoolean;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.principal.BootstrapPrincipal;
import org.sagebionetworks.repo.model.principal.BootstrapUser;
import org.sagebionetworks.repo.web.NotFoundException;
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
	
	private UserGroup individualGroup = null;
	
	@Before
	public void setUp() throws Exception {
		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		individualGroup.setId(userGroupDAO.create(individualGroup).toString());
	}
		
	
	@After
	public void tearDown() throws Exception{
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
		userProfile.setEtag(NodeConstants.ZERO_E_TAG);
		Map<String, UserPreference> prefs = new HashMap<String, UserPreference>();
		UserPreferenceBoolean pref = new UserPreferenceBoolean();
		pref.setValue(true);
		prefs.put("aKey", pref);
		userProfile.setPreferences(prefs);
		
		long initialCount = userProfileDAO.getCount();
		// Create it
		String id = userProfileDAO.create(userProfile);
		assertNotNull(id);
		
		assertEquals(1+initialCount, userProfileDAO.getCount());
		
		// Fetch it
		UserProfile clone = userProfileDAO.get(id);
		assertNotNull(clone);
		assertNotNull(clone.getPreferences());
		assertTrue(clone.getPreferences().containsKey("aKey"));
		assertEquals(UserPreferenceBoolean.class, clone.getPreferences().get("aKey").getClass());
		UserPreferenceBoolean v = (UserPreferenceBoolean)clone.getPreferences().get("aKey");
		assertEquals(true, v.getValue());
		assertEquals(userProfile, clone);

		// Update it
		UserProfile updatedProfile = userProfileDAO.update(clone);
		assertTrue("etags should be different after an update", !clone.getEtag().equals(updatedProfile.getEtag()));

		try {
			clone.setFirstName("This Should Fail");
			userProfileDAO.update(clone);
			fail("conflicting update exception not thrown");
		}
		catch(ConflictingUpdateException e) {
			// We expected this exception
		}

		// Delete it
		userProfileDAO.delete(id);

		assertEquals(initialCount, userProfileDAO.getCount());
	}
	
	@Test
	public void testBootstrapUsers() throws DatastoreException, NotFoundException{
		List<BootstrapPrincipal> boots = this.userGroupDAO.getBootstrapPrincipals();
		assertNotNull(boots);
		assertTrue(boots.size() >0);
		// Each should exist
		for(BootstrapPrincipal bootUg: boots){
			if(bootUg instanceof BootstrapUser){
				UserProfile profile = userProfileDAO.get(bootUg.getId().toString());
				userGroupDAO.get(bootUg.getId());
				assertEquals(bootUg.getId().toString(), profile.getOwnerId());
			}
		}
	}
}
