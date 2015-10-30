package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.file.transfer.FileTransferStrategy;
import org.sagebionetworks.repo.manager.file.transfer.TransferRequest;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Mimetypes;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.StringInputStream;

/**
 * A unit test for the FileUploadManagerImpl.
 * 
 * @author jmhill
 *
 */
public class FileHandleManagerImplTest {
	
	FileHandleManagerImpl manager;
	FileItemIterator mockIterator;
	FileItemStream mockFileStream;
	FileItemStream mockParamParentIdStream;
	UserInfo mockUser;
	FileHandleDao mockfileMetadataDao;
	FileTransferStrategy mockPrimaryStrategy;
	FileTransferStrategy mockFallbackStrategy;
	S3FileHandle validResults;
	AmazonS3Client mockS3Client;
	AuthorizationManager mockAuthorizationManager;
	StorageLocationDAO mockStorageLocationDao;
	FileHandleAuthorizationManager mockFileHandleAuthorizationManager;
	
	String bucket;
	String key;
	String md5;
	Long fileSize;
	Long storageLocationId;
	// setup a storage location
	ExternalS3StorageLocationSetting externalS3StorageLocationSetting;
	S3FileHandle externals3FileHandle;
	
	
	@Before
	public void before() throws UnsupportedEncodingException, IOException, NoSuchAlgorithmException{
		mockIterator = Mockito.mock(FileItemIterator.class);
		mockfileMetadataDao = Mockito.mock(FileHandleDao.class);
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		mockAuthorizationManager = Mockito.mock(AuthorizationManager.class);
		mockStorageLocationDao = Mockito.mock(StorageLocationDAO.class);
		mockFileHandleAuthorizationManager = Mockito.mock(FileHandleAuthorizationManager.class);
		
		// The user is not really a mock
		mockUser = new UserInfo(false,"987");
		
		// Other helper mocks
		// First mock a file stream
		mockFileStream = Mockito.mock(FileItemStream.class);
		// This file stream is actually a file and not a parameter
		String contentType = "text/plain";
		String fileName = "someTextFile.txt";
		String contentString = "I am a very short string";
		byte[] contentBytes = contentString.getBytes();
		String contentMD5 = BinaryUtils.toHex((MessageDigest.getInstance("MD5").digest(contentBytes)));
		
		when(mockFileStream.isFormField()).thenReturn(false);
		when(mockFileStream.openStream()).thenReturn(new StringInputStream(contentString));
		when(mockFileStream.getContentType()).thenReturn(contentType);
		when(mockFileStream.getName()).thenReturn(fileName);
		when(mockFileStream.getFieldName()).thenReturn("file");
		// Mock a parameter as a stream.
		mockParamParentIdStream = Mockito.mock(FileItemStream.class);
		// This file stream is actually a file and not a parameter
		when(mockParamParentIdStream.isFormField()).thenReturn(true);
		when(mockParamParentIdStream.openStream()).thenReturn(new StringInputStream("syn123"));
		when(mockParamParentIdStream.getContentType()).thenReturn(null);
		when(mockParamParentIdStream.getName()).thenReturn(null);
		when(mockParamParentIdStream.getFieldName()).thenReturn("parentId");
		
		mockPrimaryStrategy = Mockito.mock(FileTransferStrategy.class);
		mockFallbackStrategy = Mockito.mock(FileTransferStrategy.class);
		// setup the primary to succeed
		validResults = new S3FileHandle();
		validResults.setId("123");
		validResults.setCreatedBy(mockUser.getId().toString());
		validResults.setCreatedOn(new Date());
		validResults.setContentType(contentType);
		validResults.setContentSize(new Long(contentBytes.length));
		validResults.setContentMd5(contentMD5);
		validResults.setFileName(fileName);
		validResults.setBucketName("bucket");
		validResults.setKey("key");
		
		bucket = "some-bucket";
		key = "some-key";
		md5 = "some-md5";
		fileSize = 103L;
		storageLocationId = 987L;
		// setup a storage location
		externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBucket(bucket);
		externalS3StorageLocationSetting.setCreatedBy(mockUser.getId());
		when(mockStorageLocationDao.get(storageLocationId)).thenReturn(externalS3StorageLocationSetting);
		ObjectMetadata mockMeta = Mockito.mock(ObjectMetadata.class);
		when(mockS3Client.getObjectMetadata(bucket, key)).thenReturn(mockMeta);
		when(mockMeta.getETag()).thenReturn(md5);
		when(mockMeta.getContentLength()).thenReturn(fileSize);
		
		externals3FileHandle = new S3FileHandle();
		externals3FileHandle.setBucketName(bucket);
		externals3FileHandle.setKey(key);
		externals3FileHandle.setStorageLocationId(storageLocationId);
		
		when(mockfileMetadataDao.createFile(externals3FileHandle)).thenReturn(externals3FileHandle);
		
		// the manager to test.
		manager = new FileHandleManagerImpl(
				mockfileMetadataDao, mockPrimaryStrategy, 
				mockFallbackStrategy, mockAuthorizationManager, 
				mockS3Client, mockFileHandleAuthorizationManager);
		ReflectionTestUtils.setField(manager, "storageLocationDAO", mockStorageLocationDao);
	}
	
