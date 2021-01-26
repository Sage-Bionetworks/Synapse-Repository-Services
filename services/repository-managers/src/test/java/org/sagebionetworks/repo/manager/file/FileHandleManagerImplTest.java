package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.file.FileHandleManagerImpl.FILE_HANDLE_COPY_RECORD_TYPE;
import static org.sagebionetworks.repo.manager.file.FileHandleManagerImpl.MAX_REQUESTS_PER_CALL;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.audit.dao.ObjectRecordBatch;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.audit.ObjectRecordQueue;
import org.sagebionetworks.repo.manager.events.EventsCollector;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.DBOStorageLocationDAOImpl;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.BatchFileHandleCopyResult;
import org.sagebionetworks.repo.model.file.BatchFileRequest;
import org.sagebionetworks.repo.model.file.BatchFileResult;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalGoogleCloudUploadDestination;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreUploadDestination;
import org.sagebionetworks.repo.model.file.ExternalS3UploadDestination;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.FileHandleCopyRequest;
import org.sagebionetworks.repo.model.file.FileHandleCopyResult;
import org.sagebionetworks.repo.model.file.FileResult;
import org.sagebionetworks.repo.model.file.FileResultFailureCode;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.S3UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestination;
import org.sagebionetworks.repo.model.file.UploadDestinationLocation;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalObjectStorageLocationSetting;
import org.sagebionetworks.repo.model.project.ExternalS3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.ProjectSettingsType;
import org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.model.project.UploadDestinationListSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.BinaryUtils;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.StorageException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * A unit test for the FileUploadManagerImpl.
 * 
 * @author jmhill
 *
 */
@ExtendWith(MockitoExtension.class)
public class FileHandleManagerImplTest {
	private static final String BANNER = "dummy banner text";
	private static final String BASE_KEY = "some-base-key";
	private static final String PARENT_ENTITY_ID = "syn123";

	@Mock
	FileHandleDao mockFileHandleDao;
	@Mock
	SynapseS3Client mockS3Client;
	@Mock
	SynapseGoogleCloudStorageClient mockGoogleCloudStorageClient;
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
	@Mock
	EventsCollector mockStatisticsCollector;

	@InjectMocks
	@Spy
	FileHandleManagerImpl manager;

	UserInfo mockUser;
	S3FileHandle validResults;

	String bucket;
	String key;
	String md5;
	Long fileSize;
	Long externalS3StorageLocationId;
	String endpointUrl;
	// setup a storage location
	ExternalS3StorageLocationSetting externalS3StorageLocationSetting;
	S3FileHandle externals3FileHandle;

	Long proxyStorageLocationId;
	ProxyStorageLocationSettings proxyStorageLocationSettings;
	ProxyFileHandle externalProxyFileHandle;

	Long externalObjectStorageLocationId;
	ExternalObjectStorageLocationSetting externalObjectStorageLocationSetting;
	ExternalObjectStoreFileHandle externalObjectStoreFileHandle;

	Long externalGoogleCloudStorageLocationId;
	ExternalGoogleCloudStorageLocationSetting externalGoogleCloudStorageLocationSetting;
	GoogleCloudFileHandle googleCloudFileHandle;
	GoogleCloudFileHandle externalGoogleCloudFileHandle;

	Long synapseStorageLocationId;
	S3StorageLocationSetting synapseStorageLocationSetting;

	List<FileHandleAssociation> associations;
	FileHandleAssociation fha1;
	FileHandleAssociation fha2;
	FileHandleAssociation fhaMissing;
	BatchFileRequest batchRequest;
	ObjectRecord successRecord;

	@BeforeEach
	public void before() throws IOException, NoSuchAlgorithmException{
		bucket = "some-bucket";
		key = BASE_KEY + "/some-key";

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

		googleCloudFileHandle = new GoogleCloudFileHandle();
		googleCloudFileHandle.setId("124");
		googleCloudFileHandle.setCreatedBy(mockUser.getId().toString());
		googleCloudFileHandle.setCreatedOn(new Date());
		googleCloudFileHandle.setContentType(contentType);
		googleCloudFileHandle.setContentSize(new Long(contentBytes.length));
		googleCloudFileHandle.setContentMd5(contentMD5);
		googleCloudFileHandle.setFileName(fileName);
		googleCloudFileHandle.setBucketName(bucket);
		googleCloudFileHandle.setKey(key);

		md5 = "0123456789abcdef0123456789abcdef";
		fileSize = 103L;
		externalS3StorageLocationId = 987L;
		// setup a storage location
		externalS3StorageLocationSetting = new ExternalS3StorageLocationSetting();
		externalS3StorageLocationSetting.setBanner(BANNER);
		externalS3StorageLocationSetting.setBaseKey(BASE_KEY);
		externalS3StorageLocationSetting.setBucket(bucket);
		externalS3StorageLocationSetting.setCreatedBy(mockUser.getId());
		externalS3StorageLocationSetting.setStorageLocationId(externalS3StorageLocationId);
		externalS3StorageLocationSetting.setStsEnabled(true);
		externalS3StorageLocationSetting.setUploadType(UploadType.S3);

		externals3FileHandle = new S3FileHandle();
		externals3FileHandle.setBucketName(bucket);
		externals3FileHandle.setKey(key);
		externals3FileHandle.setStorageLocationId(externalS3StorageLocationId);
		externals3FileHandle.setContentMd5(md5);

		externalGoogleCloudStorageLocationId = 10241L;
		externalGoogleCloudStorageLocationSetting = new ExternalGoogleCloudStorageLocationSetting();
		externalGoogleCloudStorageLocationSetting.setBanner(BANNER);
		externalGoogleCloudStorageLocationSetting.setBaseKey(BASE_KEY);
		externalGoogleCloudStorageLocationSetting.setBucket(bucket);
		externalGoogleCloudStorageLocationSetting.setCreatedBy(mockUser.getId());
		externalGoogleCloudStorageLocationSetting.setStorageLocationId(externalGoogleCloudStorageLocationId);
		externalGoogleCloudStorageLocationSetting.setUploadType(UploadType.GOOGLECLOUDSTORAGE);

		externalGoogleCloudFileHandle = new GoogleCloudFileHandle();
		externalGoogleCloudFileHandle.setBucketName(bucket);
		externalGoogleCloudFileHandle.setKey(key);
		externalGoogleCloudFileHandle.setStorageLocationId(externalGoogleCloudStorageLocationId);
		externalGoogleCloudFileHandle.setContentMd5(md5);

		// proxy storage location setup.
		proxyStorageLocationId = 5555L;
		proxyStorageLocationSettings = new ProxyStorageLocationSettings();
		proxyStorageLocationSettings.setStorageLocationId(proxyStorageLocationId);
		proxyStorageLocationSettings.setCreatedBy(mockUser.getId());

		externalProxyFileHandle = new ProxyFileHandle();
		externalProxyFileHandle.setContentMd5("0123456789abcdef0123456789abcdef");
		externalProxyFileHandle.setContentSize(123L);
		externalProxyFileHandle.setContentType("plain/text");
		externalProxyFileHandle.setFileName("foo.bar");
		externalProxyFileHandle.setFilePath("/pathParent/pathChild");
		externalProxyFileHandle.setStorageLocationId(proxyStorageLocationSettings.getStorageLocationId());
		externalProxyFileHandle.setId("444444");

		//set up external object store
		endpointUrl = "https://www.url.com";
		externalObjectStorageLocationId = 96024L;
		externalObjectStorageLocationSetting = new ExternalObjectStorageLocationSetting();
		externalObjectStorageLocationSetting.setBanner(BANNER);
		externalObjectStorageLocationSetting.setStorageLocationId(externalObjectStorageLocationId);
		externalObjectStorageLocationSetting.setBucket(bucket);
		externalObjectStorageLocationSetting.setEndpointUrl(endpointUrl);
		externalObjectStorageLocationSetting.setUploadType(UploadType.HTTPS);

		externalObjectStoreFileHandle = new ExternalObjectStoreFileHandle();
		externalObjectStoreFileHandle.setStorageLocationId(externalObjectStorageLocationId);
		externalObjectStoreFileHandle.setContentMd5(md5);
		externalObjectStoreFileHandle.setContentSize(fileSize);
		externalObjectStoreFileHandle.setFileKey(key);

		// Set up Synapse Storage Location.
		synapseStorageLocationId = 97981L;
		synapseStorageLocationSetting = new S3StorageLocationSetting();
		synapseStorageLocationSetting.setBanner(BANNER);
		synapseStorageLocationSetting.setBaseKey(BASE_KEY);
		synapseStorageLocationSetting.setStorageLocationId(synapseStorageLocationId);
		synapseStorageLocationSetting.setStsEnabled(true);
		synapseStorageLocationSetting.setUploadType(UploadType.S3);

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
		
		successRecord = FileHandleManagerImpl.createObjectRecord(mockUser.getId().toString(), fha2, 123L);

		batchRequest = new BatchFileRequest();
		batchRequest.setRequestedFiles(associations);
		batchRequest.setIncludeFileHandles(true);
		batchRequest.setIncludePreSignedURLs(true);
	}
		
