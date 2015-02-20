package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.Favorite;
import org.sagebionetworks.repo.model.FavoriteDAO;
import org.sagebionetworks.repo.model.IdSet;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.UserProfileDAO;
import org.sagebionetworks.repo.model.attachment.AttachmentData;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.UserProfileUtils;
import org.sagebionetworks.repo.model.dbo.persistence.DBOUserProfile;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.principal.AliasType;
import org.sagebionetworks.repo.model.principal.PrincipalAlias;
import org.sagebionetworks.repo.model.principal.PrincipalAliasDAO;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;

public class UserProfileManagerImplUnitTest {

	LocationHelper mocKLocationHelper;
	UserProfileDAO mockProfileDAO;
	UserGroupDAO mockUserGroupDAO;
	UserManager mockUserManager;
	FavoriteDAO mockFavoriteDAO;
	PrincipalAliasDAO mockPrincipalAliasDAO;
	AuthorizationManager mockAuthorizationManager;
	AmazonS3Client mockS3Client;
	FileHandleDao mockFileHandleDao;
	
	UserProfileManager userProfileManager;
	
	UserInfo userInfo;
	UserInfo adminUserInfo;
	UserProfile userProfile;
	S3AttachmentToken testToken;
	
	private static final Long userId = 9348725L;
	private static final Long adminUserId = 823746L;
	private static final String USER_EMAIL = "foo@bar.org";
	private static final String USER_OPEN_ID = "http://myspace.com/foo";
	
	private Random rand = new Random();
	