	@Test (expected=IllegalStateException.class)
	public void testPrimaryStrategyNull() throws Exception{
		manager.setPrimaryStrategy(null);
		FileUploadResults results = manager.uploadfiles(mockUser, new HashSet<String>(), mockIterator);
	}
	
	@Test (expected=IllegalStateException.class)
	public void testFallbacStrategyNull() throws Exception{
		manager.setFallbackStrategy(null);
		FileUploadResults results = manager.uploadfiles(mockUser, new HashSet<String>(), mockIterator);
	}
	
	/**
	 * Test that finding a file stream without the required parameters triggers an exception.
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws ServiceUnavailableException 
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testUploadfilesMissingExpectedParams() throws FileUploadException, IOException, ServiceUnavailableException{
		// These are the parameters we expect to find before reading a file
		HashSet<String> expectedParams = new HashSet<String>();
		expectedParams.add("parentId");
		// For this test the parent id parameter is missing
		when(mockIterator.hasNext()).thenReturn(true, false);
		when(mockIterator.next()).thenReturn(mockFileStream);
		manager.uploadfiles(mockUser, expectedParams, mockIterator);
	}
	
	/**
	 * For this test all of the expected parameters are provided, and the primary strategy works.
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws ServiceUnavailableException 
	 */
	@Test
	public void testUploadfilesWithExpectedParams() throws FileUploadException, IOException, ServiceUnavailableException{
		// These are the parameters we expect to find before reading a file
		HashSet<String> expectedParams = new HashSet<String>();
		expectedParams.add("parentId");
		// For this test the parent id parameter is missing
		when(mockIterator.hasNext()).thenReturn(true, true, false);
		// return the parentId param before the file.
		when(mockIterator.next()).thenReturn(mockParamParentIdStream, mockFileStream);
		// set the primary to fire.
		when(mockPrimaryStrategy.transferToS3(any(TransferRequest.class))).thenReturn(validResults);
		// Set the secondary to throw an exception
		when(mockFallbackStrategy.transferToS3(any(TransferRequest.class))).thenThrow(new ServiceUnavailableException());
		when(mockfileMetadataDao.createFile(validResults)).thenReturn(validResults);
		FileUploadResults results = manager.uploadfiles(mockUser, expectedParams, mockIterator);
		assertNotNull(results);
		assertNotNull(results.getParameters());
		assertNotNull(results.getFiles());
		// Was parentId extracted as expected?
		assertEquals("syn123", results.getParameters().get("parentId"));
		// Do we have one file
		assertEquals(1, results.getFiles().size());
		S3FileHandle fileData = results.getFiles().get(0);
		assertEquals("text/plain", fileData.getContentType());
		assertNotNull(fileData.getKey());
		assertNotNull(fileData.getBucketName());
		
		verify(mockfileMetadataDao, times(1)).createFile(validResults);
		verify(mockPrimaryStrategy, times(1)).transferToS3(any(TransferRequest.class));
		// the fall back should not have been called.
		verify(mockFallbackStrategy, never()).transferToS3(any(TransferRequest.class));
	}
	