	@Test
	public void testGetFileHandleUnAuthorized() throws DatastoreException, NotFoundException{
		// You must be authorized to see a file handle
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// denied!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> manager.getRawFileHandle(mockUser, handleId));
	}
	
	@Test
	public void testGetFileHandleAuthorized() throws DatastoreException, NotFoundException{
		// You must be authorized to see a file handle
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// allow
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationStatus.authorized());
		FileHandle handle = manager.getRawFileHandle(mockUser, handleId);
		assertEquals(validResults, handle, "failed to get the handle");
	}

	@Test
	public void testGetRawFileHandleUnchecked() {
		// Mock dependencies.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);

		// Method under test.
		FileHandle handle = manager.getRawFileHandleUnchecked(handleId);
		assertSame(validResults, handle);

		// This method does not call auth manager.
		verifyZeroInteractions(mockAuthorizationManager);
	}

	@Test
	public void testDeleteNotFound() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenThrow(new NotFoundException());
		manager.deleteFileHandle(mockUser, handleId);
	}
	
	@Test
	public void testDeleteUnAuthorzied() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// denied!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> manager.deleteFileHandle(mockUser, handleId));
	}
	
	@Test
	public void testDeleteAuthorized() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// allow!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationStatus.authorized());
		manager.deleteFileHandle(mockUser, handleId);
		// The S3 file should get deleted.
		verify(mockS3Client, times(1)).deleteObject(validResults.getBucketName(), validResults.getKey());
		// The database handle should be deleted.
		verify(mockFileHandleDao, times(1)).delete(handleId);
	}

	@Test
	public void testDeleteGoogleCloud() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(googleCloudFileHandle);
		// allow!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, googleCloudFileHandle.getCreatedBy())).thenReturn(AuthorizationStatus.authorized());
		manager.deleteFileHandle(mockUser, handleId);
		// The S3 file should get deleted.
		verify(mockGoogleCloudStorageClient, times(1)).deleteObject(googleCloudFileHandle.getBucketName(), googleCloudFileHandle.getKey());
		// The database handle should be deleted.
		verify(mockFileHandleDao, times(1)).delete(handleId);
	}


	@Test
	public void testDeleteFileHandleDisablePreview() throws Exception {
		// Deleting a file handle that has previews disabled should not StackOverflow :)
		validResults.setPreviewId(validResults.getId());
		when(mockFileHandleDao.get(validResults.getId())).thenReturn(validResults);
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, validResults.getId(), validResults.getCreatedBy())).thenReturn(AuthorizationStatus.authorized());
		manager.deleteFileHandle(mockUser, validResults.getId());
	}
	
	@Test
	public void testDeleteFileHandleWithFailedDeleteOnS3() throws Exception {
		String fileHandleId = "123";
		
		when(mockFileHandleDao.get(fileHandleId)).thenReturn(validResults);
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, validResults.getId(), validResults.getCreatedBy())).thenReturn(AuthorizationStatus.authorized());
		
		RuntimeException deleteException = new RuntimeException("Something went wrong");
		
		doThrow(deleteException).when(mockFileHandleDao).delete(any());
		
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			// Call under test
			manager.deleteFileHandle(mockUser, fileHandleId);			
		});
		
		assertEquals(deleteException, ex);
		
		verify(mockFileHandleDao).get(fileHandleId);
		verify(mockAuthorizationManager).canAccessRawFileHandleByCreator(mockUser, fileHandleId, validResults.getCreatedBy());
		verify(mockFileHandleDao).delete(fileHandleId);
		
		verifyZeroInteractions(mockS3Client);
		
	}
	
	@Test
	public void testDeleteFileHandleWithFailedDeleteOnGC() throws Exception {
		String fileHandleId = "123";
		
		when(mockFileHandleDao.get(fileHandleId)).thenReturn(googleCloudFileHandle);
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, fileHandleId, googleCloudFileHandle.getCreatedBy())).thenReturn(AuthorizationStatus.authorized());
		
		RuntimeException deleteException = new RuntimeException("Something went wrong");
		
		doThrow(deleteException).when(mockFileHandleDao).delete(any());
		
		RuntimeException ex = assertThrows(RuntimeException.class, () -> {
			// Call under test
			manager.deleteFileHandle(mockUser, fileHandleId);			
		});
		
		assertEquals(deleteException, ex);
		
		verify(mockFileHandleDao).get(fileHandleId);
		verify(mockAuthorizationManager).canAccessRawFileHandleByCreator(mockUser, fileHandleId, googleCloudFileHandle.getCreatedBy());
		verify(mockFileHandleDao).delete(fileHandleId);
		
		verifyZeroInteractions(mockGoogleCloudStorageClient);
		
	}
	
	@Test
	public void testClearPreviewUnauthroized() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// denied!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationStatus.accessDenied(""));
		assertThrows(UnauthorizedException.class, () -> manager.clearPreview(mockUser, handleId));
	}
	
	@Test
	public void testClearPreviewAuthorized() throws DatastoreException, NotFoundException{
		// Deleting a handle that no longer exists should not throw an exception.
		String handleId = "123";
		when(mockFileHandleDao.get(handleId)).thenReturn(validResults);
		// allow!
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, handleId, validResults.getCreatedBy())).thenReturn(AuthorizationStatus.authorized());
		manager.clearPreview(mockUser, handleId);
		// The database reference to the preview handle should be cleared
		verify(mockFileHandleDao, times(1)).setPreviewId(eq(handleId), eq((String)null));
	}
	
	
	@Test
	public void testDeleteWithPreview() throws DatastoreException, NotFoundException{
		// Test deleting a file with a preview
		S3FileHandle preview = new S3FileHandle();
		preview.setId("456");
		preview.setCreatedBy(validResults.getCreatedBy());
		preview.setBucketName("previewBucket");
		preview.setKey("previewKey");
		// Assign the preview to the file
		validResults.setPreviewId(preview.getId());
		when(mockFileHandleDao.get(validResults.getId())).thenReturn(validResults);
		when(mockFileHandleDao.get(preview.getId())).thenReturn(preview);
		// Allow all calls
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(any(UserInfo.class), anyString(), any(String.class))).thenReturn(AuthorizationStatus.authorized());
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
		external.setCreatedBy(mockUser.getId().toString());
		when(mockFileHandleDao.get(external.getId())).thenReturn(external);
		when(mockAuthorizationManager.isUserCreatorOrAdmin(mockUser, external.getCreatedBy())).thenReturn(true);
		// fire!
		String redirect = manager.getRedirectURLForFileHandle(mockUser, external.getId());
		assertNotNull(redirect);
		assertEquals(external.getExternalURL(), redirect.toString());
	}
	
	@Test
	public void testGetRedirectURLForFileHandleS3() throws DatastoreException, NotFoundException, MalformedURLException{
		S3FileHandle s3FileHandle = new S3FileHandle();
		s3FileHandle.setId("123");
		s3FileHandle.setBucketName("bucket");
		s3FileHandle.setKey("key");
		s3FileHandle.setCreatedBy(mockUser.getId().toString());
		when(mockFileHandleDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		when(mockAuthorizationManager.isUserCreatorOrAdmin(mockUser, s3FileHandle.getCreatedBy())).thenReturn(true);
		String expectedURL = "https://amamzon.com";
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).
			thenReturn(new URL(expectedURL));
		// fire!
		String redirect = manager.getRedirectURLForFileHandle(mockUser, s3FileHandle.getId());
		assertNotNull(redirect);
		assertEquals(expectedURL, redirect.toString());
	}

	@Test
	public void testGetRedirectURLForFileHandleGoogleCloud() throws DatastoreException, NotFoundException, MalformedURLException{
		GoogleCloudFileHandle googleCloudFileHandle = new GoogleCloudFileHandle();
		googleCloudFileHandle.setId("123");
		googleCloudFileHandle.setBucketName("bucket");
		googleCloudFileHandle.setKey("key");
		googleCloudFileHandle.setCreatedBy(mockUser.getId().toString());
		when(mockFileHandleDao.get(googleCloudFileHandle.getId())).thenReturn(googleCloudFileHandle);
		when(mockAuthorizationManager.isUserCreatorOrAdmin(mockUser, googleCloudFileHandle.getCreatedBy())).thenReturn(true);
		String expectedURL = "https://google.com";
		when(mockGoogleCloudStorageClient.createSignedUrl(anyString(), anyString(), anyLong(), any(HttpMethod.class))).
				thenReturn(new URL(expectedURL));
		// fire!
		String redirect = manager.getRedirectURLForFileHandle(mockUser, googleCloudFileHandle.getId());
		assertNotNull(redirect);
		assertEquals(expectedURL, redirect);
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
	
	@Test
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
		assertThrows(IllegalArgumentException.class, () -> manager.getURLForFileHandle(proxyHandle));
	}

	//////////////////////////////////////////////////////
	// createExternalFileHandle(ExternalFileHandle) tests
	//////////////////////////////////////////////////////
		
	@Test
	public void testCreateExternalFileHappyCase() throws Exception{
		ExternalFileHandle efh = createFileHandle();
		when(mockFileHandleDao.createFile(efh)).thenReturn(efh);
		// This should work
		ExternalFileHandle result = manager.createExternalFileHandle(mockUser, efh);
		assertNotNull(result);
		assertEquals(mockUser.getId().toString(), result.getCreatedBy());

		verifyZeroInteractions(mockProjectSettingsManager, mockStorageLocationDao);
	}
	
	@Test
	public void testCreateExternalFileHandleNullUser(){
		ExternalFileHandle efh = createFileHandle();
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(null, efh));
	}
	
	@Test
	public void testCreateExternalFileHandleNullHandle(){
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, (ExternalFileHandle) null));
	}

	@Test
	public void testCreateExternalFileHandleNullFileName(){
		ExternalFileHandle efh = createFileHandle();
		efh.setFileName(null);
		// This should not fail.
		manager.createExternalFileHandle(mockUser, efh);
	}

	@Test
	public void testCreateExternalFileHandleNullContentType(){
		ExternalFileHandle efh = createFileHandle();
		efh.setContentType(null);
		// This should not fail.
		manager.createExternalFileHandle(mockUser, efh);
	}
	
	@Test
	public void testCreateExternalFileHandleNullURL(){
		ExternalFileHandle efh = createFileHandle();
		efh.setExternalURL(null);
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, efh));
	}
	
	@Test
	public void testCreateExternalFileHandleMalformedURL(){
		ExternalFileHandle efh = createFileHandle();
		efh.setExternalURL("local");
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, efh));
	}

	@Test
	public void testCreateExternalFileHandle_IsStsStorageLocationTrue(){
		// Mock dependencies.
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);
		externalS3StorageLocationSetting.setStsEnabled(true);

		when(mockProjectSettingsManager.isStsStorageLocationSetting(externalS3StorageLocationSetting))
				.thenReturn(true);

		// Set up file handle.
		ExternalFileHandle efh = createFileHandle();
		efh.setStorageLocationId(externalS3StorageLocationId);

		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser,
				efh));
		assertEquals("Cannot create ExternalFileHandle in an STS-enabled storage location", ex.getMessage());
	}

	@Test
	public void testCreateExternalFileHandle_IsStsStorageLocationFalse(){
		// Mock dependencies.
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);
		externalS3StorageLocationSetting.setStsEnabled(false);

		when(mockProjectSettingsManager.isStsStorageLocationSetting(externalS3StorageLocationSetting))
				.thenReturn(false);

		// Set up file handle.
		ExternalFileHandle efh = createFileHandle();
		efh.setStorageLocationId(externalS3StorageLocationId);

		// Method under test - Does not throw.
		manager.createExternalFileHandle(mockUser, efh);
	}

	@Test
	public void testGetURLRequestWithOwnerAuthCheck() throws Exception {
		S3FileHandle s3FileHandle = new S3FileHandle();
		s3FileHandle.setId("123");
		s3FileHandle.setCreatedBy(mockUser.getId().toString());
		s3FileHandle.setBucketName("bucket");
		s3FileHandle.setKey("key");
		when(mockFileHandleDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		
		String expectedURL = "https://amamzon.com";
		
		when(mockFileHandleDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL(expectedURL));
		when(mockAuthorizationManager.isUserCreatorOrAdmin(mockUser, s3FileHandle.getCreatedBy())).thenReturn(true);
		
		FileHandleUrlRequest request = new FileHandleUrlRequest(mockUser, s3FileHandle.getId());
		
		String redirectURL = manager.getRedirectURLForFileHandle(request);
		
		verify(mockAuthorizationManager, times(1)).isUserCreatorOrAdmin(mockUser, s3FileHandle.getCreatedBy());
		verify(mockFileHandleAuthorizationManager, never()).canDownLoadFile(any(UserInfo.class), any(List.class));
		// Verifies that download stats are not sent
		verify(mockStatisticsCollector, never()).collectEvent(any());
		
		assertEquals(expectedURL, redirectURL);
	}
	
	@Test
	public void testGetURLRequestWithAssociateAuthCheck() throws Exception {
		S3FileHandle s3FileHandle = new S3FileHandle();
		s3FileHandle.setId("123");
		s3FileHandle.setCreatedBy(mockUser.getId().toString());
		s3FileHandle.setBucketName("bucket");
		s3FileHandle.setKey("key");
		when(mockFileHandleDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		
		String expectedURL = "https://amamzon.com";
		
		when(mockFileHandleDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL(expectedURL));
		
		FileHandleAssociation association = new FileHandleAssociation();
		
		association.setAssociateObjectId("999");
		association.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		association.setFileHandleId(s3FileHandle.getId());
		
		List<FileHandleAssociation> associations = Collections.singletonList(association);
		
		FileHandleAssociationAuthorizationStatus authorizationResult = 
				new FileHandleAssociationAuthorizationStatus(association, AuthorizationStatus.authorized());

		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).
		thenReturn(Collections.singletonList(authorizationResult));
		
		FileHandleUrlRequest request = new FileHandleUrlRequest(mockUser, s3FileHandle.getId())
				.withAssociation(association.getAssociateObjectType(), association.getAssociateObjectId());
		
		String redirectURL = manager.getRedirectURLForFileHandle(request);
		
		verify(mockAuthorizationManager, never()).isUserCreatorOrAdmin(any(UserInfo.class), any(String.class));
		verify(mockFileHandleAuthorizationManager, times(1)).canDownLoadFile(mockUser, associations);
		// Verifies that download stats are sent
		verify(mockStatisticsCollector, times(1)).collectEvent(any());
		
		assertEquals(expectedURL, redirectURL);
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
	
	@Test
	public void testGetURLUnauthorized() throws Exception{
		S3FileHandle s3FileHandle = new S3FileHandle();
		s3FileHandle.setId("123");
		s3FileHandle.setCreatedBy("456");
		s3FileHandle.setBucketName("bucket");
		s3FileHandle.setKey("key");
		when(mockFileHandleDao.get(s3FileHandle.getId())).thenReturn(s3FileHandle);
		when(mockAuthorizationManager.isUserCreatorOrAdmin(mockUser, s3FileHandle.getCreatedBy())).thenReturn(false);
		assertThrows(UnauthorizedException.class, () -> manager.getRedirectURLForFileHandle(mockUser, s3FileHandle.getId()));
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
				association, AuthorizationStatus.authorized());

		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).
		thenReturn(Collections.singletonList(authorizationResult));
		
		// method under test
		String redirect = manager.getRedirectURLForFileHandle(mockUser,
				s3FileHandle.getId(), FileHandleAssociateType.VerificationSubmission, associateObjectId);
		
		// the manager returns the redirect URL, no exception thrown
		assertEquals(expectedURL, redirect);
		
		// now make it unauthorized
		authorizationResult.setStatus(AuthorizationStatus.accessDenied(""));

		assertThrows(UnauthorizedException.class, () ->
			 manager.getRedirectURLForFileHandle(mockUser,
					 s3FileHandle.getId(),
					 FileHandleAssociateType.VerificationSubmission,
					 associateObjectId)
		);
	}

	///////////////////////////////////////////////////
	// createExternalS3FileHandle tests
	///////////////////////////////////////////////////


	@Test
	public void testCreateExternalS3FileHandleHappy(){
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);
		when(mockFileHandleDao.createFile(externals3FileHandle)).thenReturn(externals3FileHandle);

		ObjectMetadata mockMeta = mock(ObjectMetadata.class);
		when(mockS3Client.getObjectMetadata(bucket, key)).thenReturn(mockMeta);
		when(mockMeta.getContentLength()).thenReturn(fileSize);

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
		assertEquals(externalS3StorageLocationId, result.getStorageLocationId());
	}
	
	@Test
	public void testCreateExternalS3FileHandleNullMD5(){
		externals3FileHandle.setContentMd5(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalS3FileHandle(mockUser, externals3FileHandle));
	}

	@Test
	public void testCreateExternalS3FileHandleInvalidMD5(){
		externals3FileHandle.setContentMd5("not hex string");
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.createExternalS3FileHandle(mockUser, externals3FileHandle),
				"FileHandle.contentMd5 is required and must not be the empty string.");
	}

	@Test
	public void testCreateExternalS3FileHandleEmptyMD5(){
		externals3FileHandle.setContentMd5("");
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.createExternalS3FileHandle(mockUser, externals3FileHandle),
				"FileHandle.contentMd5 is required and must not be the empty string.");
	}
	
	@Test
	public void testCreateExternalS3FileHandleUnauthorized(){
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);

		// In this case the esl created by does not match the caller.
		externalS3StorageLocationSetting.setCreatedBy(mockUser.getId()+1);
		// should fails since the user is not the creator of the storage location.
		assertThrows(UnauthorizedException.class, () -> manager.createExternalS3FileHandle(mockUser, externals3FileHandle));
	}
	
	@Test
	public void testCreateExternalS3FileHandleWrongStorageType(){
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(new S3StorageLocationSetting());
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalS3FileHandle(mockUser, externals3FileHandle));
	}

	@Test
	public void testCreateExternalS3FileHandleEmptyBucket(){
		externals3FileHandle.setBucketName("");
		assertThrows(IllegalArgumentException.class, () ->
				manager.createExternalS3FileHandle(mockUser, externals3FileHandle),
				"FileHandle.bucket is required and must not be the empty string.");
	}

	@Test
	public void testCreateExternalS3FileHandleNullBucket(){
		externals3FileHandle.setBucketName(null);
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalS3FileHandle(mockUser, externals3FileHandle));
	}
	
	@Test
	public void testCreateExternalS3FileHandleNullKey(){
		externals3FileHandle.setKey(null);
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalS3FileHandle(mockUser, externals3FileHandle));
	}

	@Test
	public void testCreateExternalS3FileHandleEmptyKey(){
		externals3FileHandle.setKey("");
		// should fail
		assertThrows(IllegalArgumentException.class,
				() -> manager.createExternalS3FileHandle(mockUser, externals3FileHandle),
		"FileHandle.key is required and must not be the empty string.");
	}
	
	@Test
	public void testCreateExternalS3FileHandleNullStorageId(){
		externals3FileHandle.setStorageLocationId(null);
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalS3FileHandle(mockUser, externals3FileHandle));
	}
	
	@Test
	public void testCreateExternalS3FileHandleBucketDoesNotMatchLocation(){
		// must match the storage location bucket.
		externalS3StorageLocationSetting.setBucket(bucket);
		externals3FileHandle.setBucketName(bucket+"no-match");
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalS3FileHandle(mockUser, externals3FileHandle));
	}
	
	@Test
	public void testCreateExternalS3FileHandleS3Error(){
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);
		when(mockS3Client.getObjectMetadata(bucket, key)).thenThrow(new AmazonClientException("Something is wrong"));

		// should fail
		Exception ex = assertThrows(IllegalArgumentException.class, () -> manager.createExternalS3FileHandle(mockUser,
				externals3FileHandle));
		assertEquals("Unable to access the file at bucket: " + bucket + " key: " + key + ".", ex.getMessage());
	}

	@Test
	public void testCreateExternalS3FileHandle_StsEnabledNullAllowsAnyBaseKey(){
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(null);
		externalS3StorageLocationSetting.setBaseKey(BASE_KEY);
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);

		externals3FileHandle.setKey("mismatched-key");
		when(mockFileHandleDao.createFile(externals3FileHandle)).thenReturn(externals3FileHandle);

		ObjectMetadata mockMeta = mock(ObjectMetadata.class);
		when(mockS3Client.getObjectMetadata(bucket, externals3FileHandle.getKey())).thenReturn(mockMeta);
		when(mockMeta.getContentLength()).thenReturn(fileSize);

		// Method under test - Most of this is tested above. Just test that FileHandleDao.createFile() is called.
		manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
		verify(mockFileHandleDao).createFile(externals3FileHandle);
	}

	@Test
	public void testCreateExternalS3FileHandle_StsEnabledFalseAllowsAnyBaseKey(){
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(false);
		externalS3StorageLocationSetting.setBaseKey(BASE_KEY);
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);

		externals3FileHandle.setKey("mismatched-key");
		when(mockFileHandleDao.createFile(externals3FileHandle)).thenReturn(externals3FileHandle);

		ObjectMetadata mockMeta = mock(ObjectMetadata.class);
		when(mockS3Client.getObjectMetadata(bucket, externals3FileHandle.getKey())).thenReturn(mockMeta);
		when(mockMeta.getContentLength()).thenReturn(fileSize);

		// Method under test - Most of this is tested above. Just test that FileHandleDao.createFile() is called.
		manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
		verify(mockFileHandleDao).createFile(externals3FileHandle);
	}

	@Test
	public void testCreateExternalS3FileHandle_StsEnabledTrueWithoutBaseKey(){
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(true);
		externalS3StorageLocationSetting.setBaseKey(null);
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);

		externals3FileHandle.setKey("mismatched-key");
		when(mockFileHandleDao.createFile(externals3FileHandle)).thenReturn(externals3FileHandle);

		ObjectMetadata mockMeta = mock(ObjectMetadata.class);
		when(mockS3Client.getObjectMetadata(bucket, externals3FileHandle.getKey())).thenReturn(mockMeta);
		when(mockMeta.getContentLength()).thenReturn(fileSize);

		// Method under test - Most of this is tested above. Just test that FileHandleDao.createFile() is called.
		manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
		verify(mockFileHandleDao).createFile(externals3FileHandle);
	}

	@Test
	public void testCreateExternalS3FileHandle_StsEnabledTrueBaseKeyMatches(){
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(true);
		externalS3StorageLocationSetting.setBaseKey(BASE_KEY);
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);

		externals3FileHandle.setKey(BASE_KEY + "/subfolder/some-file");
		when(mockFileHandleDao.createFile(externals3FileHandle)).thenReturn(externals3FileHandle);

		ObjectMetadata mockMeta = mock(ObjectMetadata.class);
		when(mockS3Client.getObjectMetadata(bucket, externals3FileHandle.getKey())).thenReturn(mockMeta);
		when(mockMeta.getContentLength()).thenReturn(fileSize);

		// Method under test - Most of this is tested above. Just test that FileHandleDao.createFile() is called.
		manager.createExternalS3FileHandle(mockUser, externals3FileHandle);
		verify(mockFileHandleDao).createFile(externals3FileHandle);
	}

	@Test
	public void testCreateExternalS3FileHandle_StsEnabledTrueBaseKeyDoesNotMatch(){
		// Mock dependencies.
		externalS3StorageLocationSetting.setStsEnabled(true);
		externalS3StorageLocationSetting.setBaseKey(BASE_KEY);
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);

		String mismatchedKey = "mismatched-key";
		externals3FileHandle.setKey(mismatchedKey);

		// Method under test - Throws.
		Exception ex = assertThrows(IllegalArgumentException.class, () -> manager.createExternalS3FileHandle(mockUser,
				externals3FileHandle));
		assertEquals("The baseKey for ExternalS3StorageLocationSetting.id=" + externalS3StorageLocationId +
						" does not match the provided key: " + mismatchedKey, ex.getMessage());
		verify(mockFileHandleDao, never()).createFile(any());
	}

	///////////////////////////////////////////////////
	// createExternalGoogleCloudFileHandle tests
	///////////////////////////////////////////////////


	@Test
	public void testCreateExternalGoogleCloudFileHandleGetSizeFromGC(){
		when(mockStorageLocationDao.get(externalGoogleCloudStorageLocationId)).thenReturn(
				externalGoogleCloudStorageLocationSetting);
		when(mockFileHandleDao.createFile(externalGoogleCloudFileHandle)).thenReturn(externalGoogleCloudFileHandle);
		Blob mockGCBlob = mock(Blob.class);
		when(mockGoogleCloudStorageClient.getObject(bucket, key)).thenReturn(mockGCBlob);
		when(mockGCBlob.getSize()).thenReturn(fileSize);

		// call under test
		GoogleCloudFileHandle result = manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle);
		assertNotNull(result);
		assertEquals(mockUser.getId().toString(), result.getCreatedBy());
		assertNotNull(result.getCreatedOn());
		assertEquals(md5, result.getContentMd5());
		assertEquals(fileSize, result.getContentSize());
		assertNotNull(result.getEtag());
		assertEquals(bucket, result.getBucketName());
		assertEquals(key, result.getKey());
		assertEquals(externalGoogleCloudStorageLocationId, result.getStorageLocationId());
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleSpecifiedSize(){
		long specifiedContentSize = 999999L;
		externalGoogleCloudFileHandle.setContentSize(specifiedContentSize);

		when(mockStorageLocationDao.get(externalGoogleCloudStorageLocationId)).thenReturn(
				externalGoogleCloudStorageLocationSetting);
		when(mockFileHandleDao.createFile(externalGoogleCloudFileHandle)).thenReturn(externalGoogleCloudFileHandle);
		Blob mockGCBlob = mock(Blob.class);
		when(mockGoogleCloudStorageClient.getObject(bucket, key)).thenReturn(mockGCBlob);

		// call under test
		GoogleCloudFileHandle result = manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle);
		assertNotNull(result);
		assertEquals(mockUser.getId().toString(), result.getCreatedBy());
		assertNotNull(result.getCreatedOn());
		assertEquals(md5, result.getContentMd5());

		// The content size should be set to the specified value
		assertEquals(specifiedContentSize, result.getContentSize());

		assertNotNull(result.getEtag());
		assertEquals(bucket, result.getBucketName());
		assertEquals(key, result.getKey());
		assertEquals(externalGoogleCloudStorageLocationId, result.getStorageLocationId());

	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleNoSizeAndGCPSizeIsNull(){
		externalGoogleCloudFileHandle.setContentSize(null);

		when(mockStorageLocationDao.get(externalGoogleCloudStorageLocationId)).thenReturn(
				externalGoogleCloudStorageLocationSetting);
		Blob mockGCBlob = mock(Blob.class);
		when(mockGoogleCloudStorageClient.getObject(bucket, key)).thenReturn(mockGCBlob);
		when(mockGCBlob.getSize()).thenReturn(null);

		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle));

		verify(mockGCBlob, times(1)).getSize();
	}


	@Test
	public void testCreateExternalGoogleCloudFileHandleNullMD5(){
		externalGoogleCloudFileHandle.setContentMd5(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle));
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleInvalidMD5(){
		externalGoogleCloudFileHandle.setContentMd5("not hex string");
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle),
				"FileHandle.contentMd5 is required and must not be the empty string.");
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleEmptyMD5(){
		externalGoogleCloudFileHandle.setContentMd5("");
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle),
				"FileHandle.contentMd5 is required and must not be the empty string.");
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleUnauthorized(){
		when(mockStorageLocationDao.get(externalGoogleCloudStorageLocationId)).thenReturn(
				externalGoogleCloudStorageLocationSetting);
		// In this case the esl created by does not match the caller.
		externalGoogleCloudStorageLocationSetting.setCreatedBy(mockUser.getId()+1);
		// should fails since the user is not the creator of the storage location.
		assertThrows(UnauthorizedException.class, () -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle));
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleWrongStorageType(){
		when(mockStorageLocationDao.get(externalGoogleCloudStorageLocationId)).thenReturn(new ExternalS3StorageLocationSetting());
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle));
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleEmptyBucket(){
		externalGoogleCloudFileHandle.setBucketName("");
		assertThrows(IllegalArgumentException.class, () ->
						manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle),
				"FileHandle.bucket is required and must not be the empty string.");
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleNullBucket(){
		externalGoogleCloudFileHandle.setBucketName(null);
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle));
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleNullKey(){
		externalGoogleCloudFileHandle.setKey(null);
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle));
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleEmptyKey(){
		externalGoogleCloudFileHandle.setKey("");
		// should fail
		assertThrows(IllegalArgumentException.class,
				() -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle),
				"FileHandle.key is required and must not be the empty string.");
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleNullStorageId(){
		externalGoogleCloudFileHandle.setStorageLocationId(null);
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle));
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleBucketDoesNotMatchLocation(){
		// must match the storage location bucket.
		externalS3StorageLocationSetting.setBucket(bucket);
		externalGoogleCloudFileHandle.setBucketName(bucket+"no-match");
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle));
	}

	@Test
	public void testCreateExternalGoogleCloudFileHandleGoogleCloudError(){
		when(mockStorageLocationDao.get(externalGoogleCloudStorageLocationId)).thenReturn(
				externalGoogleCloudStorageLocationSetting);
		when(mockGoogleCloudStorageClient.getObject(bucket, key)).thenThrow(new StorageException(403, "Something is wrong"));
		// should fail
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalGoogleCloudFileHandle(mockUser, externalGoogleCloudFileHandle));
	}

	///////////////////////////////////////////////////
	// createExternalFileHandle(ProxyFileHandle) tests
	///////////////////////////////////////////////////

	@Test
	public void testCreateExternalProxyFileHandleHappy() {
		when(mockStorageLocationDao.get(proxyStorageLocationId)).thenReturn(proxyStorageLocationSettings);
		when(mockFileHandleDao.createFile(externalProxyFileHandle)).thenReturn(externalProxyFileHandle);

		// call under test
		ProxyFileHandle pfh = manager.createExternalFileHandle(mockUser, externalProxyFileHandle);
		assertNotNull(pfh);
		assertEquals(""+mockUser.getId(), pfh.getCreatedBy());
		assertNotNull(pfh.getCreatedOn());
		assertNotNull(pfh.getEtag());
	}

	@Test
	public void testCreateExternalProxyFileHandleNotCreatorBenefactorNull() {
		when(mockStorageLocationDao.get(proxyStorageLocationId)).thenReturn(proxyStorageLocationSettings);

		// The user did not create the proxyStorageLocationSettings and no benefactor is set.
		proxyStorageLocationSettings.setCreatedBy(mockUser.getId()+1);
		proxyStorageLocationSettings.setBenefactorId(null);
		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	}
	
	@Test
	public void testCreateExternalProxyFileHandleNotCreatorBenefactorNotAuthroized() {
		when(mockStorageLocationDao.get(proxyStorageLocationId)).thenReturn(proxyStorageLocationSettings);

		// The user did not create the proxyStorageLocationSettings and no benefactor is set.
		proxyStorageLocationSettings.setCreatedBy(mockUser.getId()+1);
		String benefactorId = "syn99999";
		proxyStorageLocationSettings.setBenefactorId(benefactorId);
		// user lacks create on the benefactor
		when(mockAuthorizationManager.canAccess(mockUser, benefactorId, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.accessDenied("No"));
		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	}
	
	@Test
	public void testCreateExternalProxyFileHandleNotCreatorBenefactorAuthroized() {
		when(mockStorageLocationDao.get(proxyStorageLocationId)).thenReturn(proxyStorageLocationSettings);
		when(mockFileHandleDao.createFile(externalProxyFileHandle)).thenReturn(externalProxyFileHandle);

		// The user did not create the proxyStorageLocationSettings and no benefactor is set.
		proxyStorageLocationSettings.setCreatedBy(mockUser.getId()+1);
		String benefactorId = "syn99999";
		proxyStorageLocationSettings.setBenefactorId(benefactorId);
		// user has create on benefactor.
		when(mockAuthorizationManager.canAccess(mockUser, benefactorId, ObjectType.ENTITY, ACCESS_TYPE.CREATE)).thenReturn(AuthorizationStatus.authorized());
		// call under test
		ProxyFileHandle pfh = manager.createExternalFileHandle(mockUser, externalProxyFileHandle);
		assertNotNull(pfh);
		assertEquals(""+mockUser.getId(), pfh.getCreatedBy());
		assertNotNull(pfh.getCreatedOn());
		assertNotNull(pfh.getEtag());
	}
	
	
	@Test
	public void testCreateExternalProxyFileHandleWrongStorageLocation() {
		// setup wrong settings type.
		when(mockStorageLocationDao.get(proxyStorageLocationSettings.getStorageLocationId())).thenReturn(externalS3StorageLocationSetting);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	} 
	
	@Test
	public void testCreateExternalProxyFileHandleNullUserInfo() {
		mockUser = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	} 
	
	@Test
	public void testCreateExternalProxyFileHandleNullUserProxyHandle() {
		externalProxyFileHandle = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	}
	
	@Test
	public void testCreateExternalProxyFileHandleNullFileName() {
		externalProxyFileHandle.setFileName(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	}
	
	@Test
	public void testCreateExternalProxyFileHandleNullMD5() {
		externalProxyFileHandle.setContentMd5(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	}
	
	@Test
	public void testCreateExternalProxyFileHandleNullContentType() {
		externalProxyFileHandle.setContentType(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	}
	
	@Test
	public void testCreateExternalProxyFileHandleNullContentSize() {
		externalProxyFileHandle.setContentSize(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	}
	
	@Test
	public void testCreateExternalProxyFileHandleNullFilePath() {
		externalProxyFileHandle.setFilePath(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	}
	
	@Test
	public void testCreateExternalProxyFileHandleNullStorageLocationId() {
		externalProxyFileHandle.setStorageLocationId(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalProxyFileHandle));
	}

	/////////////////////////////////////////////////////////////////
	// createExternalFileHandle(ExternalObjectStoreFileHandle) tests
	/////////////////////////////////////////////////////////////////

	@Test
	public void testCreateExternalObjectStoreFileHandleNullUserId(){
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(null, externalObjectStoreFileHandle));
	}

	@Test
	public void testCreateExternalObjectStoreFileHandleNullFileHandle(){
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, (ExternalObjectStoreFileHandle) null));
	}

	@Test
	public void testCreateExternalObjectStoreFileHandleNullStorageLocationId(){
		externalObjectStoreFileHandle.setStorageLocationId(null);
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalObjectStoreFileHandle));
	}

	@Test
	public void testCreateExternalObjectStoreFileHandleNullContentSize(){
		externalObjectStoreFileHandle.setContentSize(null);
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalObjectStoreFileHandle));
	}

	@Test
	public void testCreateExternalObjectStoreFileHandleNullContentMd5(){
		externalObjectStoreFileHandle.setContentMd5(null);
		assertThrows(IllegalArgumentException.class, () -> 	manager.createExternalFileHandle(mockUser, externalObjectStoreFileHandle));
	}

	@Test
	public void testCreateExternalObjectStoreFileHandleInvalidContentMd5(){
		externalObjectStoreFileHandle.setContentMd5("not hexadecimal");
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalObjectStoreFileHandle));
	}

	@Test
	public void testCreateExternalObjectStoreFileHandleNullFileKey(){
		assertThrows(IllegalArgumentException.class, () -> externalObjectStoreFileHandle.setFileKey(null));
	}

	@Test
	public void testCreateExternalObjectStoreFileHandleWrongStorageLocationType(){
		when(mockStorageLocationDao.get(externalObjectStorageLocationId)).thenReturn(externalS3StorageLocationSetting);
		assertThrows(IllegalArgumentException.class, () -> manager.createExternalFileHandle(mockUser, externalObjectStoreFileHandle));
	}

	@Test
	public void testCreateExternalObjectStoreFileHandle(){
		when(mockFileHandleDao.createFile(externalObjectStoreFileHandle)).thenReturn(externalObjectStoreFileHandle);
		when(mockStorageLocationDao.get(externalObjectStorageLocationId)).thenReturn(
				externalObjectStorageLocationSetting);

		//method under test
		ExternalObjectStoreFileHandle result = manager.createExternalFileHandle(mockUser, externalObjectStoreFileHandle);

		verify(mockStorageLocationDao, times(1)).get(externalObjectStorageLocationId);
		verify(mockFileHandleDao, times(1)).createFile(externalObjectStoreFileHandle);

		//check the metadata is set correctly
		assertNotNull(result);

		//since the externalObjectStoreFileHandle did not have fileName and contentType set, check that they have value of NOT_SET
		assertEquals(FileHandleManagerImpl.NOT_SET, result.getFileName());
		assertEquals(FileHandleManagerImpl.NOT_SET, result.getContentType());

		//check that new metadata was added
		assertEquals(mockUser.getId().toString(), result.getCreatedBy());
		assertNotNull(result.getCreatedOn());
		assertNotNull(result.getEtag());
		assertNotNull(result.getId());

		//check that the provided metadata (in setup()) was not modified
		assertEquals(md5, result.getContentMd5());
		assertEquals(fileSize, result.getContentSize());
		assertEquals(key, result.getFileKey());
		assertEquals(externalObjectStorageLocationId, result.getStorageLocationId());
		assertEquals(bucket, result.getBucket());
		assertEquals(endpointUrl, result.getEndpointUrl());

	}


	@Test
	public void testCreateS3FileHandleCopy() {
		when(mockFileHandleDao.get("123")).thenReturn(createS3FileHandle());
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "987"))
				.thenReturn(AuthorizationStatus.authorized());

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
				.thenReturn(AuthorizationStatus.authorized());

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
				.thenReturn(AuthorizationStatus.authorized());

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
				.thenReturn(AuthorizationStatus.authorized());

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

	@Test
	public void testCreateS3FileHandleCopyFailOnNeither() {
		assertThrows(IllegalArgumentException.class, () -> manager.createS3FileHandleCopy(mockUser, "123", null, null));
	}
	@Test
	public void testCreateS3FileHandleCopyFileNameHasNonAsciiCharacters() {
		assertThrows(IllegalArgumentException.class, () -> manager.createS3FileHandleCopy(mockUser, "123", "föö bär.txt", null));
	}

	@Test
	public void testCreateS3FileHandleCopyFailOnNotExist() {
		when(mockFileHandleDao.get("123")).thenThrow(NotFoundException.class);
		assertThrows(NotFoundException.class, () -> manager.createS3FileHandleCopy(mockUser, "123", "new", null));
	}

	@Test
	public void testCreateS3FileHandleCopyFailOnNotOwner() {
		S3FileHandle originalFileHandle = createS3FileHandle();
		originalFileHandle.setCreatedBy("000");
		when(mockFileHandleDao.get("123")).thenReturn(originalFileHandle);
		when(mockAuthorizationManager.canAccessRawFileHandleByCreator(mockUser, "123", "000")).thenReturn(
				AuthorizationStatus.accessDenied(""));

		assertThrows(UnauthorizedException.class, () -> manager.createS3FileHandleCopy(mockUser, "123", null, "image"));
	}
	
	@Test
	public void testGetFileHandleAndUrlBatch() throws Exception {
		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationStatus.accessDenied(""));
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationStatus.authorized());
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationStatus.authorized());
		List<FileHandleAssociationAuthorizationStatus> authResults = Lists.newArrayList(status1, status2, missingStatus);
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).thenReturn(authResults);

		FileHandle fh2 = new S3FileHandle();
		fh2.setId(fha2.getFileHandleId());
		Map<String, FileHandle> handleMap = new HashMap<>();
		handleMap.put(fh2.getId(), fh2);
		when(mockFileHandleDao.getAllFileHandlesBatch(any())).thenReturn(handleMap);

		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL("https", "host","/a-url"));

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
		
		// Verifies that download stats are sent
		verify(mockStatisticsCollector, times(1)).collectEvents(any());
		
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

		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationStatus.accessDenied(""));
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationStatus.authorized());
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationStatus.authorized());
		List<FileHandleAssociationAuthorizationStatus> authResults = Lists.newArrayList(status1, status2, missingStatus);
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).thenReturn(authResults);

		FileHandle fh2 = new S3FileHandle();
		fh2.setId(fha2.getFileHandleId());
		Map<String, FileHandle> handleMap = new HashMap<>();
		handleMap.put(fh2.getId(), fh2);
		when(mockFileHandleDao.getAllFileHandlesBatch(any())).thenReturn(handleMap);

		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL("https", "host","/a-url"));

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
		// Verifies that download stats are sent
		verify(mockStatisticsCollector, times(1)).collectEvents(any());
	}

	@Test
	public void testGetFileHandleAndUrlBatchUrlsOnly() throws Exception {
		batchRequest.setIncludeFileHandles(false);
		batchRequest.setIncludePreSignedURLs(true);
		batchRequest.setIncludePreviewPreSignedURLs(false);

		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationStatus.accessDenied(""));
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationStatus.authorized());
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationStatus.authorized());
		List<FileHandleAssociationAuthorizationStatus> authResults = Lists.newArrayList(status1, status2, missingStatus);
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).thenReturn(authResults);

		FileHandle fh2 = new S3FileHandle();
		fh2.setId(fha2.getFileHandleId());
		Map<String, FileHandle> handleMap = new HashMap<>();
		handleMap.put(fh2.getId(), fh2);
		when(mockFileHandleDao.getAllFileHandlesBatch(any())).thenReturn(handleMap);

		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL("https", "host","/a-url"));

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
		// Verifies that download stats are sent
		verify(mockStatisticsCollector, times(1)).collectEvents(any());
	}

	@Test
	public void testGetFileHandleAndUrlBatchPreviewPreSignedURLOnly() throws Exception {
		batchRequest.setIncludeFileHandles(false);
		batchRequest.setIncludePreSignedURLs(false);
		batchRequest.setIncludePreviewPreSignedURLs(true);

		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationStatus.accessDenied(""));
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationStatus.authorized());
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationStatus.authorized());
		List<FileHandleAssociationAuthorizationStatus> authResults = Lists.newArrayList(status1, status2, missingStatus);
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).thenReturn(authResults);

		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL("https", "host","/a-url"));

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
		// Verifies that download stats are never sent
		verify(mockStatisticsCollector, never()).collectEvents(any());
	}

	@Test
	public void testGetFileHandleAndUrlBatchPreviewPreSignedURLOnlyPreviewDoesNotExist() throws Exception {
		batchRequest.setIncludeFileHandles(false);
		batchRequest.setIncludePreSignedURLs(false);
		batchRequest.setIncludePreviewPreSignedURLs(true);

		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationStatus.accessDenied(""));
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationStatus.authorized());
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationStatus.authorized());
		List<FileHandleAssociationAuthorizationStatus> authResults = Lists.newArrayList(status1, status2, missingStatus);
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).thenReturn(authResults);

		FileHandle fh2 = new S3FileHandle();
		fh2.setId(fha2.getFileHandleId());
		Map<String, FileHandle> handleMap = new HashMap<>();
		handleMap.put(fh2.getId(), fh2);
		when(mockFileHandleDao.getAllFileHandlesBatch(any())).thenReturn(handleMap);

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
		// Verifies that download stats are never sent
		verify(mockStatisticsCollector, never()).collectEvents(any());
	}

	@Test
	public void testGetFileHandleAndUrlBatchHandlesOnlyWithNullValue() throws Exception {
		batchRequest.setIncludeFileHandles(true);
		batchRequest.setIncludePreSignedURLs(null);

		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationStatus.accessDenied(""));
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationStatus.authorized());
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationStatus.authorized());
		List<FileHandleAssociationAuthorizationStatus> authResults = Lists.newArrayList(status1, status2, missingStatus);
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).thenReturn(authResults);

		FileHandle fh2 = new S3FileHandle();
		fh2.setId(fha2.getFileHandleId());
		Map<String, FileHandle> handleMap = new HashMap<>();
		handleMap.put(fh2.getId(), fh2);
		when(mockFileHandleDao.getAllFileHandlesBatch(any())).thenReturn(handleMap);

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
		// Verifies that download stats are never sent
		verify(mockStatisticsCollector, never()).collectEvents(any());
	}

	@Test
	public void testGetFileHandleAndUrlBatchHandlesOnly() throws Exception {
		batchRequest.setIncludeFileHandles(true);
		batchRequest.setIncludePreSignedURLs(false);

		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationStatus.accessDenied(""));
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationStatus.authorized());
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationStatus.authorized());
		List<FileHandleAssociationAuthorizationStatus> authResults = Lists.newArrayList(status1, status2, missingStatus);
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).thenReturn(authResults);

		FileHandle fh2 = new S3FileHandle();
		fh2.setId(fha2.getFileHandleId());
		Map<String, FileHandle> handleMap = new HashMap<>();
		handleMap.put(fh2.getId(), fh2);
		when(mockFileHandleDao.getAllFileHandlesBatch(any())).thenReturn(handleMap);

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
		// Verifies that download stats are never sent
		verify(mockStatisticsCollector, never()).collectEvents(any());
	}
	
	@Test
	public void testGetFileHandleAndUrlBatchNullUser() throws Exception {
		mockUser = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getFileHandleAndUrlBatch(mockUser, batchRequest));
	}
	
	@Test
	public void testGetFileHandleAndUrlBatchNullRequest() throws Exception {
		batchRequest = null;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getFileHandleAndUrlBatch(mockUser, batchRequest));
	}
	
	@Test
	public void testGetFileHandleAndUrlBatchNullFiles() throws Exception {
		batchRequest.setRequestedFiles(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getFileHandleAndUrlBatch(mockUser, batchRequest));
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

		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.getFileHandleAndUrlBatch(mockUser, batchRequest),
				FileHandleManagerImpl.MAX_REQUESTS_PER_CALL_MESSAGE);
	}
	
	@Test
	public void testGetFileHandleAndUrlBatchEitherHandleOrUrl() throws Exception {
		batchRequest.setIncludeFileHandles(false);
		batchRequest.setIncludePreSignedURLs(false);
		// call under test
		assertThrows(IllegalArgumentException.class,
				() -> manager.getFileHandleAndUrlBatch(mockUser, batchRequest),
				FileHandleManagerImpl.MUST_INCLUDE_EITHER);
	}
	
	
	@Test
	public void testGetFileHandleAndUrlBatchAllUnauthorized() throws Exception {
		// setup all failures.
		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationStatus.accessDenied(""));
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationStatus.accessDenied(""));
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationStatus.accessDenied(""));
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
		// Verifies that download stats are never sent
		verify(mockStatisticsCollector, never()).collectEvents(any());
	}



	@Test
	public void testGetFileHandleAndUrlBatchPreSignedURLForExternalObjectStore() throws Exception {
		batchRequest.setIncludeFileHandles(false);
		batchRequest.setIncludePreSignedURLs(true);
		batchRequest.setIncludePreviewPreSignedURLs(false);
		ExternalObjectStoreFileHandle fh = new ExternalObjectStoreFileHandle();
		fh.setEndpointUrl("https://s3.amazonaws.com");
		fh.setBucket("some.bucket.name");
		fh.setFileKey("somepath/file.txt");

		fh.setId(fha2.getFileHandleId());
		Map<String, FileHandle> handleMap = new HashMap<String, FileHandle>();
		handleMap.put(fh.getId(), fh);
		when(mockFileHandleDao.getAllFileHandlesBatch(any(Iterable.class))).thenReturn(handleMap);

		FileHandleAssociationAuthorizationStatus status1 = new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationStatus.accessDenied(""));
		FileHandleAssociationAuthorizationStatus status2 = new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationStatus.authorized());
		FileHandleAssociationAuthorizationStatus missingStatus = new FileHandleAssociationAuthorizationStatus(fhaMissing, AuthorizationStatus.authorized());
		List<FileHandleAssociationAuthorizationStatus> authResults = Lists.newArrayList(status1, status2, missingStatus);
		when(mockFileHandleAuthorizationManager.canDownLoadFile(mockUser, associations)).thenReturn(authResults);

		// call under test
		BatchFileResult results = manager.getFileHandleAndUrlBatch(mockUser, batchRequest);
		assertNotNull(results);
		assertNotNull(results.getRequestedFiles());
		assertEquals(3, results.getRequestedFiles().size());

		//check the results
		FileResult result = results.getRequestedFiles().get(1);
		assertNotNull(result);
		assertEquals(fha2.getFileHandleId(), result.getFileHandleId());
		assertNull(result.getFailureCode());
		assertNull(result.getFileHandle());
		assertEquals("https://s3.amazonaws.com/some.bucket.name/somepath/file.txt", result.getPreSignedURL());
		assertNull(result.getPreviewPreSignedURL());

		verify(mockObjectRecordQueue, times(1)).pushObjectRecordBatch(any(ObjectRecordBatch.class));
		verify(mockFileHandleDao, times(1)).getAllFileHandlesBatch(any(Iterable.class));
		verify(mockStatisticsCollector, times(1)).collectEvents(any());
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

	@Test
	public void testCopyFileHandlesWithNullUserInfo() {
		assertThrows(IllegalArgumentException.class, () -> manager.copyFileHandles(null, new BatchFileHandleCopyRequest()));
	}

	@Test
	public void testCopyFileHandlesWithNullBatch() {
		assertThrows(IllegalArgumentException.class, () -> manager.copyFileHandles(mockUser, null));
	}

	@Test
	public void testCopyFileHandlesWithNullCopyRequest() {
		assertThrows(IllegalArgumentException.class, () -> manager.copyFileHandles(mockUser, new BatchFileHandleCopyRequest()));
	}

	@Test
	public void testCopyFileHandlesWithCopyRequestOverMaxLimit() {
		BatchFileHandleCopyRequest batch = new BatchFileHandleCopyRequest();
		List<FileHandleCopyRequest> copyRequests = new LinkedList<FileHandleCopyRequest>();
		batch.setCopyRequests(copyRequests);
		for (int i = 0; i <= MAX_REQUESTS_PER_CALL; i++) {
			copyRequests.add(new FileHandleCopyRequest());
		}
		assertThrows(IllegalArgumentException.class, () -> manager.copyFileHandles(mockUser, batch));
	}

	@Test
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
		assertThrows(IllegalArgumentException.class, () -> manager.copyFileHandles(mockUser, batch));
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
		authResults.add(new FileHandleAssociationAuthorizationStatus(fha1, AuthorizationStatus.accessDenied("")));
		authResults.add(new FileHandleAssociationAuthorizationStatus(fha2, AuthorizationStatus.authorized()));
		authResults.add(new FileHandleAssociationAuthorizationStatus(fha3, AuthorizationStatus.authorized()));
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
		when(mockFileHandleDao.getAllFileHandlesBatch(any())).thenReturn(fileHandles);
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
		assertNotEquals(newFileHandle.getEtag(), oldEtag);
		assertNotNull(newFileHandle.getCreatedOn());
		assertNotEquals(newFileHandle.getCreatedOn(), oldCreationDate);
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
	public void getUploadDestinationLocations_NoProjectSettings() {
		// Mock dependencies.
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(Optional.empty());

		// Method under test.
		List<UploadDestinationLocation> locationList = manager.getUploadDestinationLocations(mockUser,
				PARENT_ENTITY_ID);
		assertEquals(1, locationList.size());
		UploadDestinationLocation location = locationList.get(0);
		assertEquals(DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID, location.getStorageLocationId());
		assertEquals(UploadType.S3, location.getUploadType());
	}

	@Test
	public void getUploadDestinationLocations_NullStorageLocations() {
		// Mock dependencies.
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setLocations(null);
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(Optional.of(setting));

		// Method under test.
		List<UploadDestinationLocation> locationList = manager.getUploadDestinationLocations(mockUser,
				PARENT_ENTITY_ID);
		assertEquals(1, locationList.size());
		UploadDestinationLocation location = locationList.get(0);
		assertEquals(DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID, location.getStorageLocationId());
		assertEquals(UploadType.S3, location.getUploadType());
	}

	@Test
	public void getUploadDestinationLocations_EmptyStorageLocations() {
		// Mock dependencies.
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setLocations(ImmutableList.of());
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(Optional.of(setting));

		// Method under test.
		List<UploadDestinationLocation> locationList = manager.getUploadDestinationLocations(mockUser,
				PARENT_ENTITY_ID);
		assertEquals(1, locationList.size());
		UploadDestinationLocation location = locationList.get(0);
		assertEquals(DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID, location.getStorageLocationId());
		assertEquals(UploadType.S3, location.getUploadType());
	}

	@Test
	public void getUploadDestinationLocations_NormalCase() {
		// Mock dependencies.
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setLocations(ImmutableList.of(externalS3StorageLocationId));
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class)).thenReturn(Optional.of(setting));

		UploadDestinationLocation location = new UploadDestinationLocation();
		when(mockProjectSettingsManager.getUploadDestinationLocations(mockUser,
				ImmutableList.of(externalS3StorageLocationId))).thenReturn(ImmutableList.of(location));

		// Method under test.
		List<UploadDestinationLocation> result = manager.getUploadDestinationLocations(mockUser,
				PARENT_ENTITY_ID);
		assertEquals(1, result.size());
		assertSame(location, result.get(0));
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
				.thenReturn(Optional.of(new UploadDestinationListSetting()));
		assertEquals(DBOStorageLocationDAOImpl.getDefaultUploadDestination(),
				manager.getDefaultUploadDestination(mockUser, "syn1"));
	}

	@Test
	public void testGetDefaultUploadDestinationWithEmptyLocations(){
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setLocations(new LinkedList<>());
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, "syn1",
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(setting));
		assertEquals(DBOStorageLocationDAOImpl.getDefaultUploadDestination(),
				manager.getDefaultUploadDestination(mockUser, "syn1"));
	}

	@Test
	public void testGetDefaultUploadDestinationWithANullLocation(){
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setLocations(Collections.singletonList(null));
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, "syn1",
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(setting));
		assertEquals(DBOStorageLocationDAOImpl.getDefaultUploadDestination(),
				manager.getDefaultUploadDestination(mockUser, "syn1"));
	}

	@Test
	public void testGetDefaultUploadDestinationWithANormalLocation(){
		// Mock dependencies.
		UploadDestinationListSetting setting = new UploadDestinationListSetting();
		setting.setLocations(ImmutableList.of(externalS3StorageLocationId));
		when(mockProjectSettingsManager.getProjectSettingForNode(mockUser, PARENT_ENTITY_ID,
				ProjectSettingsType.upload, UploadDestinationListSetting.class))
				.thenReturn(Optional.of(setting));

		// Spy manager.getUploadDestination(). This is tested elsewhere and requires several more mocks.
		UploadDestination mockUploadDestination = mock(UploadDestination.class);
		doReturn(mockUploadDestination).when(manager).getUploadDestination(mockUser, PARENT_ENTITY_ID,
				externalS3StorageLocationId);

		UploadDestination result = manager.getDefaultUploadDestination(mockUser, PARENT_ENTITY_ID);
		assertSame(mockUploadDestination, result);
	}

	@Test
	public void testGetUploadDestinationWithNullStorageLocationId() {
		assertThrows(IllegalArgumentException.class, () -> manager.getUploadDestination(mockUser, "syn1", null));
	}

	@Test
	public void testGetUploadDestination_Default() {
		// Note that this generates a new UploadDestination instance, so we use equals, not same.
		UploadDestination result = manager.getUploadDestination(mockUser, PARENT_ENTITY_ID,
				DBOStorageLocationDAOImpl.DEFAULT_STORAGE_LOCATION_ID);
		assertEquals(DBOStorageLocationDAOImpl.getDefaultUploadDestination(), result);
	}

	@Test
	public void testGetUploadDestination_Synapse() {
		when(mockStorageLocationDao.get(synapseStorageLocationId)).thenReturn(synapseStorageLocationSetting);

		UploadDestination result = manager.getUploadDestination(mockUser, PARENT_ENTITY_ID, synapseStorageLocationId);
		assertEquals(BANNER, result.getBanner());
		assertEquals(synapseStorageLocationId, result.getStorageLocationId());
		assertEquals(UploadType.S3, result.getUploadType());

		assertTrue(result instanceof S3UploadDestination);
		S3UploadDestination synapseUploadDestination = (S3UploadDestination) result;
		assertEquals(BASE_KEY, synapseUploadDestination.getBaseKey());
		assertTrue(synapseUploadDestination.getStsEnabled());
	}

	@Test
	public void testGetUploadDestination_ExternalS3() {
		when(mockStorageLocationDao.get(externalS3StorageLocationId)).thenReturn(externalS3StorageLocationSetting);

		UploadDestination result = manager.getUploadDestination(mockUser, PARENT_ENTITY_ID,
				externalS3StorageLocationId);
		assertEquals(BANNER, result.getBanner());
		assertEquals(externalS3StorageLocationId, result.getStorageLocationId());
		assertEquals(UploadType.S3, result.getUploadType());

		assertTrue(result instanceof ExternalS3UploadDestination);
		ExternalS3UploadDestination externalS3UploadDestination = (ExternalS3UploadDestination) result;
		assertEquals(BASE_KEY, externalS3UploadDestination.getBaseKey());
		assertEquals(bucket, externalS3UploadDestination.getBucket());
		assertTrue(externalS3UploadDestination.getStsEnabled());
	}

	@Test
	public void testGetUploadDestination_ExternalGoogleCloud() {
		when(mockStorageLocationDao.get(externalGoogleCloudStorageLocationId)).thenReturn(
				externalGoogleCloudStorageLocationSetting);

		UploadDestination result = manager.getUploadDestination(mockUser, PARENT_ENTITY_ID,
				externalGoogleCloudStorageLocationId);
		assertEquals(BANNER, result.getBanner());
		assertEquals(externalGoogleCloudStorageLocationId, result.getStorageLocationId());
		assertEquals(UploadType.GOOGLECLOUDSTORAGE, result.getUploadType());

		assertTrue(result instanceof ExternalGoogleCloudUploadDestination);
		ExternalGoogleCloudUploadDestination externalGoogleCloudUploadDestination =
				(ExternalGoogleCloudUploadDestination) result;
		assertEquals(BASE_KEY, externalGoogleCloudUploadDestination.getBaseKey());
		assertEquals(bucket, externalGoogleCloudUploadDestination.getBucket());
	}

	@Test
	public void testGetUploadDestinationExternalObjectStore(){
		when(mockStorageLocationDao.get(externalObjectStorageLocationId)).thenReturn(
				externalObjectStorageLocationSetting);

		ExternalObjectStoreUploadDestination result = (ExternalObjectStoreUploadDestination) manager.getUploadDestination(mockUser, "syn123", externalObjectStorageLocationId);
		assertNotNull(result);
		verify(mockStorageLocationDao, times(1)).get(externalObjectStorageLocationId);
		assertNotNull(result.getKeyPrefixUUID());
		assertEquals(externalObjectStorageLocationId, result.getStorageLocationId());
		assertEquals(bucket, result.getBucket());
		assertEquals(endpointUrl, result.getEndpointUrl());
		assertEquals(UploadType.HTTPS, result.getUploadType());
		assertEquals(BANNER, result.getBanner());
	}

	@Test
	public void testGetUploadDestination_ProxyStorageNotSupported() {
		when(mockStorageLocationDao.get(proxyStorageLocationId)).thenReturn(proxyStorageLocationSettings);
		Exception ex = assertThrows(IllegalArgumentException.class, () -> manager.getUploadDestination(mockUser,
				PARENT_ENTITY_ID, proxyStorageLocationId));
		assertEquals("Cannot handle upload destination location setting of type: org.sagebionetworks.repo.model.project.ProxyStorageLocationSettings",
				ex.getMessage());
	}
	
	@Test
	public void testIsMatchingMD5() {
		String sourceId = "123";
		String targetId = "456";
		
		boolean expected = true;
		
		when(mockFileHandleDao.isMatchingMD5(any(), any())).thenReturn(expected);
		
		// Call under test
		boolean result = manager.isMatchingMD5(sourceId, targetId);
		
		assertEquals(expected, result);

		verify(mockFileHandleDao).isMatchingMD5(sourceId, targetId);
	}
	
	@Test
	public void testIsMatchingMD5AndNotMatching() {
		String sourceId = "123";
		String targetId = "456";
		
		boolean expected = false;
		
		when(mockFileHandleDao.isMatchingMD5(any(), any())).thenReturn(expected);
		
		// Call under test
		boolean result = manager.isMatchingMD5(sourceId, targetId);
		
		assertEquals(expected, result);

		verify(mockFileHandleDao).isMatchingMD5(sourceId, targetId);
	}
	
	@Test
	public void testIsMatchingMD5WithNullSource() {
		String sourceId = null;
		String targetId = "456";
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.isMatchingMD5(sourceId, targetId);
		});
		
		assertEquals("The sourceFileHandleId is required.", ex.getMessage());
		
	}
	
	@Test
	public void testIsMatchingMD5WithNullTarget() {
		String sourceId = "123";
		String targetId = null;
		
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.isMatchingMD5(sourceId, targetId);
		});
		
		assertEquals("The targetFileHandleId is required.", ex.getMessage());
		
	}
}
