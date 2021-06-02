package org.sagebionetworks.repo.manager.file.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartRequestUtils;
import org.sagebionetworks.repo.model.file.AbortMultipartRequest;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.BucketOwnerStorageLocationSetting;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAO;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAOProvider;
import org.sagebionetworks.upload.multipart.MultipartUploadUtils;
import org.sagebionetworks.upload.multipart.PresignedUrl;

@ExtendWith(MockitoExtension.class)
public class MultipartUploadRequestHandlerTest {
	
	@Mock
	private CloudServiceMultipartUploadDAO mockCloudDao;
	
	@Mock
	private CloudServiceMultipartUploadDAOProvider mockCloudDaoProvider;

	@InjectMocks
	private MultipartUploadRequestHandler handler;
	
	@Mock
	private UserInfo mockUser;
	
	@Mock
	private MultipartUploadRequest mockRequest;
	
	@Mock
	private BucketOwnerStorageLocationSetting mockStorageLocation;
	
	@Mock
	private CompositeMultipartUploadStatus mockStatus;
	
	@Test
	public void testGetRequestClass() {
		// Call under test
		assertEquals(MultipartUploadRequest.class, handler.getRequestClass());
	}
	
	@Test
	public void testValidateRequest() {
		String fileName = "fileName";
		Long fileSize = 1024L;
		String md5 = "md5";
		
		when(mockRequest.getFileName()).thenReturn(fileName);
		when(mockRequest.getFileSizeBytes()).thenReturn(fileSize);
		when(mockRequest.getContentMD5Hex()).thenReturn(md5);
		
		// Call under test
		handler.validateRequest(mockUser, mockRequest);
	}
	