	@Before
	public void before() throws Exception {
		mocKLocationHelper = Mockito.mock(LocationHelper.class);
		mockProfileDAO = Mockito.mock(UserProfileDAO.class);
		mockUserGroupDAO = Mockito.mock(UserGroupDAO.class);
		mockUserManager = Mockito.mock(UserManager.class);
		mockFavoriteDAO = Mockito.mock(FavoriteDAO.class);
		mockPrincipalAliasDAO = Mockito.mock(PrincipalAliasDAO.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		mockFileHandleDao = Mockito.mock(FileHandleDao.class);
		userProfileManager = new UserProfileManagerImpl(mockProfileDAO, mockUserGroupDAO, mockFavoriteDAO, mockPrincipalAliasDAO, mockAuthorizationManager, mockS3Client,mockFileHandleDao);
		
		
		userInfo = new UserInfo(false, userId);

		adminUserInfo = new UserInfo(true, adminUserId);
		
		userProfile = new UserProfile();
		userProfile.setOwnerId(userInfo.getId().toString());
		userProfile.setRStudioUrl("myPrivateRStudioUrl");
		userProfile.setUserName("TEMPORARY-111");
		userProfile.setLocation("I'm guessing this is private");
		userProfile.setOpenIds(Collections.singletonList(USER_OPEN_ID));
		userProfile.setEmails(Collections.singletonList(USER_EMAIL));
		
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
		
		PrincipalAlias alias = new PrincipalAlias();
		alias.setAlias(USER_EMAIL);
		alias.setPrincipalId(userId);
		alias.setType(AliasType.USER_EMAIL);
		List<PrincipalAlias> aliases =  new ArrayList<PrincipalAlias>();
		aliases.add(alias);
		alias = new PrincipalAlias();
		alias.setAlias(USER_OPEN_ID);
		alias.setPrincipalId(userId);
		alias.setType(AliasType.USER_OPEN_ID);
		aliases.add(alias);
		when(mockPrincipalAliasDAO.listPrincipalAliases(userId)).thenReturn(aliases);
		when(mockPrincipalAliasDAO.listPrincipalAliases(Collections.singleton(userId))).thenReturn(aliases);
		
	}
	
//	@Test (expected=UnauthorizedException.class)
//	public void testCreateS3URLNonAdminNonOwner() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
//		userProfileManager.createS3UserProfileAttachmentToken(userInfo, "222", testToken);
//	}
//	@Test
//	public void testIsOwner() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
//		userProfileManager.createS3UserProfileAttachmentToken(userInfo, userId.toString(), testToken);			
//	}
//	@Test
//	public void testIsAdmin() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
//		userProfileManager.createS3UserProfileAttachmentToken(adminUserInfo, "Superman", testToken);
//	}	
//	@Test
//	public void testAdminGetPresignedUrl() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
//		// Simulate an admin trying to access a presigned url
//		Long tokenId = new Long(456);
//		String profileId = "132";
//		PresignedUrl expectedPreSignedUrl = new PresignedUrl();
//		expectedPreSignedUrl.setPresignedUrl("I am a presigned url! whooot!");
//		when(mockUserManager.getUserInfo(userId)).thenReturn(adminUserInfo);
//		when(mockS3TokenManager.getAttachmentUrl(userId, profileId, tokenId.toString())).thenReturn(expectedPreSignedUrl);
//		// Make the actual call
//		PresignedUrl url = userProfileManager.getUserProfileAttachmentUrl(userId, profileId, tokenId.toString());
//		assertNotNull(url);
//		assertEquals(expectedPreSignedUrl.getPresignedUrl(), url.getPresignedUrl());
//	}	
//	@Test
//	public void testOwnerGetPresignedUrl() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
//		// Simulate an normal user trying to access a presigned url
//		Long tokenId = new Long(456);
//		String profileId = "132";
//		PresignedUrl expectedPreSignedUrl = new PresignedUrl();
//		expectedPreSignedUrl.setPresignedUrl("I am a presigned url! whooot!");
//		when(mockUserManager.getUserInfo(userId)).thenReturn(adminUserInfo);
//		when(mockS3TokenManager.getAttachmentUrl(adminUserId, profileId, tokenId.toString())).thenReturn(expectedPreSignedUrl);
//		// Make the actual call
//		PresignedUrl url = userProfileManager.getUserProfileAttachmentUrl(adminUserId, profileId, tokenId.toString());
//		assertNotNull(url);
//		assertEquals(expectedPreSignedUrl.getPresignedUrl(), url.getPresignedUrl());
//	}
//	@Test
//	public void testNonOwnerGetPresignedUrl() throws NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
//		// Simulate an normal user trying to access a presigned url
//		Long tokenId = new Long(456);
//		String profileId = "132";
//		Long otherUserId = 209384L;
//		
//		PresignedUrl expectedPreSignedUrl = new PresignedUrl();
//		expectedPreSignedUrl.setPresignedUrl("I am a presigned url! whooot!");
//		when(mockUserManager.getUserInfo(userId)).thenReturn(adminUserInfo);
//		when(mockS3TokenManager.getAttachmentUrl(otherUserId, profileId, tokenId.toString())).thenReturn(expectedPreSignedUrl);
//		
//		// Make the actual call
//		PresignedUrl url = userProfileManager.getUserProfileAttachmentUrl(otherUserId, profileId, tokenId.toString());
//		assertNotNull(url);
//		assertEquals(expectedPreSignedUrl.getPresignedUrl(), url.getPresignedUrl());
//	}	
	
	@Test
	public void testUpdateProfileFileHandleAuthrorized() throws NotFoundException{
		String fileHandleId = "123";
		when(mockAuthorizationManager.canAccessRawFileHandleById(userInfo, fileHandleId)).thenReturn(new AuthorizationStatus(true, null));
		UserProfile profile = new UserProfile();
		profile.setOwnerId(""+userInfo.getId());
		profile.setUserName("some username");
		profile.setProfilePicureFileHandleId(fileHandleId);
		userProfileManager.updateUserProfile(userInfo, profile);
		verify(mockProfileDAO).update(any(UserProfile.class));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testUpdateProfileFileHandleUnAuthrorized() throws NotFoundException{
		String fileHandleId = "123";
		when(mockAuthorizationManager.canAccessRawFileHandleById(userInfo, fileHandleId)).thenReturn(new AuthorizationStatus(false, "User does not own the file handle"));
		UserProfile profile = new UserProfile();
		profile.setOwnerId(""+userInfo.getId());
		profile.setUserName("some username");
		profile.setProfilePicureFileHandleId(fileHandleId);
		userProfileManager.updateUserProfile(userInfo, profile);
	}
	
	@Test
	public void testCreateFileHandleFromAttachmentNull(){
		assertEquals(null, userProfileManager.createFileHandleFromAttachment("123", null));
		assertEquals(null, userProfileManager.createFileHandleFromAttachment("123", new AttachmentData()));
	}
	
	@Test
	public void testCreateFileHandleFromAttachment(){

		// Starting attachment.
		String bucket = StackConfiguration.getS3Bucket();
		AttachmentData ad = new AttachmentData();
		ad.setTokenId("123/image.jpg");
		
		when(mockS3Client.getObjectMetadata(bucket, ad.getTokenId())).thenReturn(new ObjectMetadata());
		// Return what was passed
		when(mockFileHandleDao.createFile(any(S3FileHandle.class))).then(new Answer<FileHandle>() {
			@Override
			public FileHandle answer(InvocationOnMock invocation) throws Throwable {
				return (FileHandle) invocation.getArguments()[0];
			}
		});
		String createdBy = "007";
		S3FileHandle result = userProfileManager.createFileHandleFromAttachment(createdBy, ad);
		assertNotNull(result);
		assertEquals("image.jpg", result.getFileName());
		assertEquals(bucket, result.getBucketName());
		assertEquals(createdBy, result.getCreatedBy());
		assertNotNull(result.getCreatedOn());
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
	public void testGetOwnUserProfile() throws Exception {
		String ownerId = userInfo.getId().toString();
		UserProfile upClone = userProfileManager.getUserProfile(userInfo, ownerId);
		assertEquals(userProfile, upClone);
	}
		
	@Test
	public void getAll() throws Exception {
		UserProfile upForList = new UserProfile();
		upForList.setOwnerId(userProfile.getOwnerId());
		upForList.setRStudioUrl(userProfile.getRStudioUrl());
		upForList.setUserName(userProfile.getUserName());
		upForList.setLocation(userProfile.getLocation());
		upForList.setEmails(userProfile.getEmails());
		upForList.setOpenIds(userProfile.getOpenIds());
		
		List<UserProfile> upList = Collections.singletonList(upForList);
		when(mockProfileDAO.getInRange(0L, 1L)).thenReturn(upList);
		when(mockProfileDAO.getCount()).thenReturn(1L);
		when(mockProfileDAO.list(Collections.singleton(Long.parseLong(userProfile.getOwnerId())))).
			thenReturn(upList);

		QueryResults<UserProfile> results=userProfileManager.getInRange(adminUserInfo, 0, 1);
		assertFalse(upForList.getEmails().isEmpty());
		assertFalse(upForList.getOpenIds().isEmpty());
		assertEquals(1L, results.getTotalNumberOfResults());
		assertEquals(upList, results.getResults());
		
		IdSet ids = new IdSet();
		ids.setSet(Collections.singleton(Long.parseLong(userProfile.getOwnerId())));
		assertEquals(results.getResults(), userProfileManager.list(ids).getList());
	}
		
	@Test
	public void testGetOthersUserProfileByAdmin() throws Exception {
		String ownerId = userInfo.getId().toString();
		UserProfile upClone = userProfileManager.getUserProfile(adminUserInfo, ownerId);
		assertEquals(userProfile, upClone);
	}
	
	@Ignore
	@Test
	public void testUpdateOwnUserProfile() throws Exception {
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
	public void testUpdateOthersUserProfile() throws Exception {
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
