package org.sagebionetworks.repo.manager;


import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;
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

	private IdGenerator mockIdGenerator;
	private static final String TEST_USER_NAME = "test-user";
	
	private UserGroup individualGroup = null;
	
	
	@Before
	public void setUp() throws Exception {
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		if (individualGroup == null) {
			individualGroup = new UserGroup();
			individualGroup.setName(TEST_USER_NAME);
			individualGroup.setIsIndividual(true);
			individualGroup.setCreationDate(new Date());
			userGroupDAO.create(individualGroup);
		}
		individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		assertNotNull(individualGroup);

		mockIdGenerator = Mockito.mock(IdGenerator.class);
	}

	@After
	public void tearDown() throws Exception {
		UserGroup individualGroup = userGroupDAO.findGroup(TEST_USER_NAME, true);
		userGroupDAO.delete(individualGroup.getId());
		individualGroup = null;
	}

	@Test
	public void testGetAttachmentUrl() throws Exception{
		assertNotNull(individualGroup);
		UserInfo userInfo = new UserInfo(false); // not an admin
		userInfo.setIndividualGroup(individualGroup);
		
		Long tokenId = new Long(456);
		String otherUserProfileId = "12345";
		
		// Make the actual call
		userProfileManager.getUserProfileAttachmentUrl(userInfo, otherUserProfileId, tokenId.toString());
	}
	
	@Test
	public void testCreateS3AttachmentToken() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		UserInfo userInfo = new UserInfo(false); // not an admin
		userInfo.setIndividualGroup(individualGroup);
		
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName("/some.jpg");
		String almostMd5 = "79054025255fb1a26e4bc422aef54eb4";
		startToken.setMd5(almostMd5);
		Long tokenId = new Long(456);
		String userId = individualGroup.getId();
		when(mockIdGenerator.generateNewId()).thenReturn(tokenId);
		// Make the actual calls
		S3AttachmentToken endToken = userProfileManager.createS3UserProfileAttachmentToken(userInfo, userId, startToken);
		assertNotNull(endToken);
		assertNotNull(endToken.getPresignedUrl());
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCreateS3AttachmentTokenFromInvalidFile() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		UserInfo userInfo = new UserInfo(false); // not an admin
		userInfo.setIndividualGroup(individualGroup);
		
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName("/not_an_image.txt");
		String almostMd5 = "79054025255fb1a26e4bc422aef54eb4";
		startToken.setMd5(almostMd5);
		Long tokenId = new Long(456);
		String userId = individualGroup.getId();
		when(mockIdGenerator.generateNewId()).thenReturn(tokenId);
		//next line should result in an IllegalArgumentException, since the filename does not not indicate an image file that we recognize
		userProfileManager.createS3UserProfileAttachmentToken(userInfo, userId, startToken);
	}
}
