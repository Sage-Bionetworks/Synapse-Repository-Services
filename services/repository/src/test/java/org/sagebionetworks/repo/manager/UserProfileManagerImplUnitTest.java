package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;

public class UserProfileManagerImplUnitTest {

	UserProfileManagerImpl userProfileManagerImpl;
	UserProfileDAO mockProfileDAO;
	S3TokenManager mockS3TokenManager;
	UserInfo userInfo;
	UserGroup user;
	UserInfo adminUserInfo;
	UserManager mockUserManager;
	LocationHelper mocKLocationHelper;
	
	
	@Before
	public void before() {
		mocKLocationHelper = Mockito.mock(LocationHelper.class);
		mockProfileDAO = Mockito.mock(UserProfileDAO.class);
		mockS3TokenManager = Mockito.mock(S3TokenManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		userProfileManagerImpl = new UserProfileManagerImpl(mockProfileDAO, mockS3TokenManager);
		userInfo = new UserInfo(false);
		adminUserInfo = new UserInfo(true);
		user = new UserGroup();
		user.setId("111");
		userInfo.setIndividualGroup(user);
		adminUserInfo.setIndividualGroup(user);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateS3URLNonAdminNonOwner() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		userProfileManagerImpl.createS3UserProfileAttachmentToken(userInfo, "222", new S3AttachmentToken());
	}
	@Test
	public void testIsOwner() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		userProfileManagerImpl.createS3UserProfileAttachmentToken(userInfo, "111", new S3AttachmentToken());			
	}
	@Test
	public void testIsAdmin() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		userProfileManagerImpl.createS3UserProfileAttachmentToken(adminUserInfo, "Superman", new S3AttachmentToken());
	}	
	@Test
	public void testAdminGetPresignedUrl() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		// Simulate an admin trying to access a presigned url
		Long tokenId = new Long(456);
		String profileId = "132";
		String userId = "007";
		PresignedUrl expectedPreSignedUrl = new PresignedUrl();
		expectedPreSignedUrl.setPresignedUrl("I am a presigned url! whooot!");
		when(mockUserManager.getUserInfo(userId)).thenReturn(adminUserInfo);
		when(mockS3TokenManager.getAttachmentUrl(userInfo, profileId, tokenId.toString())).thenReturn(expectedPreSignedUrl);
		// Make the actual call
		PresignedUrl url = userProfileManagerImpl.getUserProfileAttachmentUrl(userInfo, profileId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectedPreSignedUrl.getPresignedUrl(), url.getPresignedUrl());
	}	
	@Test
	public void testOwnerGetPresignedUrl() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		// Simulate an normal user trying to access a presigned url
		Long tokenId = new Long(456);
		String profileId = "132";
		String userId = userInfo.getIndividualGroup().getId();
		PresignedUrl expectedPreSignedUrl = new PresignedUrl();
		expectedPreSignedUrl.setPresignedUrl("I am a presigned url! whooot!");
		when(mockUserManager.getUserInfo(userId)).thenReturn(adminUserInfo);
		when(mockS3TokenManager.getAttachmentUrl(adminUserInfo, profileId, tokenId.toString())).thenReturn(expectedPreSignedUrl);
		// Make the actual call
		PresignedUrl url = userProfileManagerImpl.getUserProfileAttachmentUrl(adminUserInfo, profileId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectedPreSignedUrl.getPresignedUrl(), url.getPresignedUrl());
	}
	@Test
	public void testNonOwnerGetPresignedUrl() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		// Simulate an normal user trying to access a presigned url
		Long tokenId = new Long(456);
		String profileId = "132";
		
		UserInfo userInfo2 = new UserInfo(false);
		UserGroup user2 = new UserGroup();
		user2.setId("222");
		
		String userId = userInfo.getIndividualGroup().getId();
		PresignedUrl expectedPreSignedUrl = new PresignedUrl();
		expectedPreSignedUrl.setPresignedUrl("I am a presigned url! whooot!");
		when(mockUserManager.getUserInfo(userId)).thenReturn(adminUserInfo);
		when(mockS3TokenManager.getAttachmentUrl(userInfo2, profileId, tokenId.toString())).thenReturn(expectedPreSignedUrl);
		
		// Make the actual call
		PresignedUrl url = userProfileManagerImpl.getUserProfileAttachmentUrl(userInfo2, profileId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectedPreSignedUrl.getPresignedUrl(), url.getPresignedUrl());
	}	
}
