package org.sagebionetworks.repo.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.attachment.URLStatus;
import org.sagebionetworks.repo.util.LocationHelper;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.http.HttpMethod;

import com.amazonaws.services.securitytoken.model.Credentials;

/**
 * A unit test for the S3TokenManager
 * 
 * @author John
 *
 */
public class S3TokenManagerUnitTest {

	private EntityPermissionsManager mockPermissionsManager;
	private UserManager mockUuserManager;
	private IdGenerator mocIdGenerator;
	private LocationHelper mocKLocationHelper;
	private S3TokenManagerImpl manager;
	private AmazonS3Utility mockS3Utilitiy;
	private UserInfo mockUser;
	String userId = "007";
	
	@Before
	public void before(){
		// Create the mocks
		mockPermissionsManager = Mockito.mock(EntityPermissionsManager.class);
		mockUuserManager = Mockito.mock(UserManager.class);
		mocIdGenerator = Mockito.mock(IdGenerator.class);
		mocKLocationHelper = Mockito.mock(LocationHelper.class);
		mockS3Utilitiy = Mockito.mock(AmazonS3Utility.class);
		mockUser = new UserInfo(false);
		mockUser.setUser(new User());
		mockUser.getUser().setId(userId);
		manager = new S3TokenManagerImpl(mockPermissionsManager, mockUuserManager, mocIdGenerator, mocKLocationHelper, mockS3Utilitiy);
	}

	@Test (expected=InvalidModelException.class)
	public void testValidateMd5Invalid() throws InvalidModelException{
		String notAnMd5AtAll = "not an md5";
		manager.validateMd5(notAnMd5AtAll);
	}
	
	/**
	 * This is a valid MD5
	 * @throws InvalidModelException
	 */
	@Test
	public void testValidateMd5Valid() throws InvalidModelException{
		String md5 = "79054025255fb1a26e4bc422aef54eb4";
		manager.validateMd5(md5);
	}
	
	@Test
	public void testValidateContentTypeXLS(){
		String expectedType = "application/binary";
		String type = manager.validateContentType("SomeFile.xls");
		assertEquals(expectedType, type);
	}
	