	@Test
	public void testValidateRequestWithEmptyFileName() {
		String fileName = "";
		
		when(mockRequest.getFileName()).thenReturn(fileName);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.validateRequest(mockUser, mockRequest);
		}).getMessage();
		
		assertEquals("MultipartUploadRequest.fileName is required and must not be the empty string.", errorMessage);
	}
	
	@Test
	public void testValidateRequestWithNoSize() {
		String fileName = "fileName";
		Long fileSize = null;
		
		when(mockRequest.getFileName()).thenReturn(fileName);
		when(mockRequest.getFileSizeBytes()).thenReturn(fileSize);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.validateRequest(mockUser, mockRequest);
		}).getMessage();
		
		assertEquals("MultipartUploadRequest.fileSizeBytes is required.", errorMessage);
	}
	
	@Test
	public void testValidateRequestWithEmptyMD5() {
		String fileName = "fileName";
		Long fileSize = 1024L;
		String md5 = null;
		
		when(mockRequest.getFileName()).thenReturn(fileName);
		when(mockRequest.getFileSizeBytes()).thenReturn(fileSize);
		when(mockRequest.getContentMD5Hex()).thenReturn(md5);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.validateRequest(mockUser, mockRequest);
		}).getMessage();
		
		assertEquals("MultipartUploadRequest.contentMD5Hex is required and must not be the empty string.", errorMessage);
	}
	
	@Test
	public void testValidateRequestWithFileNameNoAscii() {
		String fileName = "文件.txt";
		
		when(mockRequest.getFileName()).thenReturn(fileName);
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.validateRequest(mockUser, mockRequest);
		}).getMessage();
		
		assertEquals("Invalid Name: '文件.txt'. Names may only contain: letters, numbers, spaces, underscores, hyphens, periods, plus signs, apostrophes, and parentheses", errorMessage);
	}
	
	@Test
	public void testInitiateRequest() {
		String fileName = "fileName";
		Long partSize = PartUtils.MIN_PART_SIZE_BYTES;
		Long fileSize = 1024L;
		
		Long userId = 1L;
		String requestHash = "hash";
		String uploadToken = "token";
		UploadType uploadType = UploadType.S3;
		
		when(mockRequest.getFileName()).thenReturn(fileName);
		when(mockRequest.getFileSizeBytes()).thenReturn(fileSize);
		when(mockRequest.getPartSizeBytes()).thenReturn(partSize);
		when(mockUser.getId()).thenReturn(userId);
		when(mockStorageLocation.getUploadType()).thenReturn(uploadType);
		when(mockCloudDaoProvider.getCloudServiceMultipartUploadDao(any())).thenReturn(mockCloudDao);
		when(mockCloudDao.initiateMultipartUpload(any(), any(), any())).thenReturn(uploadToken);
		
		String requestBody = MultipartRequestUtils.createRequestJSON(mockRequest);
		
		CreateMultipartRequest expected = new CreateMultipartRequest(userId,
				requestHash, requestBody, uploadToken, uploadType,
				null, null, 1, partSize);
		
		// Call under test
		CreateMultipartRequest result = handler.initiateRequest(mockUser, mockRequest, requestHash, mockStorageLocation);
		
		expected.setBucket(result.getBucket());
		expected.setKey(result.getKey());
		
		assertEquals(expected, result);
		
		verify(mockCloudDaoProvider).getCloudServiceMultipartUploadDao(uploadType);
		verify(mockCloudDao).initiateMultipartUpload(result.getBucket(), result.getKey(), mockRequest);		
		
	}
	
	@Test
	public void testInitiateRequestWithNoStorageLocation() {
		StorageLocationSetting storageLocation = null;
		String requestHash = "hash";
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			handler.initiateRequest(mockUser, mockRequest, requestHash, storageLocation);
		}).getMessage();
		
		assertEquals("The storage location is required.", errorMessage);
		
		verifyZeroInteractions(mockCloudDaoProvider);
		verifyZeroInteractions(mockCloudDao);		
		
	}
	
	@Test
	public void testGetPresignedUrl() throws MalformedURLException {
		String contentType = "plain/text";
		UploadType uploadType = UploadType.S3;
		String bucket = "bucket";
		String key = "key";
		Long partNumber = 1L;
		
		PresignedUrl url = new PresignedUrl().withUrl(new URL("https://some.url"));
		
		when(mockStatus.getBucket()).thenReturn(bucket);
		when(mockStatus.getKey()).thenReturn(key);
		when(mockStatus.getUploadType()).thenReturn(uploadType);
		when(mockCloudDaoProvider.getCloudServiceMultipartUploadDao(any())).thenReturn(mockCloudDao);
		when(mockCloudDao.createPartUploadPreSignedUrl(any(), any(), any())).thenReturn(url);
		
		String partKey = MultipartUploadUtils.createPartKey(key, partNumber);
		
		// Call under test
		PresignedUrl result = handler.getPresignedUrl(mockStatus, 1, contentType);
	
		assertEquals(url, result);
		
		verify(mockCloudDaoProvider).getCloudServiceMultipartUploadDao(uploadType);
		verify(mockCloudDao).createPartUploadPreSignedUrl(bucket, partKey, contentType);
		
	}
	
	@Test
	public void testValidateAddedPart() {
		String partMD5Hex = "md5";
		UploadType uploadType = UploadType.S3;
		String uploadId = "id";
		String uploadToken = "token";
		String bucket = "bucket";
		String key = "key";
		Long partNumber = 1L;
		
		MultipartUploadStatus uploadStatus = new MultipartUploadStatus();
		uploadStatus.setUploadId(uploadId);
		
		when(mockStatus.getMultipartUploadStatus()).thenReturn(uploadStatus);
		when(mockStatus.getUploadToken()).thenReturn(uploadToken);
		when(mockStatus.getBucket()).thenReturn(bucket);
		when(mockStatus.getKey()).thenReturn(key);
		when(mockStatus.getUploadType()).thenReturn(uploadType);
		when(mockCloudDaoProvider.getCloudServiceMultipartUploadDao(any())).thenReturn(mockCloudDao);
		
		String partKey = MultipartUploadUtils.createPartKey(key, partNumber);
		
		// Call under test
		handler.validateAddedPart(mockStatus, partNumber, partMD5Hex);
		
		AddPartRequest request = new AddPartRequest(uploadId, uploadToken, bucket, key, partKey, partMD5Hex, partNumber, 1);
		
		verify(mockCloudDaoProvider).getCloudServiceMultipartUploadDao(uploadType);
		verify(mockCloudDao).validateAndAddPart(request);
	}
	
	@Test
	public void testGetFileHandleCreateRequest() {
		String fileName = "fileName";
		Long partSize = PartUtils.MIN_PART_SIZE_BYTES;
		Long fileSize = 1024L;
		String contentMd5 = "md5";
		String contentType = "plain/text";
		Long storageLocationId = 1L;
		Boolean generatePreview = true;
		
		
		MultipartUploadRequest request = new MultipartUploadRequest();
		
		request.setFileName(fileName);
		request.setPartSizeBytes(partSize);
		request.setFileSizeBytes(fileSize);
		request.setContentMD5Hex(contentMd5);
		request.setContentType(contentType);
		request.setStorageLocationId(storageLocationId);
		request.setGeneratePreview(generatePreview);
		
		String originalRequest = MultipartRequestUtils.createRequestJSON(request);
		
		FileHandleCreateRequest expected = new FileHandleCreateRequest(fileName, contentType, contentMd5, storageLocationId, generatePreview);
		
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
		int numberOfParts = 10;
		
		MultipartUploadStatus uploadStatus = new MultipartUploadStatus();
		uploadStatus.setUploadId(uploadId);
		
		when(mockStatus.getMultipartUploadStatus()).thenReturn(uploadStatus);
		when(mockStatus.getUploadToken()).thenReturn(uploadToken);
		when(mockStatus.getBucket()).thenReturn(bucket);
		when(mockStatus.getKey()).thenReturn(key);
		when(mockStatus.getUploadType()).thenReturn(uploadType);
		when(mockStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCloudDaoProvider.getCloudServiceMultipartUploadDao(any())).thenReturn(mockCloudDao);
		
		List<String> expectedPartKeys = IntStream.range(1, numberOfParts + 1)
				.boxed()
				.map( p -> MultipartUploadUtils.createPartKey(key, p))
				.collect(Collectors.toList());
		
		AbortMultipartRequest expectedRequest = new AbortMultipartRequest(uploadId, uploadToken, bucket, key).withPartKeys(expectedPartKeys);
		
		// Call under test
		handler.tryAbortMultipartRequest(mockStatus);
		
		verify(mockCloudDao).tryAbortMultipartRequest(expectedRequest);
	}
	
}
