package org.sagebionetworks.repo.manager;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:manager-test-context.xml" })
public class UserProfileManagerImplTest {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	private static final String TEST_USER_NAME = "test-user";
	private static final String TEST_USER_DISPLAY_NAME = "test-user display-name";
	
	private UserGroup individualGroup = null;
	private UserProfile userProfile = null;
	
	
	@Before
	public void setUp() throws Exception {
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup == null) {
			individualGroup = new UserGroup();
			individualGroup.setName(TEST_USER_NAME);
			individualGroup.setIsIndividual(true);
			individualGroup.setCreationDate(new Date());
			individualGroup.setId(userGroupDAO.create(individualGroup));
			// we also make an user profile for this individual
			ObjectSchema schema = SchemaCache.getSchema(UserProfile.class);
			try {
				userProfile = userProfileDAO.get(individualGroup.getId(), schema);
			} catch (NotFoundException nfe) {
				userProfile = null;
			}
			if (userProfile==null) {
				userProfile = new UserProfile();
				userProfile.setOwnerId(individualGroup.getId());
				userProfile.setUserName(TEST_USER_NAME);
				userProfile.setDisplayName(TEST_USER_DISPLAY_NAME);
				String id = userProfileDAO.create(userProfile, schema);
				userProfile = userProfileDAO.get(id, schema);
			}
		}
	}

	@After
	public void tearDown() throws Exception {
		UserGroup individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		userGroupDAO.delete(individualGroup.getId());
		individualGroup = null;
		userProfile = null;
	}
	
	@Test
	public void testGetOwnUserProfle() throws Exception {
		assertNotNull(individualGroup);
		assertNotNull(userProfile);
		UserInfo userInfo = new UserInfo(false); // not an admin
		userInfo.setIndividualGroup(individualGroup);
		String ownerId = userProfile.getOwnerId();
		assertEquals(ownerId, individualGroup.getId());
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertEquals(userProfile, upClone);
	}
	
	@Test
	public void testgetOthersUserProfle() throws Exception {
		assertNotNull(individualGroup);
		assertNotNull(userProfile);
		UserInfo userInfo = new UserInfo(false); // not an admin
		UserGroup otherIndividualGroup = new UserGroup();
		otherIndividualGroup.setId("-100");
		userInfo.setIndividualGroup(otherIndividualGroup);
		String ownerId = userProfile.getOwnerId();
		assertEquals(ownerId, individualGroup.getId());
		// there will be missing fields, intentionally 'blanked out'
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertFalse(userProfile.equals(upClone));
		assertNull(upClone.getUserName());
		assertEquals(userProfile.getDisplayName(), upClone.getDisplayName());
	}
	
	@Test
	public void testgetOthersUserProfleByAdmin() throws Exception {
		assertNotNull(individualGroup);
		assertNotNull(userProfile);
		UserInfo userInfo = new UserInfo(true); // IS an admin
		UserGroup otherIndividualGroup = new UserGroup();
		otherIndividualGroup.setId("-100");
		userInfo.setIndividualGroup(otherIndividualGroup);
		String ownerId = userProfile.getOwnerId();
		assertEquals(ownerId, individualGroup.getId());

		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertEquals(userProfile, upClone);
	}
	
	private Random rand = new Random();
	
	@Test
	public void testUpdateOwnUserProfle() throws Exception {
		assertNotNull(individualGroup);
		assertNotNull(userProfile);
		UserInfo userInfo = new UserInfo(false); // not an admin
		userInfo.setIndividualGroup(individualGroup);
		String ownerId = userProfile.getOwnerId();
		assertEquals(ownerId, individualGroup.getId());
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertEquals(userProfile, upClone);
		
		String newURL = "http://"+rand.nextLong(); // just a random long number
		upClone.setRStudioUrl(newURL);
		
		userProfileManager.updateUserProfile(userInfo, upClone);
		
		upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertEquals(newURL, upClone.getRStudioUrl());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUpdateOthersUserProfle() throws Exception {
		assertNotNull(individualGroup);
		assertNotNull(userProfile);
		UserInfo userInfo = new UserInfo(false); // not an admin
		UserGroup otherIndividualGroup = new UserGroup();
		otherIndividualGroup.setId("-100");
		userInfo.setIndividualGroup(otherIndividualGroup);
		String ownerId = userProfile.getOwnerId();
		
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		// so we get back the UserProfile for the specified owner...
		assertEquals(ownerId, upClone.getOwnerId());
		// ... but we can't update it, since we are not the owner or an admin
		// the following step will fail
		userProfileManager.updateUserProfile(userInfo, upClone);
	}
	

}
