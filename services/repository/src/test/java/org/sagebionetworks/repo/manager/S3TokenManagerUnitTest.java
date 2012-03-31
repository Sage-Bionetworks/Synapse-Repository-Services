package org.sagebionetworks.repo.manager;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.repo.model.AuthorizationConstants.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
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

	private PermissionsManager mockPermissionsManager;
	private UserManager mockUuserManager;
	private IdGenerator mocIdGenerator;
	private LocationHelper mocKLocationHelper;
	private S3TokenManagerImpl manager;
	private UserInfo mockUser;
	
	@Before
	public void before(){
		// Create the mocks
		mockPermissionsManager = Mockito.mock(PermissionsManager.class);
		mockUuserManager = Mockito.mock(UserManager.class);
		mocIdGenerator = Mockito.mock(IdGenerator.class);
		mocKLocationHelper = Mockito.mock(LocationHelper.class);
		mockUser = Mockito.mock(UserInfo.class);
		manager = new S3TokenManagerImpl(mockPermissionsManager, mockUuserManager, mocIdGenerator, mocKLocationHelper);
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
		manager.validateUpdateAccess(userId, entityId);
		
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
		manager.validateUpdateAccess(userId, entityId);
	}

	@Test (expected=UnauthorizedException.class)
	public void testValidateReadAccessFail() throws DatastoreException, NotFoundException, UnauthorizedException{
		String userId = "123456";
		String entityId = "abc";
		// return the mock user.
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(false);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenThrow(new IllegalArgumentException("Read and not update should have been checked"));
		manager.validateReadAccess(userId, entityId);
		
	}
	
	@Test 
	public void testValidateReadAccessPass() throws DatastoreException, NotFoundException, UnauthorizedException{
		String userId = "123456";
		String entityId = "abc";
		// return the mock user.
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Say now to this
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenThrow(new IllegalArgumentException("Read and not update should have been checked"));
		manager.validateReadAccess(userId, entityId);
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
		String expectedPath = "/" + entityId+"/"+tokenId.toString();
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mocIdGenerator.generateNewId()).thenReturn(tokenId);
		Credentials mockCreds = Mockito.mock(Credentials.class);
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(true);
		when(mocKLocationHelper.createFederationTokenForS3(userId,HttpMethod.PUT,expectedPath)).thenReturn(mockCreds);
		when(mocKLocationHelper.presignS3PUTUrl(mockCreds, expectedPath, md5, "image/jpeg")).thenReturn(expectePreSigneUrl);
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
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateS3AttachmentTokenNoUpdateAccess() throws NumberFormatException, DatastoreException, NotFoundException, UnauthorizedException, InvalidModelException{
		S3AttachmentToken startToken = new S3AttachmentToken();
		startToken.setFileName(null);
		String almostMd5 = "79054025255fb1a26e4bc422aef54eb4";
		startToken.setMd5(almostMd5);
		Long tokenId = new Long(456);
		String entityId = "132";
		String userId = "007";
		String expectedPath = entityId+"/"+tokenId.toString();
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mocIdGenerator.generateNewId()).thenReturn(tokenId);
		Credentials mockCreds = Mockito.mock(Credentials.class);
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.UPDATE, mockUser)).thenReturn(false);
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
		String userId = "007";
		String expectedPath = "/" + entityId + "/"+ tokenId.toString();
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(true);
		when(mocKLocationHelper.presignS3GETUrlShortLived(userId, expectedPath)).thenReturn(expectePreSigneUrl);
		// Make the actual call
		PresignedUrl url = manager.getAttachmentUrl(userId, entityId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectePreSigneUrl, url.getPresignedUrl());
	}
	

	@Test (expected=UnauthorizedException.class)
	public void testGetAttachmentUrlNoReadAccess() throws Exception{
		Long tokenId = new Long(456);
		String entityId = "132";
		String userId = "007";
		String expectedPath = entityId+"/"+tokenId.toString();
		String expectePreSigneUrl = "I am a presigned url! whooot!";
		when(mockUuserManager.getUserInfo(userId)).thenReturn(mockUser);
		// Simulate a 
		when(mockPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, mockUser)).thenReturn(false);
		when(mocKLocationHelper.presignS3GETUrl(userId, expectedPath)).thenReturn(expectePreSigneUrl);
		// Make the actual call
		PresignedUrl url = manager.getAttachmentUrl(userId, entityId, tokenId.toString());
		assertNotNull(url);
		assertEquals(expectePreSigneUrl, url.getPresignedUrl());
	}
}