	@Test
	public void testValidateContentTypeTxt(){
		String expectedType = "text/plain";
		String type = manager.validateContentType("SomeFile.txt");
		assertEquals(expectedType, type);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testValidateUpdateAccessFail() throws DatastoreException, NotFoundException, UnauthorizedException{
		String userId = "123456";
		String entityId = "abc";
		// return the mock user.
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(false);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenThrow(new IllegalArgumentException("Update and not read should have been checked"));
		manager.validateUpdateAccess(mockUser, entityId);
		
	}
	
	@Test 
	public void testValidateUpdateAccessPass() throws DatastoreException, NotFoundException, UnauthorizedException{
		String userId = "123456";
		String entityId = "abc";
		// return the mock user.
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(true);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenThrow(new IllegalArgumentException("Update and not read should have been checked"));
		manager.validateUpdateAccess(mockUser, entityId);
	}
	
	@Test
	public void testCreateS3AttachmentToken() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName("SomeFile.jpg");
		String md5 = "79054025255fb1a26e4bc422aef54eb4";
		startToken.setMd5(md5);
		Long tokenId = new Long(456);
		String entityId = "132";
		String userId = "007";
		String expectedPath = S3TokenManagerImpl.createAttachmentPathSlash(entityId, tokenId.toString());

		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mocIdGenerator.generateNewId()).thenReturn(tokenId);
		Credentials mockCreds = Mockito.mock(Credentials.class);
		when(mockUuserManager.getUserInfo(any(String.class))).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(true);
		when(mocKLocationHelper.createFederationTokenForS3(userId,HttpMethod.PUT,expectedPath)).thenReturn(mockCreds);
		when(mocKLocationHelper.presignS3PUTUrl(any(Credentials.class), any(String.class), any(String.class), any(String.class))).thenReturn(expectePreSigneUrl);
		// Make the actual call
		S3AttachmentToken endToken = manager.createS3AttachmentToken(userId, entityId, startToken);
		assertNotNull(endToken);
		assertEquals(expectePreSigneUrl, endToken.getPresignedUrl());
	}
	
	@Test (expected=InvalidModelException.class)
	public void testCreateS3AttachmentTokenBadMD5() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName("SomeFile.jpg");
		String almostMd5 = "79054025255fb1a26e4bc422aef";
		startToken.setMd5(almostMd5);
		Long tokenId = new Long(456);
		String entityId = "132";
		String userId = "007";
		String expectedPath = entityId+"/"+tokenId.toString();
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mocIdGenerator.generateNewId()).thenReturn(tokenId);
		Credentials mockCreds = Mockito.mock(Credentials.class);
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(true);
		when(mocKLocationHelper.createFederationTokenForS3(userId,HttpMethod.PUT,expectedPath)).thenReturn(mockCreds);
		when(mocKLocationHelper.presignS3PUTUrl(mockCreds, expectedPath, almostMd5, "image/jpeg")).thenReturn(expectePreSigneUrl);
		// Make the actual call
		S3AttachmentToken endToken = manager.createS3AttachmentToken(userId, entityId, startToken);
		assertNotNull(endToken);
		assertEquals(expectePreSigneUrl, endToken.getPresignedUrl());
	}
	
	@Test
	public void testGetAttachmentUrl() throws Exception{
		Long tokenId = new Long(456);
		String entityId = "132";
		String expectedPath = S3TokenManagerImpl.createAttachmentPathSlash(entityId, tokenId.toString());
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mocKLocationHelper.presignS3GETUrlShortLived(userId, expectedPath)).thenReturn(expectePreSigneUrl);
		when(mockS3Utilitiy.doesExist(any(String.class))).thenReturn(true);
		// Make the actual call
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		PresignedUrl url = manager.getAttachmentUrl(userId, entityId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectePreSigneUrl, url.getPresignedUrl());
		assertEquals(URLStatus.READ_FOR_DOWNLOAD, url.getStatus());
	}
	
	@Test
	public void testGetAttachmentUrlDoesNotExist() throws Exception{
		Long tokenId = new Long(456);
		String entityId = "132";
		String expectedPath = S3TokenManagerImpl.createAttachmentPathSlash(entityId, tokenId.toString());
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mocKLocationHelper.presignS3GETUrlShortLived(userId, expectedPath)).thenReturn(expectePreSigneUrl);
		// This time test that the url does not exist
		when(mockS3Utilitiy.doesExist(any(String.class))).thenReturn(false);
		// Make the actual call
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		PresignedUrl url = manager.getAttachmentUrl(userId, entityId, tokenId.toString());
		assertNotNull(url);
		assertEquals(null, url.getPresignedUrl());
		assertEquals(URLStatus.DOES_NOT_EXIST, url.getStatus());
	}
	
	@Test
	public void testCreateTokenId(){
		Long id = new Long(456);
		String fileName = "image.jpg";
		String tokenId = S3TokenManagerImpl.createTokenId(id, fileName);
		assertEquals("456/image.jpg", tokenId);
	}
	
	@Test
	public void testCreateTokenIdMultiDot(){
		Long id = new Long(456);
		String fileName = "catalina.2011-05-16.log";
		String tokenId = S3TokenManagerImpl.createTokenId(id, fileName);
		assertEquals("456/catalina.2011-05-16.log", tokenId);
	}
	
	@Test
	public void testCreateTokenIdSpaces(){
		Long id = new Long(456);
		String fileName = "i have spaces.log";
		String tokenId = S3TokenManagerImpl.createTokenId(id, fileName);
		assertEquals("456/i_have_spaces.log", tokenId);
	}
	
	@Test
	public void testCreateTokenIdInvalidChars(){
		Long id = new Long(456);
		String fileName = "i have~!@#$%^&*()_+{}|/\\spaces.log";
		String tokenId = S3TokenManagerImpl.createTokenId(id, fileName);
		assertEquals("456/i_have________*_________spaces.log", tokenId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateTokenIdNullId(){
		String fileName = "image.jpg";
		String tokenId = S3TokenManagerImpl.createTokenId(null, fileName);
		assertEquals("456.jpg", tokenId);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateTokenIdNullName(){
		Long id = new Long(456);
		String tokenId = S3TokenManagerImpl.createTokenId(id, null);
		assertEquals("456.jpg", tokenId);
	}
	
	public void testCreateTokenIdNoSuffix(){
		Long id = new Long(456);
		String fileName = "image";
		String tokenId = S3TokenManagerImpl.createTokenId(id, fileName);
		assertEquals("456/image", tokenId);
	}
}
