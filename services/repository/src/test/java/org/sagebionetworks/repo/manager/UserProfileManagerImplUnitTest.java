package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.web.NotFoundException;

public class UserProfileManagerImplUnitTest {

	UserProfileManagerImpl userProfileManagerImpl;
	UserProfileDAO mockProfileDAO;
	S3TokenManager mockS3TokenManager;
	UserInfo userInfo;
	UserGroup user;
	UserInfo adminUserInfo;
	
	@Before
	public void before() {
		mockProfileDAO = Mockito.mock(UserProfileDAO.class);
		mockS3TokenManager = Mockito.mock(S3TokenManager.class);
		userProfileManagerImpl = new UserProfileManagerImpl(mockProfileDAO, mockS3TokenManager);
		userInfo = new UserInfo(false);
		adminUserInfo = new UserInfo(true);
		user = new UserGroup();
		user.setId("Bob");
		userInfo.setIndividualGroup(user);
		adminUserInfo.setIndividualGroup(user);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateS3URLNonAdminNonOwner() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		userProfileManagerImpl.createS3UserProfileAttachmentToken(userInfo, "Joe", new S3AttachmentToken());
	}
	@Test
	public void testIsOwner() {
		try {
			userProfileManagerImpl.createS3UserProfileAttachmentToken(userInfo, "Bob", new S3AttachmentToken());			
		} catch (Exception e) {
			assertTrue("Owner could not create S3 profile attachment token", false);
		}
	}
	@Test
	public void testIsAdmin() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		try {
			userProfileManagerImpl.createS3UserProfileAttachmentToken(adminUserInfo, "Superman", new S3AttachmentToken());
		} catch (Exception e) {
			assertTrue("Admin could not create S3 profile attachment token", false);
		}
	}	
}
