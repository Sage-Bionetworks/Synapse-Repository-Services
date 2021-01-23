package org.sagebionetworks.repo.manager.file.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.manager.file.FileHandleAssociationAuthorizationStatus;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationManager;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.auth.AuthorizationStatus;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartRequestUtils;
import org.sagebionetworks.repo.model.file.AbortMultipartRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.file.FileHandleAssociation;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAO;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAOProvider;
import org.sagebionetworks.upload.multipart.PresignedUrl;

@ExtendWith(MockitoExtension.class)
public class MultipartUploadCopyRequestHandlerTest {
	
	@Mock
	private CloudServiceMultipartUploadDAO mockCloudDao;
	
	@Mock
	private CloudServiceMultipartUploadDAOProvider mockCloudDaoProvider;
	
	@Mock
	private FileHandleAuthorizationManager mockAuthManager;
	
	@Mock
	private FileHandleDao mockFileHandleDao;

	@InjectMocks
	private MultipartUploadCopyRequestHandler handler;
	
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private MultipartUploadCopyRequest mockRequest;
	
	@Mock
	private BucketOwnerStorageLocationSetting mockStorageLocation;
	
	@Mock
	private CompositeMultipartUploadStatus mockStatus;
	
	private S3FileHandle sourceFileHandle;
	private String sourceFileEntityId;
	private FileHandleAssociation fileHandleAssociation;
	
	@BeforeEach
	public void before() {
		sourceFileEntityId = "456";
		sourceFileHandle = new S3FileHandle();
		sourceFileHandle.setId("123");
		sourceFileHandle.setFileName("filename");
		sourceFileHandle.setContentMd5("contentMd5");
		sourceFileHandle.setContentSize(1024L);
		sourceFileHandle.setConcreteType("plain/text");
		sourceFileHandle.setBucketName("bucket");
		sourceFileHandle.setKey("key");
		
		fileHandleAssociation = new FileHandleAssociation();
		fileHandleAssociation.setAssociateObjectType(FileHandleAssociateType.FileEntity);
		fileHandleAssociation.setAssociateObjectId(sourceFileEntityId);
		fileHandleAssociation.setFileHandleId(sourceFileHandle.getId());
	}
	
	@Test
	public void testGetRequestClass() {
		// Call under test
		assertEquals(MultipartUploadCopyRequest.class, handler.getRequestClass());
	}
	
	@Test
	public void testValidateRequest() {
		String fileName = "fileName";
		Long storageLocationId = 678L;
		
		when(mockRequest.getFileName()).thenReturn(fileName);
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);
		when(mockRequest.getStorageLocationId()).thenReturn(storageLocationId);