	@Test
	public void testFallbackStragegy() throws FileUploadException, IOException, ServiceUnavailableException{
		// These are the parameters we expect to find before reading a file
		HashSet<String> expectedParams = new HashSet<String>();
		when(mockIterator.hasNext()).thenReturn(true, false);
		// return the parentId param before the file.
		when(mockIterator.next()).thenReturn(mockFileStream);
		// Set the primary to fail.
		when(mockPrimaryStrategy.transferToS3(any(TransferRequest.class))).thenThrow(new ServiceUnavailableException());
		// Set the secondary to work
		when(mockFallbackStrategy.transferToS3(any(TransferRequest.class))).thenReturn(validResults);
		when(mockfileMetadataDao.createFile(validResults)).thenReturn(validResults);
		FileUploadResults results = manager.uploadfiles(mockUser, expectedParams, mockIterator);
		assertNotNull(results);
		assertNotNull(results.getParameters());
		assertNotNull(results.getFiles());
		// Do we have one file
		assertEquals(1, results.getFiles().size());
		S3FileHandle fileData = results.getFiles().get(0);
		assertEquals("text/plain", fileData.getContentType());
		assertNotNull(fileData.getKey());
		assertNotNull(fileData.getBucketName());
		
		verify(mockfileMetadataDao, times(1)).createFile(validResults);
		verify(mockPrimaryStrategy, times(1)).transferToS3(any(TransferRequest.class));
		// the fall back should have been called this time
		verify(mockFallbackStrategy,times(1)).transferToS3(any(TransferRequest.class));
	}
	
