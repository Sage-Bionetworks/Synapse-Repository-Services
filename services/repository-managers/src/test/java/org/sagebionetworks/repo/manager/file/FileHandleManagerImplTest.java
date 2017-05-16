package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.file.FileHandleManagerImpl.FILE_HANDLE_COPY_RECORD_TYPE;
import static org.sagebionetworks.repo.manager.file.FileHandleManagerImpl.MAX_REQUESTS_PER_CALL;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.audit.dao.ObjectRecordBatch;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.manager.AuthorizationStatus;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.audit.ObjectRecordQueue;
import org.sagebionetworks.repo.manager.file.transfer.TransferRequest;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOStorageLocationDAOImpl;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyResult;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.FileHandleCopyResult;
import org.sagebionetworks.repo.model.file.FileResult;
import org.sagebionetworks.repo.model.file.FileResultFailureCode;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Mimetypes;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.StringInputStream;
import com.google.common.collect.Lists;

/**
 * A unit test for the FileUploadManagerImpl.
 * 
 * @author jmhill
 *
 */
public class FileHandleManagerImplTest {
	
	@Mock
	FileHandleDao mockFileHandleDao;
	@Mock
	AmazonS3Client mockS3Client;
	@Mock
	AuthorizationManager mockAuthorizationManager;
	@Mock
	StorageLocationDAO mockStorageLocationDao;
	@Mock
	FileHandleAuthorizationManager mockFileHandleAuthorizationManager;
	@Mock
	ObjectRecordQueue mockObjectRecordQueue;
	@Mock
	IdGenerator mockIdGenerator;
	@Mock
	ProjectSettingsManager mockProjectSettingsManager;

	FileHandleManagerImpl manager;
	UserInfo mockUser;
	S3FileHandle validResults;

	String bucket;
	String key;
	String md5;
	Long fileSize;
	Long storageLocationId;
	// setup a storage location
	ExternalS3StorageLocationSetting externalS3StorageLocationSetting;
	S3FileHandle externals3FileHandle;

	ProxyStorageLocationSettings proxyStorageLocationSettings;
	ProxyFileHandle externalProxyFileHandle;

	List<FileHandleAssociation> associations;
	FileHandleAssociation fha1;
	FileHandleAssociation fha2;
	FileHandleAssociation fhaMissing;
	BatchFileRequest batchRequest;
	ObjectRecord successRecord;

