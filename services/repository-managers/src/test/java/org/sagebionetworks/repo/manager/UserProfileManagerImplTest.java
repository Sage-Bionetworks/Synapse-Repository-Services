package org.sagebionetworks.repo.manager;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserPreference;
import org.sagebionetworks.repo.model.UserPreferenceBoolean;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserProfileManagerImplTest {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	private static final String USER_NAME = "foobar";
	private static final String USER_EMAIL = "foo@bar.com";
	private Long userId;
	
	
	@Before
	public void setUp() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(USER_EMAIL);
		user.setFirstName("Foo");
		user.setLastName("Bar");
		user.setUserName(USER_NAME);
		userId = userManager.createUser(user);
	}

	@After
	public void tearDown() throws Exception {
		if(userId != null){
			userManager.deletePrincipal(new UserInfo(true, 0L), userId);
		}
		userId = null;
	}

	@Test
	public void testGetAttachmentUrl() throws Exception{
		assertNotNull(userId);
		UserInfo userInfo = new UserInfo(false, userId); // not an admin
		
		Long tokenId = new Long(456);
		String otherUserProfileId = "12345";
		
		// Make the actual call
		userProfileManager.getUserProfileAttachmentUrl(userInfo.getId(), otherUserProfileId, tokenId.toString());
	}
	
	@Test
	public void testCreateS3AttachmentToken() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		assertNotNull(userId);
		UserInfo userInfo = new UserInfo(false, userId); // not an admin
		
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName("/some.jpg");
		String almostMd5 = "79054025255fb1a26e4bc422aef54eb4";
		startToken.setMd5(almostMd5);
		String userId = this.userId.toString();
		// Make the actual calls
		S3AttachmentToken endToken = userProfileManager.createS3UserProfileAttachmentToken(userInfo, userId, startToken);
		assertNotNull(endToken);
		assertNotNull(endToken.getPresignedUrl());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateS3AttachmentTokenFromInvalidFile() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		UserInfo userInfo = new UserInfo(false, userId.toString()); // not an admin
		
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName("/not_an_image.txt");
		String almostMd5 = "79054025255fb1a26e4bc422aef54eb4";
		startToken.setMd5(almostMd5);
		Long tokenId = new Long(456);
		String userId = this.userId.toString();
		//next line should result in an IllegalArgumentException, since the filename does not not indicate an image file that we recognize
		userProfileManager.createS3UserProfileAttachmentToken(userInfo, userId, startToken);
	}
	
	@Test
	public void testCRU() throws DatastoreException, UnauthorizedException, NotFoundException{
		// delete the existing user profile so we can create our own
		userProfileDAO.delete(userId.toString());
		
		// Create a new UserProfile
		Long principalId = Long.parseLong(this.userId.toString());
		UserProfile created;
		{
			UserProfile profile = new UserProfile();
			profile.setCompany("Spies 'R' Us");
			profile.setFirstName("James");
			profile.setLastName("Bond");
			profile.setOwnerId(this.userId.toString());
			profile.setUserName(USER_NAME);
			Map<String, UserPreference> prefs = new HashMap<String, UserPreference>();
			UserPreferenceBoolean pref = new UserPreferenceBoolean();
			pref.setValue(true);
			prefs.put("someBoolPref", pref);
			profile.setPreferences(prefs);
			// Create the profile
			created = this.userProfileManager.createUserProfile(profile);
			// the changed fields are etag and emails (which are ignored)
			// set these fields in 'profile' so we can compare to 'created'
			profile.setEmails(Collections.singletonList(USER_EMAIL));
			profile.setOpenIds(new ArrayList<String>());
			profile.setUserName(USER_NAME);
			profile.setEtag(created.getEtag());
			assertEquals(profile, created);
		}
		assertNotNull(created);
		assertNotNull(created.getEtag());
		
		
		UserInfo userInfo = new UserInfo(false, principalId);
		// Get it back
		UserProfile clone = userProfileManager.getUserProfile(userInfo, principalId.toString());
		assertEquals(created, clone);
		
		// Make sure we can update it
		created.setUserName("newUsername");
		Map<String, UserPreference> prefs = created.getPreferences();
		UserPreferenceBoolean pref = (UserPreferenceBoolean)prefs.get("someBoolPref");
		assertEquals(true, pref.getValue());
		pref.setValue(false);
		String startEtag = created.getEtag();
		// Changing emails is currently disabled See 
		UserProfile updated = userProfileManager.updateUserProfile(userInfo, created);
		assertFalse("Update failed to update the etag",startEtag.equals(updated.getEtag()));
		// Get it back
		clone = userProfileManager.getUserProfile(userInfo, principalId.toString());
		assertEquals(updated, clone);
		assertEquals("newUsername", clone.getUserName());
		
	}
	
	// Note:  In PLFM-2486 we allow the client to change the emails passed in, we just ignore them
	@Test
	public void testPLFM_2504() throws DatastoreException, UnauthorizedException, NotFoundException{
		// delete the existing user profile so we can create our own
		userProfileDAO.delete(userId.toString());

		// Create a new UserProfile
		Long principalId = Long.parseLong(this.userId.toString());
		UserProfile profile = new UserProfile();
		profile.setCompany("Spies 'R' Us");
		profile.setEmails(new LinkedList<String>());
		profile.getEmails().add("jamesBond@spies.org");
		profile.setUserName("007");
		profile.setOwnerId(this.userId.toString());
		// Create the profile
		profile = this.userProfileManager.createUserProfile(profile);
		assertNotNull(profile);
		assertNotNull(profile.getUserName());
		assertNotNull(profile.getEtag());
		
		UserInfo userInfo = new UserInfo(false, principalId);
		// Get it back
		UserProfile clone = userProfileManager.getUserProfile(userInfo, principalId.toString());
		assertEquals(profile, clone);
		assertEquals(Collections.singletonList(USER_EMAIL), clone.getEmails());
		
		// try to update it
		profile.getEmails().clear();
		profile.getEmails().add("myNewEmail@spies.org");
		String startEtag = profile.getEtag();
		// update
		// OK to change emails, as any changes to email are ignored
		profile = userProfileManager.updateUserProfile(userInfo, profile);
		assertEquals(Collections.singletonList(USER_EMAIL), profile.getEmails());
	}
}
