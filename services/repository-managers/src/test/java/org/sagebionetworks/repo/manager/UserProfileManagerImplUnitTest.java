package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Random;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.dbo.dao.UserProfileUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.ObjectSchema;

public class UserProfileManagerImplUnitTest {

	LocationHelper mocKLocationHelper;
	UserProfileDAO mockProfileDAO;
	UserGroupDAO mockUserGroupDAO;
	S3TokenManager mockS3TokenManager;
	UserManager mockUserManager;
	FavoriteDAO mockFavoriteDAO;
	AttachmentManager mockAttachmentManager;
	
	UserProfileManager userProfileManager;
	
	UserInfo userInfo;
	UserGroup user;
	UserInfo adminUserInfo;
	UserProfile userProfile;
	S3AttachmentToken testToken;
	
	private Random rand = new Random();
	private static final String TEST_USER_NAME = "TruncatableName@test.com";
	
	@Before
	public void before() throws Exception {
		mocKLocationHelper = Mockito.mock(LocationHelper.class);
		mockProfileDAO = Mockito.mock(UserProfileDAO.class);
		mockUserGroupDAO = Mockito.mock(UserGroupDAO.class);
		mockS3TokenManager = Mockito.mock(S3TokenManager.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockFavoriteDAO = Mockito.mock(FavoriteDAO.class);
		mockAttachmentManager = Mockito.mock(AttachmentManager.class);
		userProfileManager = new UserProfileManagerImpl(mockProfileDAO, mockUserGroupDAO, mockS3TokenManager, mockFavoriteDAO, mockAttachmentManager);
		
		user = new UserGroup();
		user.setId("111");
		user.setName(TEST_USER_NAME);
		user.setIsIndividual(true);
		
		userInfo = new UserInfo(false);
		userInfo.setIndividualGroup(user);
		
		adminUserInfo = new UserInfo(true);
		adminUserInfo.setIndividualGroup(user);
		adminUserInfo.setUser(new User());
		adminUserInfo.getUser().setUserId("This should not appear in the profiles");
		
		userProfile = new UserProfile();
		userProfile.setOwnerId(user.getId());
		userProfile.setDisplayName("test-user display-name");
		userProfile.setRStudioUrl("myPrivateRStudioUrl");
		userProfile.setEmail(TEST_USER_NAME);
		userProfile.setLocation("I'm guessing this is private");
		
		// UserProfileDAO should return a copy of the mock UserProfile when getting
		Mockito.doAnswer(new Answer<UserProfile>() {
			@Override
			public UserProfile answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				ObjectSchema schema = (ObjectSchema) args[1];
				
				DBOUserProfile intermediate = new DBOUserProfile();
				UserProfile copy = new UserProfile();
				UserProfileUtils.copyDtoToDbo(userProfile, intermediate, schema);
				UserProfileUtils.copyDboToDto(intermediate, copy, schema);
				return copy;
			}
		}).when(mockProfileDAO).get(Mockito.anyString(), (ObjectSchema) Mockito.any());
		
		// UserProfileDAO should return a copy of the argument when updating
		Mockito.doAnswer(new Answer<UserProfile>() {
			@Override
			public UserProfile answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				UserProfile copy = (UserProfile) args[0];
				ObjectSchema schema = (ObjectSchema) args[1];
				
				DBOUserProfile intermediate = new DBOUserProfile();
				UserProfileUtils.copyDtoToDbo(copy, intermediate, schema);
				UserProfileUtils.copyDboToDto(intermediate, copy, schema);
				return copy;
			}
		}).when(mockProfileDAO).update((UserProfile) Mockito.any(), (ObjectSchema) Mockito.any());
		
		testToken = new S3AttachmentToken();
		testToken.setFileName("testonly.jpg");
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateS3URLNonAdminNonOwner() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		userProfileManager.createS3UserProfileAttachmentToken(userInfo, "222", testToken);
	}
	@Test
	public void testIsOwner() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		userProfileManager.createS3UserProfileAttachmentToken(userInfo, "111", testToken);			
	}
	@Test
	public void testIsAdmin() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		userProfileManager.createS3UserProfileAttachmentToken(adminUserInfo, "Superman", testToken);
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
		PresignedUrl url = userProfileManager.getUserProfileAttachmentUrl(userInfo, profileId, tokenId.toString());
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
		PresignedUrl url = userProfileManager.getUserProfileAttachmentUrl(adminUserInfo, profileId, tokenId.toString());
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
		PresignedUrl url = userProfileManager.getUserProfileAttachmentUrl(userInfo2, profileId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectedPreSignedUrl.getPresignedUrl(), url.getPresignedUrl());
	}	
	
	@Test
	public void testAddFavorite() throws Exception {
		String entityId = "syn123";
		userProfileManager.addFavorite(userInfo, entityId);
		Favorite fav = new Favorite();
		fav.setPrincipalId(userInfo.getIndividualGroup().getId());
		fav.setEntityId(entityId);
		verify(mockFavoriteDAO).add(fav);
	}
	
	
	/* Tests moved and mocked from UserProfileManagerImplTest */
	
	@Test
	public void testGetOwnUserProfle() throws Exception {
		String ownerId = userInfo.getIndividualGroup().getId();
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertEquals(userProfile, upClone);
	}
	
	@Test
	@Ignore // Private fields are removed in the service layer
	public void testgetOthersUserProfle() throws Exception {
		String ownerId = userInfo.getIndividualGroup().getId();
		userInfo.getIndividualGroup().setId("-100");
		
		// There should be missing fields, intentionally blanked-out
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertFalse(userProfile.equals(upClone));
		assertEquals(userProfile.getDisplayName(), upClone.getDisplayName());
	}
	
	@Test
	public void testgetOthersUserProfleByAdmin() throws Exception {
		String ownerId = userInfo.getIndividualGroup().getId();
		UserProfile upClone = userProfileManager.getUserProfile(adminUserInfo, ownerId);
		assertEquals(userProfile, upClone);
	}
	
	@Test
	public void testUpdateOwnUserProfle() throws Exception {
		// Get a copy of a UserProfile to update
		String ownerId = userInfo.getIndividualGroup().getId();
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertEquals(userProfile, upClone);
		
		// Change a field
		String newURL = "http://"+rand.nextLong(); // just a random long number
		upClone.setRStudioUrl(newURL);
		upClone = userProfileManager.updateUserProfile(userInfo, upClone);
		assertEquals(newURL, upClone.getRStudioUrl());
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUpdateOthersUserProfle() throws Exception {
		String ownerId = userInfo.getIndividualGroup().getId();
		userInfo.getIndividualGroup().setId("-100");
		
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		// so we get back the UserProfile for the specified owner...
		assertEquals(ownerId, upClone.getOwnerId());
		// ... but we can't update it, since we are not the owner or an admin
		// the following step will fail
		userProfileManager.updateUserProfile(userInfo, upClone);
	}
}