	@Test (expected=ServiceUnavailableException.class)
	public void testBothStrategiesFailed() throws FileUploadException, IOException, ServiceUnavailableException{
		// These are the parameters we expect to find before reading a file
		HashSet<String> expectedParams = new HashSet<String>();
		when(mockIterator.hasNext()).thenReturn(true, false);
		// return the parentId param before the file.
		when(mockIterator.next()).thenReturn(mockFileStream);
		// Set the primary to fail.
		when(mockPrimaryStrategy.transferToS3(any(TransferRequest.class))).thenThrow(new ServiceUnavailableException());
		// Set the fallback to fail.
		when(mockFallbackStrategy.transferToS3(any(TransferRequest.class))).thenThrow(new ServiceUnavailableException());
		when(mockfileMetadataDao.createFile(validResults)).thenReturn(validResults);
		FileUploadResults results = manager.uploadfiles(mockUser, expectedParams, mockIterator);
	}
	@Test
	public void testCreateMetadata() throws UnsupportedEncodingException{
		// Create the metadata
		InputStream stream = new StringInputStream("stream");
		TransferRequest metadata = FileHandleManagerImpl.createRequest(Mimetypes.MIMETYPE_OCTET_STREAM, "123", "testCreateMetadata", stream);
		assertNotNull(metadata);
		assertEquals(StackConfiguration.getS3Bucket(), metadata.getS3bucketName());
		assertEquals(Mimetypes.MIMETYPE_OCTET_STREAM, metadata.getContentType());
		assertNotNull(metadata.getS3key());
		assertTrue(metadata.getS3key().startsWith("123/"));
		assertEquals(stream, metadata.getInputStream());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetFileHandleUnAuthrozied() throws DatastoreException, NotFoundException{
		// You must be authorized to see a file handle
		String handleId = "123";
		when(mockfileMetadataDao.get(handleId)).thenReturn(validResults);
		// denied!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.getRawFileHandle(mockUser, handleId);
	}
	
	@Test
	public void testGetFileHandleAuthrozied() throws DatastoreException, NotFoundException{
		// You must be authorized to see a file handle
		String handleId = "123";
		when(mockfileMetadataDao.get(handleId)).thenReturn(validResults);
		// allow
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		FileHandle handle = manager.getRawFileHandle(mockUser, handleId);
		assertEquals("failed to get the handle", handle, validResults);
	}
	
	@Test
	public void testDeleteNotFound() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockfileMetadataDao.get(handleId)).thenThrow(new NotFoundException());
		manager.deleteFileHandle(mockUser, handleId);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testDeleteUnAuthorzied() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockfileMetadataDao.get(handleId)).thenReturn(validResults);
		// denied!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.deleteFileHandle(mockUser, handleId);
	}
	
	@Test
	public void testDeleteAuthorzied() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockfileMetadataDao.get(handleId)).thenReturn(validResults);
		// allow!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		manager.deleteFileHandle(mockUser, handleId);
		// The S3 file should get deleted.
		verify(mockS3Client, times(1)).deleteObject(validResults.getBucketName(), validResults.getKey());
		// The database handle should be deleted.
		verify(mockfileMetadataDao, times(1)).delete(handleId);
	}
	
	@Test
	public void testDeleteFileHandleDisablePreview() throws Exception {
		// Deleting a file handle that has previews disabled should not StackOverflow :)
		validResults.setPreviewId(validResults.getId());
		when(mockfileMetadataDao.get(validResults.getId())).thenReturn(validResults);
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, validResults.getId(), validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		manager.deleteFileHandle(mockUser, validResults.getId());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testClearPreviewUnauthroized() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockfileMetadataDao.get(handleId)).thenReturn(validResults);
		// denied!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.clearPreview(mockUser, handleId);
	}
	
	@Test
	public void testClearPreviewAuthorzied() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockfileMetadataDao.get(handleId)).thenReturn(validResults);
		// allow!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		manager.clearPreview(mockUser, handleId);
		// The database reference to the preview handle should be cleared
		verify(mockfileMetadataDao, times(1)).setPreviewId(eq(handleId), eq((String)null));
	}
	
	
	@Test
	public void testDeleteWithPreview() throws DatastoreException, NotFoundException{
		// Test deleting a file with a preview
		PreviewFileHandle preview = new PreviewFileHandle();
		preview.setId("456");
		preview.setCreatedBy(validResults.getCreatedBy());
		preview.setBucketName("previewBucket");
		preview.setKey("previewKey");
		// Assign the preview to the file
		validResults.setPreviewId(preview.getId());
		when(mockfileMetadataDao.get(validResults.getId())).thenReturn(validResults);
		when(mockfileMetadataDao.get(preview.getId())).thenReturn(preview);
		// Allow all calls
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(any(UserInfo.class), anyString(), any(String.class))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// Now deleting the original handle should trigger the delete of the previews.
		manager.deleteFileHandle(mockUser, validResults.getId());
		// The S3 file should get deleted.
		verify(mockS3Client, times(1)).deleteObject(validResults.getBucketName(), validResults.getKey());
		// The database handle should be deleted.
		verify(mockfileMetadataDao, times(1)).delete(validResults.getId());
		// The S3 file for the preview should get deleted.
		verify(mockS3Client, times(1)).deleteObject(preview.getBucketName(), preview.getKey());
		// The database handle of the preview should be deleted.
		verify(mockfileMetadataDao, times(1)).delete(preview.getId());
	}
	
	@Test
	public void testGetRedirectURLForFileHandleExternal() throws DatastoreException, NotFoundException, MalformedURLException{
		ExternalFileHandle external = new ExternalFileHandle();
		external.setId("123");
		external.setExternalURL("http://google.com");
		when(mockfileMetadataDao.get(external.getId())).thenReturn(external);
		// fire!
		String redirect = manager.getRedirectURLForFileHandle(external.getId());
		assertNotNull(redirect);
		assertEquals(external.getExternalURL(), redirect.toString());
	}
	
	@Test
	public void testGetRedirectURLForFileHandleS3() throws DatastoreException, NotFoundException, MalformedURLException{
		S3FileHandle s3FileHandle = new S3FileHandle();
		s3FileHandle.setId("123");
		s3FileHandle.setBucketName("bucket");
		s3FileHandle.setKey("key");
		when(mockfileMetadataDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		String expectedURL = "https://amamzon.com";
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).
			thenReturn(new URL(expectedURL));
		// fire!
		String redirect = manager.getRedirectURLForFileHandle(s3FileHandle.getId());
		assertNotNull(redirect);
		assertEquals(expectedURL, redirect.toString());
	}
	
	@Test
	public void testGetRedirectURLForFileHandleS3WithFileNameOverride() throws DatastoreException, NotFoundException, MalformedURLException{
		S3FileHandle s3FileHandle = new S3FileHandle();
		s3FileHandle.setId("123");
		s3FileHandle.setBucketName("bucket");
		s3FileHandle.setKey("key");
		s3FileHandle.setFileName("we will not use this one.jpg");
		when(mockfileMetadataDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		String expectedURL = "https://amamzon.com";
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).
			thenReturn(new URL(expectedURL));
		// fire!
		String redirect = manager.getRedirectURLForFileHandle(s3FileHandle.getId(), "foo.txt");
		assertNotNull(redirect);
		assertEquals(expectedURL, redirect.toString());
		ArgumentCaptor<GeneratePresignedUrlRequest> gpuRequest = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
		verify(mockS3Client).generatePresignedUrl(gpuRequest.capture());
		assertEquals("attachment; filename=foo.txt", 
				gpuRequest.getValue().getResponseHeaders().getContentDisposition());
	}
		
	@Test
	public void testCreateExternalFileHappyCase() throws Exception{
		ExternalFileHandle efh = createFileHandle();
		when(mockfileMetadataDao.createFile(efh)).thenReturn(efh);
		// This should work
		ExternalFileHandle result = manager.createExternalFileHandle(mockUser, efh);
		assertNotNull(result);
		assertEquals(mockUser.getId().toString(), result.getCreatedBy());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalFileHandleNullUser(){
		ExternalFileHandle efh = createFileHandle();
		manager.createExternalFileHandle(null, efh);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalFileHandleNullHandle(){
		manager.createExternalFileHandle(mockUser, null);
	}
	
	public void testCreateExternalFileHandleNullFileName(){
		ExternalFileHandle efh = createFileHandle();
		efh.setFileName(null);
		// This should not fail.
		manager.createExternalFileHandle(mockUser, efh);
	}
	
	public void testCreateExternalFileHandleNullContentType(){
		ExternalFileHandle efh = createFileHandle();
		efh.setContentType(null);
		// This should not fail.
		manager.createExternalFileHandle(mockUser, efh);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalFileHandleNullURL(){
		ExternalFileHandle efh = createFileHandle();
		efh.setExternalURL(null);
		manager.createExternalFileHandle(mockUser, efh);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalFileHandleMalformedURL(){
		ExternalFileHandle efh = createFileHandle();
		efh.setExternalURL("local");
		manager.createExternalFileHandle(mockUser, efh);
	}
	
	@Test
	public void testGetURLAuthorized() throws Exception{
		S3FileHandle s3FileHandle = new S3FileHandle();
		s3FileHandle.setId("123");
		s3FileHandle.setCreatedBy("456");
		s3FileHandle.setBucketName("bucket");
		s3FileHandle.setKey("key");
		when(mockfileMetadataDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		String expecedURL = "https://amamzon.com";
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL(expecedURL));
		when(mockAuthorizationManager.isUserCreatorOrAdmin(mockUser, s3FileHandle.getCreatedBy())).thenReturn(true);
		String redirect = manager.getRedirectURLForFileHandle(mockUser, s3FileHandle.getId());
		assertEquals(expecedURL, redirect);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetURLUnauthorized() throws Exception{
		S3FileHandle s3FileHandle = new S3FileHandle();
		s3FileHandle.setId("123");
		s3FileHandle.setCreatedBy("456");
		s3FileHandle.setBucketName("bucket");
		s3FileHandle.setKey("key");
		when(mockfileMetadataDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		String expecedURL = "https://amamzon.com";
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL(expecedURL));
		when(mockAuthorizationManager.isUserCreatorOrAdmin(mockUser, s3FileHandle.getCreatedBy())).thenReturn(false);
		String redirect = manager.getRedirectURLForFileHandle(mockUser, s3FileHandle.getId());
		assertEquals(expecedURL, redirect);
	}
	
	
	@Test
	public void testGetURLforAssociatedFileAuthorized() throws Exception{
		S3FileHandle s3FileHandle = new S3FileHandle();
		s3FileHandle.setId("123");
		s3FileHandle.setCreatedBy("456");
		s3FileHandle.setBucketName("bucket");
		s3FileHandle.setKey("key");
		when(mockfileMetadataDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		String expectedURL = "https://amazon.com";
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL(expectedURL));
		FileHandleAssociation association = new FileHandleAssociation();
		String associateObjectId = "999";
		association.setAssociateObjectId(associateObjectId);
		association.setAssociateObjectType(FileHandleAssociateType.VerificationSubmission);
		association.setFileHandleId(s3FileHandle.getId());
		List<FileHandleAssociation> associations = Collections.singletonList(association);
		FileHandleAssociationAuthorizationStatus authorizationResult = 
				new FileHandleAssociationAuthorizationStatus(
				association, AuthorizationManagerUtil.AUTHORIZED);

		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).
		thenReturn(Collections.singletonList(authorizationResult));
		
		// method under test
		String redirect = manager.getRedirectURLForFileHandle(mockUser,
				s3FileHandle.getId(), FileHandleAssociateType.VerificationSubmission, associateObjectId);
		
		// the manager returns the redirect URL, no exception thrown
		assertEquals(expectedURL, redirect);
		
		// now make it unauthorized
		authorizationResult.setStatus(AuthorizationManagerUtil.ACCESS_DENIED);
		try {
			 manager.getRedirectURLForFileHandle(mockUser,
						s3FileHandle.getId(), FileHandleAssociateType.VerificationSubmission, associateObjectId);;
			fail("Exception expected");
		} catch (UnauthorizedException e) {
			// as expected
		}
	}
	
	
	@Test
	public void testCreateExternalS3FileHandleHappy(){
		// call under test
		S3FileHandle result = manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
		assertNotNull(result);
		assertEquals(mockUser.getId().toString(), result.getCreatedBy());
		assertNotNull(result.getCreatedOn());
		assertEquals(md5, result.getContentMd5());
		assertEquals(fileSize, result.getContentSize());
		assertNotNull(result.getEtag());
		assertEquals(bucket, result.getBucketName());
		assertEquals(key, result.getKey());
		assertEquals(storageLocationId, result.getStorageLocationId());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateExternalS3FileHandleUnauthorized(){
		// In this case the esl created by does not match the caller.
		externalS3StorageLocationSetting.setCreatedBy(mockUser.getId()+1);
		// should fails since the user is not the creator of the storage location.
		S3FileHandle result = manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalS3FileHandleWongStorageType(){
		when(mockStorageLocationDao.get(storageLocationId)).thenReturn(new S3StorageLocationSetting());
		// should fail
		S3FileHandle result = manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalS3FileHandleNullBucket(){
		externals3FileHandle.setBucketName(null);
		// should fail
		S3FileHandle result = manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalS3FileHandleNullKey(){
		externals3FileHandle.setKey(null);
		// should fail
		S3FileHandle result = manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalS3FileHandleNullStorageId(){
		externals3FileHandle.setStorageLocationId(null);
		// should fail
		S3FileHandle result = manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalS3FileHandleBucketDoesNotMatchLocation(){
		// must match the storage location bucket.
		externalS3StorageLocationSetting.setBucket(bucket);
		externals3FileHandle.setBucketName(bucket+"no-match");
		// should fail
		S3FileHandle result = manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalS3FileHandleS3Error(){
		when(mockS3Client.getObjectMetadata(bucket, key)).thenThrow(new AmazonClientException("Something is wrong"));
		// should fail
		S3FileHandle result = manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
	}
	
	@Test
	public void testCreateS3FileHandleCopy() {
		when(mockfileMetadataDao.get("123")).thenReturn(createS3FileHandle());
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "987"))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		manager.createS3FileHandleCopy(mockUser, "123", "newname.png", "image");

		ArgumentCaptor<S3FileHandle> copy = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockfileMetadataDao).createFile(copy.capture());
		assertEquals("bucket", copy.getValue().getBucketName());
		assertEquals("key", copy.getValue().getKey());
		assertEquals("newname.png", copy.getValue().getFileName());
		assertEquals("image", copy.getValue().getContentType());
		assertNull(copy.getValue().getId());
		assertEquals(mockUser.getId().toString(), copy.getValue().getCreatedBy());
	}

	@Test
	public void testCreateS3FileHandleCopyOnlyName() {
		when(mockfileMetadataDao.get("123")).thenReturn(createS3FileHandle());
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "987"))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		manager.createS3FileHandleCopy(mockUser, "123", "newname.png", null);

		ArgumentCaptor<S3FileHandle> copy = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockfileMetadataDao).createFile(copy.capture());
		assertEquals("bucket", copy.getValue().getBucketName());
		assertEquals("key", copy.getValue().getKey());
		assertEquals("newname.png", copy.getValue().getFileName());
		assertEquals("text", copy.getValue().getContentType());
		assertNull(copy.getValue().getId());
		assertEquals(mockUser.getId().toString(), copy.getValue().getCreatedBy());
	}

	@Test
	public void testCreateS3FileHandleCopyOnlyContentType() {
		when(mockfileMetadataDao.get("123")).thenReturn(createS3FileHandle());
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "987"))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		manager.createS3FileHandleCopy(mockUser, "123", null, "image");

		ArgumentCaptor<S3FileHandle> copy = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockfileMetadataDao).createFile(copy.capture());
		assertEquals("bucket", copy.getValue().getBucketName());
		assertEquals("key", copy.getValue().getKey());
		assertEquals("original.txt", copy.getValue().getFileName());
		assertEquals("image", copy.getValue().getContentType());
		assertNull(copy.getValue().getId());
		assertEquals(mockUser.getId().toString(), copy.getValue().getCreatedBy());
	}

	@Test
	public void testCreateS3FileHandleNewPreview() {
		when(mockfileMetadataDao.get("123")).thenReturn(createS3FileHandle(), createS3FileHandle(), createS3FileHandle(),
				createS3FileHandle(), createS3FileHandle());
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "987"))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		ArgumentCaptor<S3FileHandle> copy = ArgumentCaptor.forClass(S3FileHandle.class);

		manager.createS3FileHandleCopy(mockUser, "123", null, "image");
		manager.createS3FileHandleCopy(mockUser, "123", "text.png", null);
		manager.createS3FileHandleCopy(mockUser, "123", "original.txt", "text");
		manager.createS3FileHandleCopy(mockUser, "123", "different.txt", null);
		manager.createS3FileHandleCopy(mockUser, "123", "different.txt", "text");

		verify(mockfileMetadataDao, times(5)).createFile(copy.capture());
		assertNull(copy.getAllValues().get(0).getPreviewId());
		assertNull(copy.getAllValues().get(1).getPreviewId());
		assertNotNull(copy.getAllValues().get(2).getPreviewId());
		assertNotNull(copy.getAllValues().get(3).getPreviewId());
		assertNotNull(copy.getAllValues().get(4).getPreviewId());
	}

	private S3FileHandle createS3FileHandle() {
		S3FileHandle original = new S3FileHandle();
		original.setBucketName("bucket");
		original.setKey("key");
		original.setId("123");
		original.setEtag("etag");
		original.setFileName("original.txt");
		original.setContentType("text");
		original.setCreatedBy("987");
		original.setPreviewId("789");
		return original;
	}

	@Test(expected = IllegalArgumentException.class)
	public void testCreateS3FileHandleCopyFailOnNeither() {
		manager.createS3FileHandleCopy(mockUser, "123", null, null);
	}

	@SuppressWarnings("unchecked")
	@Test(expected = NotFoundException.class)
	public void testCreateS3FileHandleCopyFailOnNotExist() {
		when(mockfileMetadataDao.get("123")).thenThrow(NotFoundException.class);
		manager.createS3FileHandleCopy(mockUser, "123", "new", null);
	}

	@Test(expected = UnauthorizedException.class)
	public void testCreateS3FileHandleCopyFailOnNotOwner() {
		S3FileHandle originalFileHandle = createS3FileHandle();
		originalFileHandle.setCreatedBy("000");
		when(mockfileMetadataDao.get("123")).thenReturn(originalFileHandle);
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "000")).thenReturn(
				AuthorizationManagerUtil.ACCESS_DENIED);

		manager.createS3FileHandleCopy(mockUser, "123", null, "image");
	}

	/**
	 * This a file handle that has all of the required fields filled in.
	 * @return
	 */
	private ExternalFileHandle createFileHandle(){
		ExternalFileHandle efh = new ExternalFileHandle();
		efh.setContentType("application/json");
		efh.setFileName("foo.bar");
		efh.setExternalURL("http://www.googl.com");
		return efh;
	}

}