		// Call under test
		handler.validateRequest(mockUser, mockRequest);
	}
	
	@Test
	public void testValidateRequestWithNoFileName() {
		String fileName = null;
		Long storageLocationId = 678L;
		
		when(mockRequest.getFileName()).thenReturn(fileName);
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);
		when(mockRequest.getStorageLocationId()).thenReturn(storageLocationId);

		// Call under test
		handler.validateRequest(mockUser, mockRequest);
	}
	
	@Test
	public void testValidateRequestWithInvalidFileName() {
		Long storageLocationId = 123L;
		String fileName = "文件.txt";
		
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);
		when(mockRequest.getStorageLocationId()).thenReturn(storageLocationId);
		when(mockRequest.getFileName()).thenReturn(fileName);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.validateRequest(mockUser, mockRequest);
		}).getMessage();
		
		assertEquals("Invalid Name: '文件.txt'. Names may only contain: letters, numbers, spaces, underscores, hyphens, periods, plus signs, apostrophes, and parentheses", errorMessage);
	}
	
	@Test
	public void testValidateRequestWithNoStorageLocation() {
		Long storageLocationId = null;
		
		when(mockRequest.getStorageLocationId()).thenReturn(storageLocationId);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.validateRequest(mockUser, mockRequest);
		}).getMessage();
		
		assertEquals("MultipartUploadCopyRequest.storageLocationId is required.", errorMessage);
	}
	
	@Test
	public void testValidateRequestWithNoFileAssociation() {
		Long storageLocationId = 456L;
		fileHandleAssociation = null;
		
		when(mockRequest.getStorageLocationId()).thenReturn(storageLocationId);
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.validateRequest(mockUser, mockRequest);
		}).getMessage();
		
		assertEquals("MultipartUploadCopyRequest.sourceFileHandleAssociation is required.", errorMessage);
	}
	
	@Test
	public void testValidateRequestWithNoFileHandleId() {
		Long storageLocationId = 456L;
		fileHandleAssociation.setFileHandleId(null);
		
		when(mockRequest.getStorageLocationId()).thenReturn(storageLocationId);
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.validateRequest(mockUser, mockRequest);
		}).getMessage();
		
		assertEquals("MultipartUploadCopyRequest.sourceFileHandleAssociation.fileHandleId is required.", errorMessage);
	}
	
	@Test
	public void testValidateRequestWithNoAssociateObjectId() {
		Long storageLocationId = 456L;
		fileHandleAssociation.setAssociateObjectId(null);
		
		when(mockRequest.getStorageLocationId()).thenReturn(storageLocationId);
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.validateRequest(mockUser, mockRequest);
		}).getMessage();
		
		assertEquals("MultipartUploadCopyRequest.sourceFileHandleAssociation.associateObjectId is required.", errorMessage);
	}
	
	@Test
	public void testValidateRequestWithNoAssociateObjectType() {
		Long storageLocationId = 456L;
		fileHandleAssociation.setAssociateObjectType(null);
		
		when(mockRequest.getStorageLocationId()).thenReturn(storageLocationId);
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.validateRequest(mockUser, mockRequest);
		}).getMessage();
		
		assertEquals("MultipartUploadCopyRequest.sourceFileHandleAssociation.associateObjectType is required.", errorMessage);
	}
	
	@Test
	public void testInitiateRequest() {
		Long partSize = PartUtils.MIN_PART_SIZE_BYTES;
		Long storageLocationId = 456L;
		Long userId = 123456L;
		String requestHash = "hash";
		String uploadToken = "token";
		String sourceEtag = "etag";
		UploadType uploadType = UploadType.S3;
		
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);
		when(mockRequest.getPartSizeBytes()).thenReturn(partSize);
		when(mockUser.getId()).thenReturn(userId);
		when(mockStorageLocation.getStorageLocationId()).thenReturn(storageLocationId);
		when(mockStorageLocation.getCreatedBy()).thenReturn(userId);
		when(mockStorageLocation.getUploadType()).thenReturn(uploadType);
		when(mockAuthManager.canDownLoadFile(any(), any())).thenReturn(Collections.singletonList(new FileHandleAssociationAuthorizationStatus(fileHandleAssociation, AuthorizationStatus.authorized())));
		when(mockFileHandleDao.get(any())).thenReturn(sourceFileHandle);
		when(mockCloudDaoProvider.getCloudServiceMultipartUploadDao(any())).thenReturn(mockCloudDao);
		when(mockCloudDao.getObjectEtag(any(), any())).thenReturn(sourceEtag);
		when(mockCloudDao.initiateMultipartUploadCopy(any(), any(), any(), any())).thenReturn(uploadToken);
		
		String requestBody = MultipartRequestUtils.createRequestJSON(mockRequest);
		
		CreateMultipartRequest expected = new CreateMultipartRequest(userId,
				requestHash, requestBody, uploadToken, uploadType,
				null, null, 1, partSize, sourceFileHandle.getId(), sourceEtag);
		
		// Call under test
		CreateMultipartRequest result = handler.initiateRequest(mockUser, mockRequest, requestHash, mockStorageLocation);
		
		expected.setBucket(result.getBucket());
		expected.setKey(result.getKey());
		
		assertEquals(expected, result);
		
		verify(mockAuthManager).canDownLoadFile(mockUser, Collections.singletonList(fileHandleAssociation));
		verify(mockFileHandleDao).get(sourceFileHandle.getId());
		verify(mockCloudDaoProvider).getCloudServiceMultipartUploadDao(uploadType);
		verify(mockCloudDao).getObjectEtag(sourceFileHandle.getBucketName(), sourceFileHandle.getKey());
		verify(mockCloudDao).initiateMultipartUploadCopy(result.getBucket(), result.getKey(), mockRequest, sourceFileHandle);		
	}
	
	@Test
	public void testInitiateRequestWithWrongFileHandleType() {
		Long storageLocationId = 456L;
		Long userId = 123456L;
		String requestHash = "hash";
		UploadType uploadType = UploadType.S3;
		
		// Not a CloudProviderFileHandleInterface
		FileHandle sourceFileHandle = new ExternalFileHandle();
		
		sourceFileHandle.setId("123");
		sourceFileHandle.setFileName("filename");
		sourceFileHandle.setContentMd5("contentMd5");
		sourceFileHandle.setContentSize(1024L);
		sourceFileHandle.setConcreteType("plain/text");
		
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);
		when(mockUser.getId()).thenReturn(userId);
		when(mockStorageLocation.getStorageLocationId()).thenReturn(storageLocationId);
		when(mockStorageLocation.getCreatedBy()).thenReturn(userId);
		when(mockStorageLocation.getUploadType()).thenReturn(uploadType);
		when(mockAuthManager.canDownLoadFile(any(), any())).thenReturn(Collections.singletonList(new FileHandleAssociationAuthorizationStatus(fileHandleAssociation, AuthorizationStatus.authorized())));
		when(mockFileHandleDao.get(any())).thenReturn(sourceFileHandle);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.initiateRequest(mockUser, mockRequest, requestHash, mockStorageLocation);
		}).getMessage();
		
		assertEquals("The source file must be stored in a cloud bucket accessible by Synapse.", errorMessage);
		
		verify(mockAuthManager).canDownLoadFile(mockUser, Collections.singletonList(fileHandleAssociation));
		verify(mockFileHandleDao).get(sourceFileHandle.getId());
		verifyZeroInteractions(mockCloudDaoProvider);
		verifyZeroInteractions(mockCloudDao);	
	}
	
	@Test
	public void testInitiateRequestAndFileWithNoContentMD5() {
		sourceFileHandle.setContentMd5(null);
		
		Long userId = 123456L;
		String requestHash = "hash";
		UploadType uploadType = UploadType.S3;
		
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);
		when(mockUser.getId()).thenReturn(userId);
		when(mockStorageLocation.getCreatedBy()).thenReturn(userId);
		when(mockStorageLocation.getUploadType()).thenReturn(uploadType);
		when(mockAuthManager.canDownLoadFile(any(), any())).thenReturn(Collections.singletonList(new FileHandleAssociationAuthorizationStatus(fileHandleAssociation, AuthorizationStatus.authorized())));
		when(mockFileHandleDao.get(any())).thenReturn(sourceFileHandle);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.initiateRequest(mockUser, mockRequest, requestHash, mockStorageLocation);
		}).getMessage();
		
		assertEquals("The source file handle does not define its content MD5.", errorMessage);
		
		verify(mockAuthManager).canDownLoadFile(mockUser, Collections.singletonList(fileHandleAssociation));
		verify(mockFileHandleDao).get(sourceFileHandle.getId());
		verifyZeroInteractions(mockCloudDaoProvider);
		verifyZeroInteractions(mockCloudDao);
	}
	
	@Test
	public void testInitiateRequestAndFileWithNoContentSize() {
		sourceFileHandle.setContentSize(null);
		
		Long userId = 123456L;
		String requestHash = "hash";
		UploadType uploadType = UploadType.S3;
		
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);
		when(mockUser.getId()).thenReturn(userId);
		when(mockStorageLocation.getCreatedBy()).thenReturn(userId);
		when(mockStorageLocation.getUploadType()).thenReturn(uploadType);
		when(mockAuthManager.canDownLoadFile(any(), any())).thenReturn(Collections.singletonList(new FileHandleAssociationAuthorizationStatus(fileHandleAssociation, AuthorizationStatus.authorized())));
		when(mockFileHandleDao.get(any())).thenReturn(sourceFileHandle);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.initiateRequest(mockUser, mockRequest, requestHash, mockStorageLocation);
		}).getMessage();
		
		assertEquals("The source file handle does not define its size.", errorMessage);
		
		verify(mockAuthManager).canDownLoadFile(mockUser, Collections.singletonList(fileHandleAssociation));
		verify(mockFileHandleDao).get(sourceFileHandle.getId());
		verifyZeroInteractions(mockCloudDaoProvider);
		verifyZeroInteractions(mockCloudDao);
	}
	
	@Test
	public void testInitiateRequestAndSameStorageLocation() {
		Long storageLocationId = 456L;

		sourceFileHandle.setStorageLocationId(storageLocationId);
		
		Long userId = 123456L;
		String requestHash = "hash";
		UploadType uploadType = UploadType.S3;
		
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);
		when(mockUser.getId()).thenReturn(userId);
		when(mockStorageLocation.getStorageLocationId()).thenReturn(storageLocationId);
		when(mockStorageLocation.getCreatedBy()).thenReturn(userId);
		when(mockStorageLocation.getUploadType()).thenReturn(uploadType);
		when(mockAuthManager.canDownLoadFile(any(), any())).thenReturn(Collections.singletonList(new FileHandleAssociationAuthorizationStatus(fileHandleAssociation, AuthorizationStatus.authorized())));
		when(mockFileHandleDao.get(any())).thenReturn(sourceFileHandle);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			handler.initiateRequest(mockUser, mockRequest, requestHash, mockStorageLocation);			
		}).getMessage();
		
		assertEquals("The source file handle is already in the given destination storage location.", errorMessage);
		
		verify(mockAuthManager).canDownLoadFile(mockUser, Collections.singletonList(fileHandleAssociation));
		verify(mockFileHandleDao).get(sourceFileHandle.getId());
		verifyZeroInteractions(mockCloudDaoProvider);
		verifyZeroInteractions(mockCloudDao);
	}
	
	@Test
	public void testInitiateRequestAndUserIsNotStorageLocationOwner() {
		Long storageLocationId = 456L;

		sourceFileHandle.setStorageLocationId(storageLocationId);
		
		Long userId = 123456L;
		Long anotherUser = 789L;
		String requestHash = "hash";
		UploadType uploadType = UploadType.S3;
		
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);
		when(mockUser.getId()).thenReturn(userId);
		when(mockStorageLocation.getCreatedBy()).thenReturn(anotherUser);
		when(mockStorageLocation.getUploadType()).thenReturn(uploadType);
		when(mockAuthManager.canDownLoadFile(any(), any())).thenReturn(Collections.singletonList(new FileHandleAssociationAuthorizationStatus(fileHandleAssociation, AuthorizationStatus.authorized())));
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			handler.initiateRequest(mockUser, mockRequest, requestHash, mockStorageLocation);			
		}).getMessage();
		
		assertEquals("The user does not own the destination storage location.", errorMessage);
		
		verify(mockAuthManager).canDownLoadFile(mockUser, Collections.singletonList(fileHandleAssociation));
		verifyZeroInteractions(mockFileHandleDao);
		verifyZeroInteractions(mockCloudDaoProvider);
		verifyZeroInteractions(mockCloudDao);
	}
	
	@Test
	public void testInitiateRequestAndUserIsNotAuthorized() {
		Long storageLocationId = 456L;

		sourceFileHandle.setStorageLocationId(storageLocationId);
		
		String requestHash = "hash";
		UploadType uploadType = UploadType.S3;
		
		when(mockRequest.getSourceFileHandleAssociation()).thenReturn(fileHandleAssociation);
		when(mockStorageLocation.getUploadType()).thenReturn(uploadType);
		when(mockAuthManager.canDownLoadFile(any(), any())).thenReturn(
				Collections.singletonList(
						new FileHandleAssociationAuthorizationStatus(fileHandleAssociation, 
								AuthorizationStatus.accessDenied("denied"))));
		
		String errorMessage = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			handler.initiateRequest(mockUser, mockRequest, requestHash, mockStorageLocation);			
		}).getMessage();
		
		assertEquals("The user is not authorized to access the source file.", errorMessage);
		
		verify(mockAuthManager).canDownLoadFile(mockUser, Collections.singletonList(fileHandleAssociation));
		verifyZeroInteractions(mockFileHandleDao);
		verifyZeroInteractions(mockCloudDaoProvider);
		verifyZeroInteractions(mockCloudDao);
	}
	
	@Test
	public void testGetPresignedUrl() throws MalformedURLException {
		String contentType = "plain/text";
		UploadType uploadType = UploadType.S3;
		Long partNumber = 1L;
		
		PresignedUrl url = new PresignedUrl().withUrl(new URL("https://some.url"));
		
		when(mockStatus.getUploadType()).thenReturn(uploadType);
		when(mockCloudDaoProvider.getCloudServiceMultipartUploadDao(any())).thenReturn(mockCloudDao);
		when(mockCloudDao.createPartUploadCopyPresignedUrl(mockStatus, partNumber, contentType)).thenReturn(url);
		
		// Call under test
		PresignedUrl result = handler.getPresignedUrl(mockStatus, 1, contentType);
	
		assertEquals(url, result);
		
		verify(mockCloudDaoProvider).getCloudServiceMultipartUploadDao(uploadType);
		verify(mockCloudDao).createPartUploadCopyPresignedUrl(mockStatus, partNumber, contentType);
		
	}
	
	@Test
	public void testValidateAddedPart() {
		String partMD5Hex = "md5";
		UploadType uploadType = UploadType.S3;
		Long partNumber = 1L;
		
		when(mockStatus.getUploadType()).thenReturn(uploadType);
		when(mockCloudDaoProvider.getCloudServiceMultipartUploadDao(any())).thenReturn(mockCloudDao);
		
		// Call under test
		handler.validateAddedPart(mockStatus, partNumber, partMD5Hex);
		
		verify(mockCloudDaoProvider).getCloudServiceMultipartUploadDao(uploadType);
		verify(mockCloudDao).validatePartCopy(mockStatus, partNumber, partMD5Hex);
	}
	
	@Test
	public void testGetFileHandleCreateRequest() {
		String fileName = "fileName";
		Long partSize = PartUtils.MIN_PART_SIZE_BYTES;
		Long storageLocationId = 1L;
		Boolean generatePreview = true;
		
		when(mockFileHandleDao.get(any())).thenReturn(sourceFileHandle);
		
		MultipartUploadCopyRequest request = new MultipartUploadCopyRequest();
		
		request.setFileName(fileName);
		request.setPartSizeBytes(partSize);
		request.setSourceFileHandleAssociation(fileHandleAssociation);
		request.setStorageLocationId(storageLocationId);
		request.setGeneratePreview(generatePreview);
		
		String originalRequest = MultipartRequestUtils.createRequestJSON(request);
		
		FileHandleCreateRequest expected = new FileHandleCreateRequest(fileName, sourceFileHandle.getContentType(), sourceFileHandle.getContentMd5(), storageLocationId, generatePreview);
		
		// Call under test
		FileHandleCreateRequest result = handler.getFileHandleCreateRequest(mockStatus, originalRequest);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testAbortMultipartRequest() {
		UploadType uploadType = UploadType.S3;
		String uploadId = "id";
		String uploadToken = "token";
		String bucket = "bucket";
		String key = "key";
		
		MultipartUploadStatus uploadStatus = new MultipartUploadStatus();
		uploadStatus.setUploadId(uploadId);
		
		when(mockStatus.getMultipartUploadStatus()).thenReturn(uploadStatus);
		when(mockStatus.getUploadToken()).thenReturn(uploadToken);
		when(mockStatus.getBucket()).thenReturn(bucket);
		when(mockStatus.getKey()).thenReturn(key);
		when(mockStatus.getUploadType()).thenReturn(uploadType);
		when(mockCloudDaoProvider.getCloudServiceMultipartUploadDao(any())).thenReturn(mockCloudDao);
		
		List<String> expectedPartKeys = null;
		
		AbortMultipartRequest expectedRequest = new AbortMultipartRequest(uploadId, uploadToken, bucket, key).withPartKeys(expectedPartKeys);
		
		// Call under test
		handler.tryAbortMultipartRequest(mockStatus);

		verify(mockCloudDao).tryAbortMultipartRequest(expectedRequest);
	}
	
}
