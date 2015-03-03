package org.sagebionetworks.repo.manager;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.auth.NewUser;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserProfileManagerImplTest {
	
	@Autowired
	private UserManager userManager;
	
	@Autowired
	private UserProfileDAO userProfileDAO;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	@Autowired
	private FileHandleManager fileHandleManger;
	
	@Autowired
	private AmazonS3Client s3Client;
	
	private static final String USER_NAME = "foobar";
	private static final String USER_EMAIL = "foo@bar.com";
	private Long userId;
	UserInfo userInfo;
	
	@Before
	public void setUp() throws Exception {
		NewUser user = new NewUser();
		user.setEmail(USER_EMAIL);
		user.setFirstName("Foo");
		user.setLastName("Bar");
		user.setUserName(USER_NAME);
		userId = userManager.createUser(user);
		userInfo = new UserInfo(false, userId);
	}

	@After
	public void tearDown() throws Exception {
		if(userId != null){
			userManager.deletePrincipal(new UserInfo(true, 0L), userId);
		}
		userId = null;
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
	
	/**
	 * See PLFM-2319.  Originally a user's profile picture was a locationable attachment.
	 * We have since converted the profile pictures to be FileHandles.
	 * @throws Exception 
	 */
	@Test
	public void testConvertAttachmentToFileHandle() throws Exception{
		String userIdString = ""+userId;
		AttachmentData attachment = fileHandleManger.createAttachmentInS3("image data", "user.jpg", userIdString, userIdString, new Date());
		// Update the user profile
		UserProfile profile = userProfileManager.getUserProfile(userInfo, userIdString);
		profile.setPic(attachment);
		// The get call should convert the picture to a file handle
		profile = userProfileManager.updateUserProfile(userInfo, profile);
		assertEquals("The 'pic' fields should have been replaced and set to null",null, profile.getPic());
		assertNotNull(profile.getProfilePicureFileHandleId());
		// get the presigned url for this handle
		assertNotNull(userProfileManager.getUserProfileImageUrl(userIdString));
	}
	
	@Test (expected=NotFoundException.class)
	public void testGetPicturePresignedUrlNotFound() throws Exception{
		String userIdString = ""+userId;
		// get the presigned url for this handle
		assertNotNull(userProfileManager.getUserProfileImageUrl(userIdString));
	}

}