	@Before
	public void before() throws UnsupportedEncodingException, IOException, NoSuchAlgorithmException{
		MockitoAnnotations.initMocks(this);
		// the manager to test.
		manager = new FileHandleManagerImpl();
		ReflectionTestUtils.setField(manager, "fileHandleDao", mockFileHandleDao);
		ReflectionTestUtils.setField(manager, "authorizationManager", mockAuthorizationManager);
		ReflectionTestUtils.setField(manager, "s3Client", mockS3Client);
		ReflectionTestUtils.setField(manager, "storageLocationDAO", mockStorageLocationDao);
		ReflectionTestUtils.setField(manager, "fileHandleAuthorizationManager", mockFileHandleAuthorizationManager);
		ReflectionTestUtils.setField(manager, "objectRecordQueue", mockObjectRecordQueue);
		ReflectionTestUtils.setField(manager, "idGenerator", mockIdGenerator);
		ReflectionTestUtils.setField(manager, "projectSettingsManager", mockProjectSettingsManager);

		// The user is not really a mock
		mockUser = new UserInfo(false,"987");

		// This file stream is actually a file and not a parameter
		String contentType = "text/plain";
		String fileName = "someTextFile.txt";
		String contentString = "I am a very short string";
		byte[] contentBytes = contentString.getBytes();
		String contentMD5 = BinaryUtils.toHex((MessageDigest.getInstance("MD5").digest(contentBytes)));

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
		externals3FileHandle.setContentMd5(md5);
		
		when(mockFileHandleDao.createFile(externals3FileHandle)).thenReturn(externals3FileHandle);
		
		// proxy storage location setup.
		proxyStorageLocationSettings = new ProxyStorageLocationSettings();
		proxyStorageLocationSettings.setStorageLocationId(5555L);
		proxyStorageLocationSettings.setCreatedBy(mockUser.getId());
		when(mockStorageLocationDao.get(proxyStorageLocationSettings.getStorageLocationId())).thenReturn(proxyStorageLocationSettings);
		
		externalProxyFileHandle = new ProxyFileHandle();
		externalProxyFileHandle.setContentMd5("md5");
		externalProxyFileHandle.setContentSize(123L);
		externalProxyFileHandle.setContentType("plain/text");
		externalProxyFileHandle.setFileName("foo.bar");
		externalProxyFileHandle.setFilePath("/pathParent/pathChild");
		externalProxyFileHandle.setStorageLocationId(proxyStorageLocationSettings.getStorageLocationId());
		externalProxyFileHandle.setId("444444");
		when(mockFileHandleDao.createFile(externalProxyFileHandle)).thenReturn(externalProxyFileHandle);
		
		// one
		fha1 = new FileHandleAssociation();
		fha1.setAssociateObjectId("syn123");
		fha1.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha1.setFileHandleId("333");
		// two
		fha2 = new FileHandleAssociation();
		fha2.setAssociateObjectId("syn456");
		fha2.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		fha2.setFileHandleId("444");
		// missing
		fhaMissing = new FileHandleAssociation();
		fhaMissing.setAssociateObjectId("999");
		fhaMissing.setAssociateObjectType(FileHandleAssociateType.WikiAttachment);
		fhaMissing.setFileHandleId("555");
		associations = Lists.newArrayList(fha1, fha2, fhaMissing);
		
		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationManagerUtil.ACCESS_DENIED);
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationManagerUtil.AUTHORIZED);
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationManagerUtil.AUTHORIZED);
		List<FileHandleAssociationAuthorizationStatus> authResults = Lists.newArrayList(status1, status2, missingStatus);
		
		successRecord = FileHandleManagerImpl.createObjectRecord(mockUser.getId().toString(), fha2, 123L);
		
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).thenReturn(authResults);
		
		FileHandle fh2 = new S3FileHandle();
		fh2.setId(fha2.getFileHandleId());
		Map<String, FileHandle> handleMap = new HashMap<String, FileHandle>();
		handleMap.put(fh2.getId(), fh2);
		when(mockFileHandleDao.getAllFileHandlesBatch(any(Iterable.class))).thenReturn(handleMap);
		
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL("https", "host","/a-url"));
		
		batchRequest = new BatchFileRequest();
		batchRequest.setRequestedFiles(associations);
		batchRequest.setIncludeFileHandles(true);
		batchRequest.setIncludePreSignedURLs(true);
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
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// denied!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.getRawFileHandle(mockUser, handleId);
	}
	
	@Test
	public void testGetFileHandleAuthrozied() throws DatastoreException, NotFoundException{
		// You must be authorized to see a file handle
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// allow
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		FileHandle handle = manager.getRawFileHandle(mockUser, handleId);
		assertEquals("failed to get the handle", handle, validResults);
	}
	
	@Test
	public void testDeleteNotFound() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenThrow(new NotFoundException());
		manager.deleteFileHandle(mockUser, handleId);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testDeleteUnAuthorzied() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// denied!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.deleteFileHandle(mockUser, handleId);
	}
	
	@Test
	public void testDeleteAuthorzied() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// allow!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		manager.deleteFileHandle(mockUser, handleId);
		// The S3 file should get deleted.
		verify(mockS3Client, times(1)).deleteObject(validResults.getBucketName(), validResults.getKey());
		// The database handle should be deleted.
		verify(mockFileHandleDao, times(1)).delete(handleId);
	}
	
	@Test
	public void testDeleteFileHandleDisablePreview() throws Exception {
		// Deleting a file handle that has previews disabled should not StackOverflow :)
		validResults.setPreviewId(validResults.getId());
		when(mockFileHandleDao.get(validResults.getId())).thenReturn(validResults);
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, validResults.getId(), validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		manager.deleteFileHandle(mockUser, validResults.getId());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testClearPreviewUnauthroized() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// denied!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.ACCESS_DENIED);
		manager.clearPreview(mockUser, handleId);
	}
	
	@Test
	public void testClearPreviewAuthorzied() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// allow!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		manager.clearPreview(mockUser, handleId);
		// The database reference to the preview handle should be cleared
		verify(mockFileHandleDao, times(1)).setPreviewId(eq(handleId), eq((String)null));
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
		when(mockFileHandleDao.get(validResults.getId())).thenReturn(validResults);
		when(mockFileHandleDao.get(preview.getId())).thenReturn(preview);
		// Allow all calls
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(any(UserInfo.class), anyString(), any(String.class))).thenReturn(AuthorizationManagerUtil.AUTHORIZED);
		// Now deleting the original handle should trigger the delete of the previews.
		manager.deleteFileHandle(mockUser, validResults.getId());
		// The S3 file should get deleted.
		verify(mockS3Client, times(1)).deleteObject(validResults.getBucketName(), validResults.getKey());
		// The database handle should be deleted.
		verify(mockFileHandleDao, times(1)).delete(validResults.getId());
		// The S3 file for the preview should get deleted.
		verify(mockS3Client, times(1)).deleteObject(preview.getBucketName(), preview.getKey());
		// The database handle of the preview should be deleted.
		verify(mockFileHandleDao, times(1)).delete(preview.getId());
	}
	
	@Test
	public void testGetRedirectURLForFileHandleExternal() throws DatastoreException, NotFoundException, MalformedURLException{
		ExternalFileHandle external = new ExternalFileHandle();
		external.setId("123");
		external.setExternalURL("http://google.com");
		when(mockFileHandleDao.get(external.getId())).thenReturn(external);
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
		when(mockFileHandleDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		String expectedURL = "https://amamzon.com";
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).
			thenReturn(new URL(expectedURL));
		// fire!
		String redirect = manager.getRedirectURLForFileHandle(s3FileHandle.getId());
		assertNotNull(redirect);
		assertEquals(expectedURL, redirect.toString());
	}
	
	@Test
	public void testProxyPresignedUrl(){
		Long locationId = 123L;
		ProxyFileHandle proxyHandle = new ProxyFileHandle();
		proxyHandle.setFileName("foo.txt");
		proxyHandle.setFilePath("/path/root/child");
		proxyHandle.setStorageLocationId(locationId);
		
		ProxyStorageLocationSettings proxyLocation = new ProxyStorageLocationSettings();
		proxyLocation.setStorageLocationId(locationId);
		proxyLocation.setProxyUrl("https://host.org/");
		proxyLocation.setSecretKey("Super Secret key to sign URLs with.");
		proxyLocation.setUploadType(UploadType.SFTP);
		
		when(mockStorageLocationDao.get(locationId)).thenReturn(proxyLocation);
		
		// call under test
		String url = manager.getURLForFileHandle(proxyHandle);
		assertNotNull(url);
		assertTrue(url.startsWith("https://host.org/sftp/path/root/child?"));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testProxyPresignedUrlWrongStorageType(){
		Long locationId = 123L;
		ProxyFileHandle proxyHandle = new ProxyFileHandle();
		proxyHandle.setFileName("foo.txt");
		proxyHandle.setFilePath("/path/root/child");
		proxyHandle.setStorageLocationId(locationId);
		// wrong storage location type.
		S3StorageLocationSetting location = new S3StorageLocationSetting();
		
		when(mockStorageLocationDao.get(locationId)).thenReturn(location);
		
		// call under test
		manager.getURLForFileHandle(proxyHandle);
	}
		
	@Test
	public void testCreateExternalFileHappyCase() throws Exception{
		ExternalFileHandle efh = createFileHandle();
		when(mockFileHandleDao.createFile(efh)).thenReturn(efh);
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
		when(mockFileHandleDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
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
		when(mockFileHandleDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
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
		when(mockFileHandleDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
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
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalS3FileHandleNullMD5(){
		externals3FileHandle.setContentMd5(null);
		// call under test
		S3FileHandle result = manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
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
	public void testCreateExternalProxyFileHandleHappy() {
		// call under test
		ProxyFileHandle pfh = manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
		assertNotNull(pfh);
		assertEquals(""+mockUser.getId(), pfh.getCreatedBy());
		assertNotNull(pfh.getCreatedOn());
		assertNotNull(pfh.getEtag());
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateExternalProxyFileHandleNotCreatorBenefactorNull() {
		// The user did not create the proxyStorageLocationSettings and no benefactor is set.
		proxyStorageLocationSettings.setCreatedBy(mockUser.getId()+1);
		proxyStorageLocationSettings.setBenefactorId(null);
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testCreateExternalProxyFileHandleNotCreatorBenefactorNotAuthroized() {
		// The user did not create the proxyStorageLocationSettings and no benefactor is set.
		proxyStorageLocationSettings.setCreatedBy(mockUser.getId()+1);
		String benefactorId = "syn99999";
		proxyStorageLocationSettings.setBenefactorId(benefactorId);
		// user lacks create on the benefactor
		when(mockAuthorizationManager.canAccess(mockUser, benefactorId, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(new AuthorizationStatus(false, "No"));
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	}
	
	@Test
	public void testCreateExternalProxyFileHandleNotCreatorBenefactorAuthroized() {
		// The user did not create the proxyStorageLocationSettings and no benefactor is set.
		proxyStorageLocationSettings.setCreatedBy(mockUser.getId()+1);
		String benefactorId = "syn99999";
		proxyStorageLocationSettings.setBenefactorId(benefactorId);
		// user has create on benefactor.
		when(mockAuthorizationManager.canAccess(mockUser, benefactorId, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(new AuthorizationStatus(true, null));
		// call under test
		ProxyFileHandle pfh = manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
		assertNotNull(pfh);
		assertEquals(""+mockUser.getId(), pfh.getCreatedBy());
		assertNotNull(pfh.getCreatedOn());
		assertNotNull(pfh.getEtag());
	}
	
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalProxyFileHandleWrongStorageLocation() {
		// setup wrong settings type.
		when(mockStorageLocationDao.get(proxyStorageLocationSettings.getStorageLocationId())).thenReturn(externalS3StorageLocationSetting);
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	} 
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalProxyFileHandleNullUserInfo() {
		mockUser = null;
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	} 
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalProxyFileHandleNullUserProxyHandle() {
		externalProxyFileHandle = null;
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalProxyFileHandleNullFileName() {
		externalProxyFileHandle.setFileName(null);
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalProxyFileHandleNullMD5() {
		externalProxyFileHandle.setContentMd5(null);
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalProxyFileHandleNullContentType() {
		externalProxyFileHandle.setContentType(null);
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalProxyFileHandleNullContentSize() {
		externalProxyFileHandle.setContentSize(null);
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalProxyFileHandleNullFilePath() {
		externalProxyFileHandle.setFilePath(null);
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCreateExternalProxyFileHandleNullStorageLocationId() {
		externalProxyFileHandle.setStorageLocationId(null);
		// call under test
		manager.createExternalProxyFileHandle(mockUser, externalProxyFileHandle);
	}
	
	@Test
	public void testCreateS3FileHandleCopy() {
		when(mockFileHandleDao.get("123")).thenReturn(createS3FileHandle());
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "987"))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		manager.createS3FileHandleCopy(mockUser, "123", "newname.png", "image");

		ArgumentCaptor<S3FileHandle> copy = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockFileHandleDao).createFile(copy.capture());
		assertEquals("bucket", copy.getValue().getBucketName());
		assertEquals("key", copy.getValue().getKey());
		assertEquals("newname.png", copy.getValue().getFileName());
		assertEquals("image", copy.getValue().getContentType());
		assertNotNull(copy.getValue().getId());
		assertEquals(mockUser.getId().toString(), copy.getValue().getCreatedBy());
	}

	@Test
	public void testCreateS3FileHandleCopyOnlyName() {
		when(mockFileHandleDao.get("123")).thenReturn(createS3FileHandle());
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "987"))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		manager.createS3FileHandleCopy(mockUser, "123", "newname.png", null);

		ArgumentCaptor<S3FileHandle> copy = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockFileHandleDao).createFile(copy.capture());
		assertEquals("bucket", copy.getValue().getBucketName());
		assertEquals("key", copy.getValue().getKey());
		assertEquals("newname.png", copy.getValue().getFileName());
		assertEquals("text", copy.getValue().getContentType());
		assertNotNull(copy.getValue().getId());
		assertEquals(mockUser.getId().toString(), copy.getValue().getCreatedBy());
	}

	@Test
	public void testCreateS3FileHandleCopyOnlyContentType() {
		when(mockFileHandleDao.get("123")).thenReturn(createS3FileHandle());
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "987"))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		manager.createS3FileHandleCopy(mockUser, "123", null, "image");

		ArgumentCaptor<S3FileHandle> copy = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockFileHandleDao).createFile(copy.capture());
		assertEquals("bucket", copy.getValue().getBucketName());
		assertEquals("key", copy.getValue().getKey());
		assertEquals("original.txt", copy.getValue().getFileName());
		assertEquals("image", copy.getValue().getContentType());
		assertNotNull(copy.getValue().getId());
		assertEquals(mockUser.getId().toString(), copy.getValue().getCreatedBy());
	}

	@Test
	public void testCreateS3FileHandleNewPreview() {
		when(mockFileHandleDao.get("123")).thenReturn(createS3FileHandle(), createS3FileHandle(), createS3FileHandle(),
				createS3FileHandle(), createS3FileHandle());
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "987"))
				.thenReturn(AuthorizationManagerUtil.AUTHORIZED);

		ArgumentCaptor<S3FileHandle> copy = ArgumentCaptor.forClass(S3FileHandle.class);

		manager.createS3FileHandleCopy(mockUser, "123", null, "image");
		manager.createS3FileHandleCopy(mockUser, "123", "text.png", null);
		manager.createS3FileHandleCopy(mockUser, "123", "original.txt", "text");
		manager.createS3FileHandleCopy(mockUser, "123", "different.txt", null);
		manager.createS3FileHandleCopy(mockUser, "123", "different.txt", "text");

		verify(mockFileHandleDao, times(5)).createFile(copy.capture());
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
		when(mockFileHandleDao.get("123")).thenThrow(NotFoundException.class);
		manager.createS3FileHandleCopy(mockUser, "123", "new", null);
	}

	@Test(expected = UnauthorizedException.class)
	public void testCreateS3FileHandleCopyFailOnNotOwner() {
		S3FileHandle originalFileHandle = createS3FileHandle();
		originalFileHandle.setCreatedBy("000");
		when(mockFileHandleDao.get("123")).thenReturn(originalFileHandle);
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "000")).thenReturn(
				AuthorizationManagerUtil.ACCESS_DENIED);

		manager.createS3FileHandleCopy(mockUser, "123", null, "image");
	}
	
	@Test
	public void testGetFileHandleAndUrlBatch() throws Exception {
		// call under test
		BatchFileResult results = manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(3, results.getRequestedFiles().size());
		
		// first one is unauthorized
		FileResult result = results.getRequestedFiles().get(0);
		assertNotNull(result);
		assertEquals(fha1.getFileHandleId(), result.getFileHandleId());
		assertEquals(FileResultFailureCode.UNAUTHORIZED, result.getFailureCode());
		assertNull(result.getFileHandle());
		assertNull(result.getPreSignedURL());
		// second one is okay
		result = results.getRequestedFiles().get(1);
		assertNotNull(result);
		assertEquals(fha2.getFileHandleId(), result.getFileHandleId());
		assertNull(result.getFailureCode());
		assertNotNull(result.getFileHandle());
		assertNotNull(result.getPreSignedURL());
		// last one is missing
		result = results.getRequestedFiles().get(2);
		assertNotNull(result);
		assertEquals(fhaMissing.getFileHandleId(), result.getFileHandleId());
		assertEquals(FileResultFailureCode.NOT_FOUND, result.getFailureCode());
		assertNull(result.getFileHandle());
		assertNull(result.getPreSignedURL());
		
		// only authorized files handles should be fetched
		HashSet<String> expectedFetch = new HashSet<>();
		expectedFetch.add(fha2.getFileHandleId());
		expectedFetch.add(fhaMissing.getFileHandleId());
		verify(mockFileHandleDao).getAllFileHandlesBatch(expectedFetch);
		
		// only one pre-signed url should be generated
		verify(mockS3Client).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
		
		// Verify a download record is created for the success case.
		ArgumentCaptor<ObjectRecordBatch> batchCapture = ArgumentCaptor.forClass(ObjectRecordBatch.class);
		verify(mockObjectRecordQueue).pushObjectRecordBatch(batchCapture.capture());
		ObjectRecordBatch batch = batchCapture.getValue();
		assertNotNull(batch.getRecords());
		assertEquals(1, batch.getRecords().size());
		ObjectRecord record = batch.getRecords().get(0);
		assertEquals(successRecord.getJsonClassName(), record.getJsonClassName());
		assertEquals(successRecord.getJsonString(), record.getJsonString());
		assertNotNull(successRecord.getTimestamp());
	}
	
	@Test
	public void testGetFileHandleAndUrlBatchUrlsOnlyWithNullValue() throws Exception {
		batchRequest.setIncludeFileHandles(null);
		batchRequest.setIncludePreSignedURLs(true);
		batchRequest.setIncludePreviewPreSignedURLs(null);
		// call under test
		BatchFileResult results = manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(3, results.getRequestedFiles().size());
		
		// second one is okay
		FileResult result = results.getRequestedFiles().get(1);
		assertNotNull(result);
		assertEquals(fha2.getFileHandleId(), result.getFileHandleId());
		assertNull(result.getFailureCode());
		assertNull(result.getFileHandle());
		assertNotNull(result.getPreSignedURL());
		assertNull(result.getPreviewPreSignedURL());
		// a batch of records should be pushed.
		verify(mockObjectRecordQueue).pushObjectRecordBatch(any(ObjectRecordBatch.class));
	}

	@Test
	public void testGetFileHandleAndUrlBatchUrlsOnly() throws Exception {
		batchRequest.setIncludeFileHandles(false);
		batchRequest.setIncludePreSignedURLs(true);
		batchRequest.setIncludePreviewPreSignedURLs(false);
		// call under test
		BatchFileResult results = manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(3, results.getRequestedFiles().size());
		
		// second one is okay
		FileResult result = results.getRequestedFiles().get(1);
		assertNotNull(result);
		assertEquals(fha2.getFileHandleId(), result.getFileHandleId());
		assertNull(result.getFailureCode());
		assertNull(result.getFileHandle());
		assertNotNull(result.getPreSignedURL());
		assertNull(result.getPreviewPreSignedURL());
		// a batch of records should be pushed.
		verify(mockObjectRecordQueue).pushObjectRecordBatch(any(ObjectRecordBatch.class));
	}

	@Test
	public void testGetFileHandleAndUrlBatchPreviewPreSignedURLOnly() throws Exception {
		batchRequest.setIncludeFileHandles(false);
		batchRequest.setIncludePreSignedURLs(false);
		batchRequest.setIncludePreviewPreSignedURLs(true);
		S3FileHandle fh = new S3FileHandle();
		fh.setId(fha2.getFileHandleId());
		fh.setPreviewId(fha2.getFileHandleId());
		Map<String, FileHandle> handleMap = new HashMap<String, FileHandle>();
		handleMap.put(fh.getId(), fh);
		when(mockFileHandleDao.getAllFileHandlesBatch(any(Iterable.class))).thenReturn(handleMap);
		// call under test
		BatchFileResult results = manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(3, results.getRequestedFiles().size());
		
		// second one is okay
		FileResult result = results.getRequestedFiles().get(1);
		assertNotNull(result);
		assertEquals(fha2.getFileHandleId(), result.getFileHandleId());
		assertNull(result.getFailureCode());
		assertNull(result.getFileHandle());
		assertNull(result.getPreSignedURL());
		assertNotNull(result.getPreviewPreSignedURL());
		// no downloads should be pushed since no urls were returned.
		verify(mockObjectRecordQueue, never()).pushObjectRecordBatch(any(ObjectRecordBatch.class));
		verify(mockFileHandleDao, times(2)).getAllFileHandlesBatch(any(Iterable.class));
	}

	@Test
	public void testGetFileHandleAndUrlBatchPreviewPreSignedURLOnlyPreviewDoesNotExist() throws Exception {
		batchRequest.setIncludeFileHandles(false);
		batchRequest.setIncludePreSignedURLs(false);
		batchRequest.setIncludePreviewPreSignedURLs(true);
		// call under test
		BatchFileResult results = manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(3, results.getRequestedFiles().size());
		
		// second one is okay
		FileResult result = results.getRequestedFiles().get(1);
		assertNotNull(result);
		assertEquals(fha2.getFileHandleId(), result.getFileHandleId());
		assertNull(result.getFailureCode());
		assertNull(result.getFileHandle());
		assertNull(result.getPreSignedURL());
		assertNull(result.getPreviewPreSignedURL());
		// no downloads should be pushed since no urls were returned.
		verify(mockObjectRecordQueue, never()).pushObjectRecordBatch(any(ObjectRecordBatch.class));
		verify(mockFileHandleDao).getAllFileHandlesBatch(any(Iterable.class));
	}

	@Test
	public void testGetFileHandleAndUrlBatchHandlesOnlyWithNullValue() throws Exception {
		batchRequest.setIncludeFileHandles(true);
		batchRequest.setIncludePreSignedURLs(null);
		// call under test
		BatchFileResult results = manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(3, results.getRequestedFiles().size());
		
		// second one is okay
		FileResult result = results.getRequestedFiles().get(1);
		assertNotNull(result);
		assertEquals(fha2.getFileHandleId(), result.getFileHandleId());
		assertNull(result.getFailureCode());
		assertNotNull(result.getFileHandle());
		assertNull(result.getPreSignedURL());
		// no downloads should be pushed since no urls were returned.
		verify(mockObjectRecordQueue, never()).pushObjectRecordBatch(any(ObjectRecordBatch.class));
	}

	@Test
	public void testGetFileHandleAndUrlBatchHandlesOnly() throws Exception {
		batchRequest.setIncludeFileHandles(true);
		batchRequest.setIncludePreSignedURLs(false);
		// call under test
		BatchFileResult results = manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(3, results.getRequestedFiles().size());
		
		// second one is okay
		FileResult result = results.getRequestedFiles().get(1);
		assertNotNull(result);
		assertEquals(fha2.getFileHandleId(), result.getFileHandleId());
		assertNull(result.getFailureCode());
		assertNotNull(result.getFileHandle());
		assertNull(result.getPreSignedURL());
		// no downloads should be pushed since no urls were returned.
		verify(mockObjectRecordQueue, never()).pushObjectRecordBatch(any(ObjectRecordBatch.class));
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleAndUrlBatchNullUser() throws Exception {
		mockUser = null;
		// call under test
		manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleAndUrlBatchNullRequest() throws Exception {
		batchRequest = null;
		// call under test
		manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetFileHandleAndUrlBatchNullFiles() throws Exception {
		batchRequest.setRequestedFiles(null);
		// call under test
		manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
	}

	@Test
	public void testGetFileHandleAndUrlBatchOverLimit() throws Exception {
		List<FileHandleAssociation> overLimit = new LinkedList<>();
		for(int i=0; i<FileHandleManagerImpl.MAX_REQUESTS_PER_CALL+1; i++){
			FileHandleAssociation fas = new FileHandleAssociation();
			fas.setAssociateObjectId(""+i);
			fas.setAssociateObjectType(FileHandleAssociateType.FileEntity);
			fas.setFileHandleId(""+(i*1000));
			overLimit.add(fas);
		}
		// call under test
		batchRequest.setRequestedFiles(overLimit);
		try {
			// call under test
			manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
			fail("should have thrown an exception");
		} catch (IllegalArgumentException e) {
			assertEquals(FileHandleManagerImpl.MAX_REQUESTS_PER_CALL_MESSAGE, e.getMessage());
		}
	}
	
	@Test
	public void testGetFileHandleAndUrlBatchEitherHandleOrUrl() throws Exception {
		batchRequest.setIncludeFileHandles(false);
		batchRequest.setIncludePreSignedURLs(false);
		try {
			// call under test
			manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
			fail("should have thrown an exception");
		} catch (IllegalArgumentException e) {
			assertEquals(FileHandleManagerImpl.MUST_INCLUDE_EITHER, e.getMessage());
		}
	}
	
	
	@Test
	public void testGetFileHandleAndUrlBatchAllUnauthorized() throws Exception {
		// setup all failures.
		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationManagerUtil.ACCESS_DENIED);
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationManagerUtil.ACCESS_DENIED);
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationManagerUtil.ACCESS_DENIED);
		List<FileHandleAssociationAuthorizationStatus> authResults = Lists.newArrayList(status1, status2, missingStatus);
		reset(mockFileHandleAuthorizationManager);
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).thenReturn(authResults);
		// call under test
		BatchFileResult results = manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(3, results.getRequestedFiles().size());
		// no file handles should be fetched.
		verify(mockFileHandleDao, never()).getAllFileHandlesBatch(anyCollection());
		// no urls should be generated.
		verify(mockS3Client, never()).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
		// no records pushed
		verify(mockObjectRecordQueue, never()).pushObjectRecordBatch(any(ObjectRecordBatch.class));
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

	@Test (expected=IllegalArgumentException.class)
	public void testCopyFileHandlesWithNullUserInfo() {
		manager.copyFileHandles(null, new BatchFileHandleCopyRequest());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCopyFileHandlesWithNullBatch() {
		manager.copyFileHandles(mockUser, null);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCopyFileHandlesWithNullCopyRequest() {
		manager.copyFileHandles(mockUser, new BatchFileHandleCopyRequest());
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCopyFileHandlesWithCopyRequestOverMaxLimit() {
		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		List<FileHandleCopyRequest> copyRequests = new LinkedList<FileHandleCopyRequest>();
		batch.setCopyRequests(copyRequests);
		for (int i = 0; i <= MAX_REQUESTS_PER_CALL; i++) {
			copyRequests.add(new FileHandleCopyRequest());
		}
		manager.copyFileHandles(mockUser, batch);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testCopyFileHandlesWithDuplicateRequests() {
		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		List<FileHandleCopyRequest> copyRequests = new LinkedList<FileHandleCopyRequest>();
		batch.setCopyRequests(copyRequests);
		FileHandleCopyRequest request = new FileHandleCopyRequest();
		FileHandleAssociation originalFile = new FileHandleAssociation();
		originalFile.setFileHandleId("1");
		request.setOriginalFile(originalFile);
		copyRequests.add(request);
		copyRequests.add(request);
		manager.copyFileHandles(mockUser, batch);
	}

	@Test
	public void testCopyFileHandles() throws Exception {
		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		List<FileHandleCopyRequest> requests = new ArrayList<FileHandleCopyRequest>(2);
		batch.setCopyRequests(requests);

		FileHandleAssociation fha1 = new FileHandleAssociation();
		fha1.setAssociateObjectId("1");
		fha1.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fha1.setFileHandleId("1");
		FileHandleAssociation fha2 = new FileHandleAssociation();
		fha2.setAssociateObjectId("2");
		fha2.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		fha2.setFileHandleId("2");
		FileHandleAssociation fha3 = new FileHandleAssociation();
		fha3.setAssociateObjectId("3");
		fha3.setAssociateObjectType(FileHandleAssociateType.TableEntity);
		fha3.setFileHandleId("3");

		FileHandleCopyRequest request1 = new FileHandleCopyRequest();
		request1.setOriginalFile(fha1);
		FileHandleCopyRequest request2 = new FileHandleCopyRequest();
		request2.setOriginalFile(fha2);
		String newFileName = "newFileName";
		request2.setNewFileName(newFileName);
		FileHandleCopyRequest request3 = new FileHandleCopyRequest();
		request3.setOriginalFile(fha3);
		String newContentType = "newContentType";
		request3.setNewContentType(newContentType);

		requests.add(request1);
		requests.add(request2);
		requests.add(request3);

		List<FileHandleAssociationAuthorizationStatus> authResults = new LinkedList<FileHandleAssociationAuthorizationStatus>();
		authResults.add(new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationManagerUtil.ACCESS_DENIED));
		authResults.add(new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationManagerUtil.AUTHORIZED));
		authResults.add(new FileHandleAssociationAuthorizationStatus(fha3, AuthorizationManagerUtil.AUTHORIZED));
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, FileHandleCopyUtils.getOriginalFiles(batch))).thenReturn(authResults);
		Map<String, FileHandle> fileHandles = new HashMap<String, FileHandle>();

		S3FileHandle fileHandle = new S3FileHandle();
		String oldId = "1";
		fileHandle.setId(oldId);
		String originalOwner = "999";
		fileHandle.setCreatedBy(originalOwner);
		Date oldCreationDate = new Date();
		fileHandle.setCreatedOn(oldCreationDate);
		String oldEtag = UUID.randomUUID().toString();
		fileHandle.setEtag(oldEtag);
		String oldFileName = "oldFileName";
		fileHandle.setFileName(oldFileName);
		String oldContentType = "oldContentType";
		fileHandle.setContentType(oldContentType);
		fileHandles.put("2", fileHandle);
		when(mockFileHandleDao.getAllFileHandlesBatch(any(List.class))).thenReturn(fileHandles);
		Long newId = 789L;
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(newId );

		Thread.sleep(1000);

		BatchFileHandleCopyResult result = manager.copyFileHandles(mockUser, batch);
		assertNotNull(result);
		List<FileHandleCopyResult> copyResults = result.getCopyResults();
		assertNotNull(copyResults);
		assertEquals(3, copyResults.size());
		FileHandleCopyResult first = copyResults.get(0);
		FileHandleCopyResult second = copyResults.get(1);
		FileHandleCopyResult third = copyResults.get(2);
		assertEquals(fha1.getFileHandleId(), first.getOriginalFileHandleId());
		assertEquals(FileResultFailureCode.UNAUTHORIZED, first.getFailureCode());
		assertNull(first.getNewFileHandle());
		assertEquals(fha2.getFileHandleId(), second.getOriginalFileHandleId());
		assertNull(second.getFailureCode());
		assertEquals(fha3.getFileHandleId(), third.getOriginalFileHandleId());
		assertEquals(FileResultFailureCode.NOT_FOUND, third.getFailureCode());
		assertNull(third.getNewFileHandle());

		ArgumentCaptor<Set> fileHandleListCaptor = ArgumentCaptor.forClass(Set.class);
		verify(mockFileHandleDao).getAllFileHandlesBatch(fileHandleListCaptor.capture());
		Set<String> fileHandleList = fileHandleListCaptor.getValue();
		assertNotNull(fileHandleList);
		assertTrue(fileHandleList.contains("2"));
		assertTrue(fileHandleList.contains("3"));
		assertEquals(2, fileHandleList.size());

		ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
		verify(mockFileHandleDao).createBatch(captor.capture());
		List<FileHandle> toCreate = captor.getValue();
		assertNotNull(toCreate);
		assertEquals(1, toCreate.size());
		FileHandle newFileHandle = toCreate.get(0);
		assertEquals(newId.toString(), newFileHandle.getId());
		assertNotNull(newFileHandle.getEtag());
		assertFalse(newFileHandle.getEtag().equals(oldEtag));
		assertNotNull(newFileHandle.getCreatedOn());
		assertFalse(newFileHandle.getCreatedOn().equals(oldCreationDate));
		assertEquals(mockUser.getId().toString(), newFileHandle.getCreatedBy());
		assertEquals(newFileName, newFileHandle.getFileName());
		assertEquals(oldContentType, newFileHandle.getContentType());

		assertEquals(newFileHandle, second.getNewFileHandle());

		ArgumentCaptor<ObjectRecordBatch> recordCaptor = ArgumentCaptor.forClass(ObjectRecordBatch.class);
		verify(mockObjectRecordQueue).pushObjectRecordBatch(recordCaptor.capture());
		ObjectRecordBatch recordBatch = recordCaptor.getValue();
		assertNotNull(recordBatch);
		assertEquals(FILE_HANDLE_COPY_RECORD_TYPE, recordBatch.getType());
		List<ObjectRecord> records = recordBatch.getRecords();
		assertNotNull(records);
		assertEquals(1, records.size());
		ObjectRecord record = records.get(0);
		assertEquals(EntityFactory.createJSONStringForEntity(FileHandleCopyUtils.createCopyRecord(mockUser.getId().toString(), newId.toString(), fha2)),
				record.getJsonString());
	}

	@Test
	public void testGetDefaultUploadDestinationWithNullUploadDestinationListSetting(){
		assertEquals(DBOStorageLocationDAOImpl.getDefaultUploadDestination(),
				manager.getDefaultUploadDestination(mockUser, "syn1"));
	}

	@Test
	public void testGetDefaultUploadDestinationWithNullLocations(){
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, "syn1",
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(new UploadDestinationListSetting());
		assertEquals(DBOStorageLocationDAOImpl.getDefaultUploadDestination(),
				manager.getDefaultUploadDestination(mockUser, "syn1"));
	}

	@Test
	public void testGetDefaultUploadDestinationWithEmptyLocations(){
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setLocations(new LinkedList<Long>());
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, "syn1",
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(setting );
		assertEquals(DBOStorageLocationDAOImpl.getDefaultUploadDestination(),
				manager.getDefaultUploadDestination(mockUser, "syn1"));
	}

	@Test
	public void testGetDefaultUploadDestinationWithANullLocation(){
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setLocations(Arrays.asList((Long) null));
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, "syn1",
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(setting );
		assertEquals(DBOStorageLocationDAOImpl.getDefaultUploadDestination(),
				manager.getDefaultUploadDestination(mockUser, "syn1"));
	}

	@Test (expected=IllegalArgumentException.class)
	public void testGetUploadDestinationWithNullStorageLocationId() {
		manager.getUploadDestination(mockUser, "syn1", null);
	}
}
