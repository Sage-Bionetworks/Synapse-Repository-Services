package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
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
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.dbo.dao.UserProfileUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;

public class UserProfileManagerImplUnitTest {

	LocationHelper mocKLocationHelper;
	UserProfileDAO mockProfileDAO;
	UserGroupDAO mockUserGroupDAO;
	S3TokenManager mockS3TokenManager;
	UserManager mockUserManager;
	FavoriteDAO mockFavoriteDAO;
	AttachmentManager mockAttachmentManager;
	PrincipalAliasDAO mockPrincipalAliasDAO;
	
	
	UserProfileManager userProfileManager;
	
	UserInfo userInfo;
	UserInfo adminUserInfo;
	UserProfile userProfile;
	S3AttachmentToken testToken;
	
	private static final Long userId = 9348725L;
	private static final Long adminUserId = 823746L;
	
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
		mockPrincipalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		userProfileManager = new UserProfileManagerImpl(mockProfileDAO, mockUserGroupDAO, mockS3TokenManager, mockFavoriteDAO, mockAttachmentManager, mockPrincipalAliasDAO);
		
		
		userInfo = new UserInfo(false, 111L);

		adminUserInfo = new UserInfo(true, 456L);
		
		userProfile = new UserProfile();
		userProfile.setOwnerId(userInfo.getId().toString());
		userProfile.setRStudioUrl("myPrivateRStudioUrl");
		userProfile.setUserName("TEMPORARY-111");
		userProfile.setLocation("I'm guessing this is private");
		userProfile.setEmails(new LinkedList<String>());
		userProfile.getEmails().add("foo@bar.org");
		userProfile.setOpenIds(new LinkedList<String>());
		
		// UserProfileDAO should return a copy of the mock UserProfile when getting
		Mockito.doAnswer(new Answer<UserProfile>() {
			@Override
			public UserProfile answer(InvocationOnMock invocation) throws Throwable {
				DBOUserProfile intermediate = new DBOUserProfile();
				UserProfileUtils.copyDtoToDbo(userProfile, intermediate);
				UserProfile copy = UserProfileUtils.convertDboToDto(intermediate);
				return copy;
			}
		}).when(mockProfileDAO).get(Mockito.anyString());
		
		// UserProfileDAO should return a copy of the argument when updating
		Mockito.doAnswer(new Answer<UserProfile>() {
			@Override
			public UserProfile answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				UserProfile copy = (UserProfile) args[0];
				
				DBOUserProfile intermediate = new DBOUserProfile();
				UserProfileUtils.copyDtoToDbo(copy, intermediate);
				copy = UserProfileUtils.convertDboToDto(intermediate);
				return copy;
			}
		}).when(mockProfileDAO).update((UserProfile) Mockito.any());
		
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
		PresignedUrl expectedPreSignedUrl = new PresignedUrl();
		expectedPreSignedUrl.setPresignedUrl("I am a presigned url! whooot!");
		when(mockUserManager.getUserInfo(userId)).thenReturn(adminUserInfo);
		when(mockS3TokenManager.getAttachmentUrl(userId, profileId, tokenId.toString())).thenReturn(expectedPreSignedUrl);
		// Make the actual call
		PresignedUrl url = userProfileManager.getUserProfileAttachmentUrl(userId, profileId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectedPreSignedUrl.getPresignedUrl(), url.getPresignedUrl());
	}	
	@Test
	public void testOwnerGetPresignedUrl() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		// Simulate an normal user trying to access a presigned url
		Long tokenId = new Long(456);
		String profileId = "132";
		PresignedUrl expectedPreSignedUrl = new PresignedUrl();
		expectedPreSignedUrl.setPresignedUrl("I am a presigned url! whooot!");
		when(mockUserManager.getUserInfo(userId)).thenReturn(adminUserInfo);
		when(mockS3TokenManager.getAttachmentUrl(adminUserId, profileId, tokenId.toString())).thenReturn(expectedPreSignedUrl);
		// Make the actual call
		PresignedUrl url = userProfileManager.getUserProfileAttachmentUrl(adminUserId, profileId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectedPreSignedUrl.getPresignedUrl(), url.getPresignedUrl());
	}
	@Test
	public void testNonOwnerGetPresignedUrl() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		// Simulate an normal user trying to access a presigned url
		Long tokenId = new Long(456);
		String profileId = "132";
		Long otherUserId = 209384L;
		
		PresignedUrl expectedPreSignedUrl = new PresignedUrl();
		expectedPreSignedUrl.setPresignedUrl("I am a presigned url! whooot!");
		when(mockUserManager.getUserInfo(userId)).thenReturn(adminUserInfo);
		when(mockS3TokenManager.getAttachmentUrl(otherUserId, profileId, tokenId.toString())).thenReturn(expectedPreSignedUrl);
		
		// Make the actual call
		PresignedUrl url = userProfileManager.getUserProfileAttachmentUrl(otherUserId, profileId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectedPreSignedUrl.getPresignedUrl(), url.getPresignedUrl());
	}	
	
	@Test
	public void testAddFavorite() throws Exception {
		String entityId = "syn123";
		userProfileManager.addFavorite(userInfo, entityId);
		Favorite fav = new Favorite();
		fav.setPrincipalId(userInfo.getId().toString());
		fav.setEntityId(entityId);
		verify(mockFavoriteDAO).add(fav);
	}
	
	
	/* Tests moved and mocked from UserProfileManagerImplTest */
	
	@Test
	public void testGetOwnUserProfle() throws Exception {
		String ownerId = userInfo.getId().toString();
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertEquals(userProfile, upClone);
	}
		
	@Test
	public void testgetOthersUserProfleByAdmin() throws Exception {
		String ownerId = userInfo.getId().toString();
		UserProfile upClone = userProfileManager.getUserProfile(adminUserInfo, ownerId);
		assertEquals(userProfile, upClone);
	}
	
	@Ignore
	@Test
	public void testUpdateOwnUserProfle() throws Exception {
		// Get a copy of a UserProfile to update
		String ownerId = userInfo.getId().toString();
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertEquals(userProfile, upClone);
		
		// Change a field
		String newURL = "http://"+rand.nextLong(); // just a random long number
		upClone.setRStudioUrl(newURL);
		upClone.setUserName("jamesBond");
		upClone = userProfileManager.updateUserProfile(userInfo, upClone);
	}
	
	@Test(expected=UnauthorizedException.class)
	public void testUpdateOthersUserProfle() throws Exception {
		String ownerId = userInfo.getId().toString();
		userInfo.setId(-100L);
		
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		// so we get back the UserProfile for the specified owner...
		assertEquals(ownerId, upClone.getOwnerId());
		// ... but we can't update it, since we are not the owner or an admin
		// the following step will fail
		userProfileManager.updateUserProfile(userInfo, upClone);
	}
}
