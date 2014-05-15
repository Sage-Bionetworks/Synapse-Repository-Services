package org.sagebionetworks.repo.manager;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserProfileManagerImplTest {
	
	@Autowired
	private UserGroupDAO userGroupDAO;
	
	@Autowired
	private UserProfileManager userProfileManager;
	
	private UserGroup individualGroup = null;
	
	
	@Before
	public void setUp() throws Exception {
		individualGroup = new UserGroup();
		individualGroup.setIsIndividual(true);
		individualGroup.setCreationDate(new Date());
		Long id = userGroupDAO.create(individualGroup);
		individualGroup.setId(id.toString());
		assertNotNull(individualGroup);

	}

	@After
	public void tearDown() throws Exception {
		if(individualGroup != null){
			userGroupDAO.delete(individualGroup.getId());
		}
		individualGroup = null;
	}

	@Test
	public void testGetAttachmentUrl() throws Exception{
		assertNotNull(individualGroup);
		UserInfo userInfo = new UserInfo(false, individualGroup.getId()); // not an admin
		
		Long tokenId = new Long(456);
		String otherUserProfileId = "12345";
		
		// Make the actual call
		userProfileManager.getUserProfileAttachmentUrl(userInfo.getId(), otherUserProfileId, tokenId.toString());
	}
	
	@Test
	public void testCreateS3AttachmentToken() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		UserInfo userInfo = new UserInfo(false, individualGroup.getId()); // not an admin
		
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName("/some.jpg");
		String almostMd5 = "79054025255fb1a26e4bc422aef54eb4";
		startToken.setMd5(almostMd5);
		Long tokenId = new Long(456);
		String userId = individualGroup.getId();
		// Make the actual calls
		S3AttachmentToken endToken = userProfileManager.createS3UserProfileAttachmentToken(userInfo, userId, startToken);
		assertNotNull(endToken);
		assertNotNull(endToken.getPresignedUrl());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateS3AttachmentTokenFromInvalidFile() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		UserInfo userInfo = new UserInfo(false, individualGroup.getId()); // not an admin
		
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName("/not_an_image.txt");
		String almostMd5 = "79054025255fb1a26e4bc422aef54eb4";
		startToken.setMd5(almostMd5);
		Long tokenId = new Long(456);
		String userId = individualGroup.getId();
		//next line should result in an IllegalArgumentException, since the filename does not not indicate an image file that we recognize
		userProfileManager.createS3UserProfileAttachmentToken(userInfo, userId, startToken);
	}
	
	@Test
	public void testCRU() throws DatastoreException, UnauthorizedException, NotFoundException{
		// Create a new UserProfile
		Long principalId = Long.parseLong(this.individualGroup.getId());
		UserProfile created;
		{
			UserProfile profile = new UserProfile();
			profile.setCompany("Spies 'R' Us");
			profile.setEmails(new LinkedList<String>());
			profile.getEmails().add("jamesBond@spies.org");
			profile.getEmails().add("jamesBon2@spies.org");
			profile.setOpenIds(new LinkedList<String>());
			profile.getOpenIds().add("https://google.com/007");
			profile.setUserName("007");
			profile.setFirstName("James");
			profile.setLastName("Bond");
			profile.setOwnerId(this.individualGroup.getId());
			// Create the profile
			created = this.userProfileManager.createUserProfile(profile);
			// the changed fields are etag and emails (which are ignored)
			// set these fields in 'profile' so we can compare to 'created'
			profile.setEmails(created.getEmails());
			profile.setOpenIds(created.getOpenIds());
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
		
		
	}
	
	// Note:  In PLFM-2486 we allow the client to change the emails passed in, we just ignore them
	@Test
	public void testPLFM_2504() throws DatastoreException, UnauthorizedException, NotFoundException{
		// Create a new UserProfile
		Long principalId = Long.parseLong(this.individualGroup.getId());
		UserProfile profile = new UserProfile();
		profile.setCompany("Spies 'R' Us");
		profile.setEmails(new LinkedList<String>());
		profile.getEmails().add("jamesBond@spies.org");
		profile.setUserName("007");
		profile.setOwnerId(this.individualGroup.getId());
		// Create the profile
		profile = this.userProfileManager.createUserProfile(profile);
		assertNotNull(profile);
		assertNotNull(profile.getEtag());
		
		UserInfo userInfo = new UserInfo(false, principalId);
		// Get it back
		UserProfile clone = userProfileManager.getUserProfile(userInfo, principalId.toString());
		assertEquals(profile, clone);
		// Since we don't allow setting email via the UP, we get nothing back
		assertTrue(clone.getEmails().isEmpty());
		
		// Make sure we can update it
		profile.getEmails().clear();
		profile.getEmails().add("myNewEmail@spies.org");
		String startEtag = profile.getEtag();
		// update
		// OK to change emails, as any changes to email are ignored
		profile = userProfileManager.updateUserProfile(userInfo, profile);
		assertTrue(profile.getEmails().isEmpty());
	}
}
