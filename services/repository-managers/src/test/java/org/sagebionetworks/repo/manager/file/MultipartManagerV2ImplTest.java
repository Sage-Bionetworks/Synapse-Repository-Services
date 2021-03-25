package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.manager.file.multipart.FileHandleCreateRequest;
import org.sagebionetworks.repo.manager.file.multipart.MultipartRequestHandler;
import org.sagebionetworks.repo.manager.file.multipart.MultipartRequestHandlerProvider;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultiPartRequestType;
import org.sagebionetworks.repo.model.dbo.file.MultipartRequestUtils;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.AddPartState;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.MultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartMD5;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;
import org.sagebionetworks.repo.model.file.PartUtils;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.repo.model.project.StorageLocationSetting;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAO;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAOProvider;
import org.sagebionetworks.upload.multipart.PresignedUrl;

@ExtendWith(MockitoExtension.class)
public class MultipartManagerV2ImplTest {

	@Mock
	private MultipartUploadDAO mockMultipartUploadDAO;

	@Mock
	private FileHandleDao mockFileHandleDao;

	@Mock
	private StorageLocationDAO mockStorageLocationDao;

	@Mock
	private IdGenerator mockIdGenerator;

	@Mock
	private CloudServiceMultipartUploadDAOProvider mockCloudDaoProvider;

	@Mock
	private CloudServiceMultipartUploadDAO mockCloudDao;

	@Mock
	private MultipartRequestHandlerProvider mockHandlerProvider;

	@Mock
	private MultipartRequestHandler<MultipartRequest> mockHandler;

	@InjectMocks
	private MultipartManagerV2Impl manager;

	@Mock
	private MultipartUploadStatus mockStatus;

	@Mock
	private CompositeMultipartUploadStatus mockCompositeStatus;

	@Mock
	private MultipartRequest mockRequest;

	@Mock
	private StorageLocationSetting mockStorageSettings;

	@Mock
	private CreateMultipartRequest mockCreateMultipartRequest;

	@Mock
	private CloudProviderFileHandleInterface mockFileHandle;

	private UserInfo user;

	@BeforeEach
	public void before() {
		user = new UserInfo(false);
		user.setId(123L);
	}

	@Test
	public void testStartOrResumeMultipartOperationWithUpload() {

		MultipartUploadRequest request = new MultipartUploadRequest();

		boolean forceRestart = false;

		// We just want to verify that the right method is invoked
		MultipartManagerV2Impl managerSpy = Mockito.spy(manager);

		doReturn(mockStatus).when(managerSpy).startOrResumeMultipartUpload(any(), any(), anyBoolean());

		// Call under test
		managerSpy.startOrResumeMultipartOperation(user, request, forceRestart);

		verify(managerSpy).startOrResumeMultipartUpload(user, request, forceRestart);
	}

	@Test
	public void testStartOrResumeMultipartOperationWithCopy() {

		MultipartUploadCopyRequest request = new MultipartUploadCopyRequest();

		boolean forceRestart = false;

		// We just want to verify that the right method is invoked
		MultipartManagerV2Impl managerSpy = Mockito.spy(manager);

		doReturn(mockStatus).when(managerSpy).startOrResumeMultipartUploadCopy(any(), any(), anyBoolean());

		// Call under test
		managerSpy.startOrResumeMultipartOperation(user, request, forceRestart);

		verify(managerSpy).startOrResumeMultipartUploadCopy(user, request, forceRestart);
	}

	@Test
	public void testStartOrResumeMultipartOperationWithUnsupported() {

		MultipartRequest request = Mockito.mock(MultipartRequest.class);

		boolean forceRestart = false;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.startOrResumeMultipartOperation(user, request, forceRestart);
		}).getMessage();

		assertTrue(errorMessage.startsWith("Request type unsupported: MultipartRequest"));
	}

	@Test
	public void testStartOrResumeMultipartUpload() {
		MultipartUploadRequest request = new MultipartUploadRequest();

		boolean forceRestart = false;

		// We just want to verify that the right method is invoked
		MultipartManagerV2Impl managerSpy = Mockito.spy(manager);

		when(mockHandlerProvider.getHandlerForClass(any())).thenReturn(mockHandler);

		doReturn(mockStatus).when(managerSpy).startOrResumeMultipartRequest(any(), any(), any(), anyBoolean());

		// Call under test
		managerSpy.startOrResumeMultipartUpload(user, request, forceRestart);

		verify(mockHandlerProvider).getHandlerForClass(MultipartUploadRequest.class);
		verify(managerSpy).startOrResumeMultipartRequest(mockHandler, user, request, forceRestart);
	}

	@Test
	public void testStartOrResumeMultipartUploadCopy() {
		MultipartUploadCopyRequest request = new MultipartUploadCopyRequest();

		boolean forceRestart = false;

		// We just want to verify that the right method is invoked
		MultipartManagerV2Impl managerSpy = Mockito.spy(manager);

		when(mockHandlerProvider.getHandlerForClass(any())).thenReturn(mockHandler);

		doReturn(mockStatus).when(managerSpy).startOrResumeMultipartRequest(any(), any(), any(), anyBoolean());

		// Call under test
		managerSpy.startOrResumeMultipartUploadCopy(user, request, forceRestart);

		verify(mockHandlerProvider).getHandlerForClass(MultipartUploadCopyRequest.class);
		verify(managerSpy).startOrResumeMultipartRequest(mockHandler, user, request, forceRestart);
	}

	@Test
	public void testStartOrResumeMultipartRequestWithForceRestartTrue() {
		Long partSize = PartUtils.MIN_PART_SIZE_BYTES;
		String requestHash = MultipartRequestUtils.calculateMD5AsHex(mockRequest);
		int numberOfParts = 4;

		boolean forceRestart = true;

		when(mockRequest.getPartSizeBytes()).thenReturn(partSize);
		doNothing().when(mockHandler).validateRequest(any(), any());
		doNothing().when(mockMultipartUploadDAO).setUploadStatusHash(anyLong(), any(), any());

		// Mimic a new upload
		when(mockMultipartUploadDAO.getUploadStatus(any(), any())).thenReturn(null);
		when(mockStorageLocationDao.get(any())).thenReturn(mockStorageSettings);
		when(mockHandler.initiateRequest(any(), any(), any(), any())).thenReturn(mockCreateMultipartRequest);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockMultipartUploadDAO.createUploadStatus(any())).thenReturn(mockCompositeStatus);
		when(mockMultipartUploadDAO.getPartsState(any(), anyInt())).thenReturn("0000");

		// Call under test
		MultipartUploadStatus result = manager.startOrResumeMultipartRequest(mockHandler, user, mockRequest,
				forceRestart);

		assertEquals(mockStatus, result);

		verify(mockHandler).validateRequest(user, mockRequest);
		verify(mockMultipartUploadDAO).setUploadStatusHash(eq(user.getId()), eq(requestHash), startsWith("R_" + requestHash + "_"));
		verify(mockMultipartUploadDAO).getUploadStatus(user.getId(), requestHash);
		verify(mockHandler).initiateRequest(user, mockRequest, requestHash, mockStorageSettings);
		verify(mockMultipartUploadDAO).createUploadStatus(mockCreateMultipartRequest);
		verify(mockStatus).setPartsState("0000");
	}

	@Test
	public void testStartOrResumeMultipartRequestWithNoForceRestartFalse() {
		Long partSize = PartUtils.MIN_PART_SIZE_BYTES;
		String requestHash = MultipartRequestUtils.calculateMD5AsHex(mockRequest);
		int numberOfParts = 4;

		boolean forceRestart = false;

		when(mockRequest.getPartSizeBytes()).thenReturn(partSize);
		doNothing().when(mockHandler).validateRequest(any(), any());

		// Mimic a new upload
		when(mockMultipartUploadDAO.getUploadStatus(any(), any())).thenReturn(null);
		when(mockStorageLocationDao.get(any())).thenReturn(mockStorageSettings);
		when(mockHandler.initiateRequest(any(), any(), any(), any())).thenReturn(mockCreateMultipartRequest);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockMultipartUploadDAO.createUploadStatus(any())).thenReturn(mockCompositeStatus);
		when(mockMultipartUploadDAO.getPartsState(any(), anyInt())).thenReturn("0000");

		// Call under test
		MultipartUploadStatus result = manager.startOrResumeMultipartRequest(mockHandler, user, mockRequest,
				forceRestart);

		assertEquals(mockStatus, result);

		verify(mockHandler).validateRequest(user, mockRequest);
		verify(mockMultipartUploadDAO, never()).setUploadStatusHash(anyLong(), any(), any());
		verify(mockMultipartUploadDAO).getUploadStatus(user.getId(), requestHash);
		verify(mockHandler).initiateRequest(user, mockRequest, requestHash, mockStorageSettings);
		verify(mockMultipartUploadDAO).createUploadStatus(mockCreateMultipartRequest);
		verify(mockStatus).setPartsState("0000");

	}

	@Test
	public void testStartOrResumeMultipartRequestWithAlreadyStarted() {
		Long partSize = PartUtils.MIN_PART_SIZE_BYTES;
		String requestHash = MultipartRequestUtils.calculateMD5AsHex(mockRequest);
		int numberOfParts = 4;

		boolean forceRestart = false;

		when(mockRequest.getPartSizeBytes()).thenReturn(partSize);
		doNothing().when(mockHandler).validateRequest(any(), any());

		// Mimic a new upload
		when(mockMultipartUploadDAO.getUploadStatus(any(), any())).thenReturn(mockCompositeStatus);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockMultipartUploadDAO.getPartsState(any(), anyInt())).thenReturn("0000");

		// Call under test
		MultipartUploadStatus result = manager.startOrResumeMultipartRequest(mockHandler, user, mockRequest,
				forceRestart);

		assertEquals(mockStatus, result);

		verify(mockHandler).validateRequest(user, mockRequest);
		verify(mockMultipartUploadDAO, never()).setUploadStatusHash(anyLong(), any(), any());
		verify(mockMultipartUploadDAO).getUploadStatus(user.getId(), requestHash);
		verify(mockStatus).setPartsState("0000");
		verify(mockHandler, never()).initiateRequest(any(), any(), any(), any());
		verify(mockMultipartUploadDAO, never()).createUploadStatus(any());
	}

	@Test
	public void testStartOrResumeMultipartRequestWithCompleted() {
		Long partSize = PartUtils.MIN_PART_SIZE_BYTES;
		String requestHash = MultipartRequestUtils.calculateMD5AsHex(mockRequest);
		int numberOfParts = 4;

		boolean forceRestart = false;

		when(mockRequest.getPartSizeBytes()).thenReturn(partSize);
		doNothing().when(mockHandler).validateRequest(any(), any());

		// Mimic a new upload
		when(mockMultipartUploadDAO.getUploadStatus(any(), any())).thenReturn(mockCompositeStatus);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockStatus.getState()).thenReturn(MultipartUploadState.COMPLETED);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);

		// Call under test
		MultipartUploadStatus result = manager.startOrResumeMultipartRequest(mockHandler, user, mockRequest,
				forceRestart);

		assertEquals(mockStatus, result);

		verify(mockHandler).validateRequest(user, mockRequest);
		verify(mockMultipartUploadDAO, never()).setUploadStatusHash(anyLong(), any(), any());
		verify(mockMultipartUploadDAO).getUploadStatus(user.getId(), requestHash);
		verify(mockStatus).setPartsState("1111");
		verify(mockHandler, never()).initiateRequest(any(), any(), any(), any());
		verify(mockMultipartUploadDAO, never()).createUploadStatus(any());
		verify(mockMultipartUploadDAO, never()).getPartsState(any(), anyInt());
	}

	@Test
	public void testStartOrResumeMultipartRequestWithAnonymous() {

		Long partSize = PartUtils.MIN_PART_SIZE_BYTES;
		boolean forceRestart = false;

		// set the user to anonymous
		user.setId(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());

		when(mockRequest.getPartSizeBytes()).thenReturn(partSize);

		String errorMessage = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.startOrResumeMultipartRequest(mockHandler, user, mockRequest, forceRestart);
		}).getMessage();

		assertEquals("Anonymous cannot upload files.", errorMessage);
	}
	
	// Test for PLFM-6533, if a cloud provider does not support the copy an UnsupportedOperationException needs to be translated to a 400
	@Test
	public void testStartOrResumeMultipartRequestWithUnsupportedOperationException() {
		Long partSize = PartUtils.MIN_PART_SIZE_BYTES;
		String requestHash = MultipartRequestUtils.calculateMD5AsHex(mockRequest);
		
		boolean forceRestart = false;

		when(mockRequest.getPartSizeBytes()).thenReturn(partSize);
		doNothing().when(mockHandler).validateRequest(any(), any());

		// Mimic a new upload
		when(mockMultipartUploadDAO.getUploadStatus(any(), any())).thenReturn(null);
		when(mockStorageLocationDao.get(any())).thenReturn(mockStorageSettings);
		
		UnsupportedOperationException cause = new UnsupportedOperationException("Unsupported");
		
		doThrow(cause).when(mockHandler).initiateRequest(any(), any(), any(), any());
				
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.startOrResumeMultipartRequest(mockHandler, user, mockRequest,forceRestart);
		});

		assertEquals(cause.getMessage(), ex.getMessage());
		assertEquals(cause, ex.getCause());

		verify(mockHandler).validateRequest(user, mockRequest);
		verify(mockMultipartUploadDAO, never()).setUploadStatusHash(anyLong(), any(), any());
		verify(mockMultipartUploadDAO).getUploadStatus(user.getId(), requestHash);
		verify(mockHandler).initiateRequest(user, mockRequest, requestHash, mockStorageSettings);
		verify(mockMultipartUploadDAO, never()).createUploadStatus(mockCreateMultipartRequest);
	}

	@Test
	public void testGetBatchPresignedUploadUrls() throws MalformedURLException {
		String uploadId = "upload";
		int numberOfParts = 2;

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		doReturn(mockHandler).when(mockHandlerProvider).getHandlerForType(any());

		URL url1 = new URL("http://amazon.comsomeBucket/someKey/1");
		URL url2 = new URL("http://amazon.comsomeBucket/someKey/1");

		when(mockHandler.getPresignedUrl(any(), anyLong(), any())).thenReturn(new PresignedUrl().withUrl(url1),
				new PresignedUrl().withUrl(url2));

		PartPresignedUrl part1 = new PartPresignedUrl();
		part1.setPartNumber(1L);
		part1.setUploadPresignedUrl(url1.toString());

		PartPresignedUrl part2 = new PartPresignedUrl();
		part2.setPartNumber(2L);
		part2.setUploadPresignedUrl(url2.toString());

		List<PartPresignedUrl> expectedUrls = Arrays.asList(part1, part2);

		BatchPresignedUploadUrlResponse expected = new BatchPresignedUploadUrlResponse();
		expected.setPartPresignedUrls(expectedUrls);

		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();

		request.setUploadId(uploadId);
		request.setPartNumbers(Arrays.asList(1L, 2L));
		request.setContentType("plain/text");

		// Call under test
		BatchPresignedUploadUrlResponse result = manager.getBatchPresignedUploadUrls(user, request);

		assertEquals(expected, result);

		verify(mockMultipartUploadDAO).getUploadStatus(uploadId);

		verify(mockHandler).getPresignedUrl(mockCompositeStatus, 1L, "plain/text");
		verify(mockHandler).getPresignedUrl(mockCompositeStatus, 2L, "plain/text");
	}

	@Test
	public void testGetBatchPresignedUploadUrlsWithNullPart() throws MalformedURLException {
		String uploadId = "upload";
		int numberOfParts = 2;

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		doReturn(mockHandler).when(mockHandlerProvider).getHandlerForType(any());

		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();

		request.setUploadId(uploadId);
		request.setPartNumbers(Arrays.asList(null, 2L));
		request.setContentType("plain/text");

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.getBatchPresignedUploadUrls(user, request);
		}).getMessage();

		assertEquals("PartNumber is required.", errorMessage);

		verifyZeroInteractions(mockHandler);
	}

	@Test
	public void testGetBatchPresignedUploadUrlsWithZeroPart() throws MalformedURLException {
		String uploadId = "upload";
		int numberOfParts = 2;

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		doReturn(mockHandler).when(mockHandlerProvider).getHandlerForType(any());

		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();

		request.setUploadId(uploadId);
		request.setPartNumbers(Arrays.asList(0L, 1L));
		request.setContentType("plain/text");

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.getBatchPresignedUploadUrls(user, request);
		}).getMessage();

		assertEquals("Part numbers cannot be less than one.", errorMessage);

		verifyZeroInteractions(mockHandler);
	}

	@Test
	public void testGetBatchPresignedUploadUrlsWithPartNumberTooBig() throws MalformedURLException {
		String uploadId = "upload";
		int numberOfParts = 2;

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		doReturn(mockHandler).when(mockHandlerProvider).getHandlerForType(any());

		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();

		request.setUploadId(uploadId);
		request.setPartNumbers(Arrays.asList(numberOfParts + 1L));
		request.setContentType("plain/text");

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.getBatchPresignedUploadUrls(user, request);
		}).getMessage();

		assertEquals("Part number cannot be larger than number of parts. Number of parts: 2, provided part number: 3",
				errorMessage);

		verifyZeroInteractions(mockHandler);
	}

	@Test
	public void testGetBatchPresignedUploadUrlsWithEmptyList() throws MalformedURLException {
		String uploadId = "upload";

		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();

		request.setUploadId(uploadId);
		request.setPartNumbers(Collections.emptyList());
		request.setContentType("plain/text");

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.getBatchPresignedUploadUrls(user, request);
		}).getMessage();

		assertEquals("BatchPresignedUploadUrlRequest.partNumbers must contain at least one value", errorMessage);

	}

	@Test
	public void testGetBatchPresignedUploadUrlsWithUnauthorizedUser() throws MalformedURLException {
		String uploadId = "upload";

		// Started by another user
		when(mockStatus.getStartedBy()).thenReturn("789");
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();

		request.setUploadId(uploadId);
		request.setPartNumbers(Arrays.asList(0L, 1L));
		request.setContentType("plain/text");

		String errorMessage = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.getBatchPresignedUploadUrls(user, request);
		}).getMessage();

		assertEquals(
				"Only the user that started a multipart upload can get part upload pre-signed URLs for that file upload.",
				errorMessage);

		verifyZeroInteractions(mockHandler);

	}

	@Test
	public void testAddMultipartPart() {
		String uploadId = "upload";
		Integer partNumber = 2;
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		Integer numberOfParts = 4;

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		doReturn(mockHandler).when(mockHandlerProvider).getHandlerForType(any());

		AddPartResponse expected = new AddPartResponse();

		expected.setPartNumber(Long.valueOf(partNumber));
		expected.setUploadId(uploadId);
		expected.setAddPartState(AddPartState.ADD_SUCCESS);

		// Call under test
		AddPartResponse result = manager.addMultipartPart(user, uploadId, partNumber, partMD5Hex);

		assertEquals(expected, result);

		verify(mockMultipartUploadDAO).getUploadStatus(uploadId);
		verify(mockHandler).validateAddedPart(mockCompositeStatus, partNumber, partMD5Hex);
		verify(mockMultipartUploadDAO).addPartToUpload(uploadId, partNumber, partMD5Hex);

	}

	@Test
	public void testAddMultipartPartCompleted() {
		String uploadId = "upload";
		Integer partNumber = 2;
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";

		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockStatus.getState()).thenReturn(MultipartUploadState.COMPLETED);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.addMultipartPart(user, uploadId, partNumber, partMD5Hex);
		}).getMessage();

		assertEquals("Cannot add parts to completed file upload.", errorMessage);

		verifyNoMoreInteractions(mockMultipartUploadDAO);
		verifyZeroInteractions(mockHandler);

	}

	@Test
	public void testAddMultipartPartFailed() {
		String uploadId = "upload";
		Integer partNumber = 2;
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		Integer numberOfParts = 4;

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		doReturn(mockHandler).when(mockHandlerProvider).getHandlerForType(any());

		Exception error = new RuntimeException("Something went wrong");

		doThrow(error).when(mockHandler).validateAddedPart(any(), anyLong(), any());

		AddPartResponse expected = new AddPartResponse();

		expected.setPartNumber(Long.valueOf(partNumber));
		expected.setUploadId(uploadId);
		expected.setAddPartState(AddPartState.ADD_FAILED);
		expected.setErrorMessage(error.getMessage());

		// Call under test
		AddPartResponse result = manager.addMultipartPart(user, uploadId, partNumber, partMD5Hex);

		assertEquals(expected, result);

		verify(mockMultipartUploadDAO).getUploadStatus(uploadId);
		verify(mockHandler).validateAddedPart(mockCompositeStatus, partNumber, partMD5Hex);
		verify(mockMultipartUploadDAO, never()).addPartToUpload(uploadId, partNumber, partMD5Hex);
		verify(mockMultipartUploadDAO).setPartToFailed(uploadId, partNumber, ExceptionUtils.getStackTrace(error));

	}

	@Test
	public void testAddMultipartPartWithUnauthorizedUser() {
		String uploadId = "upload";
		Integer partNumber = 2;
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		Integer numberOfParts = 4;

		// Started by a different user
		when(mockStatus.getStartedBy()).thenReturn("8796");
		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		String errorMessage = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.addMultipartPart(user, uploadId, partNumber, partMD5Hex);
		}).getMessage();

		assertEquals(
				"Only the user that started a multipart upload can get part upload pre-signed URLs for that file upload.",
				errorMessage);
	}

	@Test
	public void testAddMultipartPartBadPartNumber() {
		String uploadId = "upload";
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		Integer numberOfParts = 4;
		Integer partNumber = numberOfParts + 1;

		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.addMultipartPart(user, uploadId, partNumber, partMD5Hex);
		}).getMessage();

		assertEquals("Part number cannot be larger than number of parts. Number of parts: 4, provided part number: 5",
				errorMessage);
	}

	@Test
	public void testAddMultipartPartPartNumberLessThanOne() {
		String uploadId = "upload";
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		Integer numberOfParts = 4;
		Integer partNumber = 0;

		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.addMultipartPart(user, uploadId, partNumber, partMD5Hex);
		}).getMessage();

		assertEquals("Part numbers cannot be less than one.", errorMessage);
	}

	@Test
	public void testCompleteMultipartUpload() {
		String uploadId = "1234";
		String token = "token";
		Integer numberOfParts = 2;
		String bucket = "bucket";
		String key = "key";
		Long fileSize = PartUtils.MAX_PART_SIZE_BYTES;
		String fileHandleId = "12345";
		String originalRequest = "{}";

		List<PartMD5> addedParts = Arrays.asList(new PartMD5(2, "partMD5HexTwo"), new PartMD5(1, "partMD5HexOne"));

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		// The status is initially uploading, then it will be changed to completed
		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING, MultipartUploadState.COMPLETED);
		when(mockStatus.getUploadId()).thenReturn(uploadId);
		when(mockStatus.getResultFileHandleId()).thenReturn(fileHandleId);

		when(mockCompositeStatus.getUploadToken()).thenReturn(token);
		when(mockCompositeStatus.getBucket()).thenReturn(bucket);
		when(mockCompositeStatus.getKey()).thenReturn(key);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockCompositeStatus.getUploadType()).thenReturn(UploadType.S3);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);
		when(mockMultipartUploadDAO.getAddedPartMD5s(any())).thenReturn(addedParts);
		when(mockMultipartUploadDAO.getUploadRequest(any())).thenReturn(originalRequest);
		when(mockCloudDaoProvider.getCloudServiceMultipartUploadDao(any())).thenReturn(mockCloudDao);
		when(mockCloudDao.completeMultipartUpload(any())).thenReturn(fileSize);
		when(mockFileHandleDao.createFile(any())).thenReturn(mockFileHandle);
		when(mockFileHandle.getId()).thenReturn(fileHandleId);
		when(mockMultipartUploadDAO.setUploadComplete(any(), any())).thenReturn(mockCompositeStatus);

		doReturn(mockHandler).when(mockHandlerProvider).getHandlerForType(any());

		FileHandleCreateRequest handleCreateRequest = new FileHandleCreateRequest("fileName", "contentType", "md5",
				123L, true);

		when(mockHandler.getFileHandleCreateRequest(any(), any())).thenReturn(handleCreateRequest);

		CompleteMultipartRequest completeRequest = new CompleteMultipartRequest();

		completeRequest.setUploadId(Long.valueOf(uploadId));
		completeRequest.setNumberOfParts(numberOfParts.longValue());
		completeRequest.setAddedParts(addedParts);
		completeRequest.setBucket(bucket);
		completeRequest.setKey(key);
		completeRequest.setUploadToken(token);

		// Call under test
		MultipartUploadStatus result = manager.completeMultipartUpload(user, uploadId);

		assertEquals(mockStatus, result);

		verify(mockStatus).setPartsState("11");
		verify(mockCloudDao).completeMultipartUpload(completeRequest);
		verify(mockHandler).getFileHandleCreateRequest(mockCompositeStatus, originalRequest);
		verify(mockFileHandleDao).createFile(any());
		verify(mockMultipartUploadDAO).setUploadComplete(uploadId, fileHandleId);
	}

	@Test
	public void testCompleteMultipartUploadNotReady() {
		String uploadId = "1234";
		Integer numberOfParts = 2;

		List<PartMD5> addedParts = Arrays.asList(new PartMD5(2, "partMD5HexTwo"));

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING);

		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);
		when(mockMultipartUploadDAO.getAddedPartMD5s(any())).thenReturn(addedParts);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.completeMultipartUpload(user, uploadId);
		}).getMessage();

		assertEquals("Missing 1 part(s).  All parts must be successfully added before a file upload can be completed.",
				errorMessage);

		verifyNoMoreInteractions(mockMultipartUploadDAO);
		verifyZeroInteractions(mockCloudDao);
		verifyZeroInteractions(mockHandler);
		verifyZeroInteractions(mockFileHandleDao);
	}

	@Test
	public void testCompleteMultipartUploadAlreadyCompleted() {
		String uploadId = "1234";
		Integer numberOfParts = 2;
		String fileHandleId = "12345";

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockStatus.getState()).thenReturn(MultipartUploadState.COMPLETED);
		when(mockStatus.getResultFileHandleId()).thenReturn(fileHandleId);

		when(mockCompositeStatus.getNumberOfParts()).thenReturn(numberOfParts);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		// Call under test
		MultipartUploadStatus result = manager.completeMultipartUpload(user, uploadId);

		assertEquals(mockStatus, result);

		verify(mockStatus).setPartsState("11");

		verifyNoMoreInteractions(mockMultipartUploadDAO);
		verifyZeroInteractions(mockCloudDao);
		verifyZeroInteractions(mockHandler);
		verifyZeroInteractions(mockFileHandleDao);
	}

	@Test
	public void testCompleteMultipartUploadWithUnauthorizedUser() {
		String uploadId = "1234";

		// Started by a different user
		when(mockStatus.getStartedBy()).thenReturn("456788");

		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);

		String errorMessage = assertThrows(UnauthorizedException.class, () -> {
			// Call under test
			manager.completeMultipartUpload(user, uploadId);
		}).getMessage();

		assertEquals(
				"Only the user that started a multipart upload can get part upload pre-signed URLs for that file upload.",
				errorMessage);

		verifyNoMoreInteractions(mockMultipartUploadDAO);
		verifyZeroInteractions(mockCloudDao);
		verifyZeroInteractions(mockHandler);
		verifyZeroInteractions(mockFileHandleDao);
	}

	@Test
	public void testValidatePartNumberPartZero() {
		int partNumber = 0;
		int numberOfParts = 1;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts);
		}).getMessage();

		assertEquals("Part numbers cannot be less than one.", errorMessage);
	}

	@Test
	public void testValidatePartNumberNumberPartsZero() {
		int partNumber = 1;
		int numberOfParts = 0;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts);
		}).getMessage();

		assertEquals("Number of parts cannot be less than one.", errorMessage);
	}

	@Test
	public void testValidatePartNumberTooLarge() {
		int partNumber = 2;
		int numberOfParts = 1;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts);
		}).getMessage();

		assertEquals("Part number cannot be larger than number of parts. Number of parts: 1, provided part number: 2",
				errorMessage);
	}

	@Test
	public void testValidatePartNumberHappy() {
		int partNumber = 1;
		int numberOfParts = 1;
		// Call under test
		MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts);
	}

	@Test
	public void testValidateStartedByHappy() {

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);

		MultipartManagerV2Impl.validateStartedBy(user, mockCompositeStatus);
	}

	@Test
	public void testValidateStartedByUserNull() {

		user = null;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			MultipartManagerV2Impl.validateStartedBy(user, mockCompositeStatus);
		}).getMessage();

		assertEquals("UserInfo is required.", errorMessage);
	}

	@Test
	public void testValidateStartedByCompositeNull() {
		mockCompositeStatus = null;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			MultipartManagerV2Impl.validateStartedBy(user, mockCompositeStatus);
		}).getMessage();

		assertEquals("CompositeMultipartUploadStatus is required.", errorMessage);
	}

	@Test
	public void testValidateStartedByAnotherUser() {

		// started by another user.
		when(mockStatus.getStartedBy()).thenReturn("4566");
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);

		String errorMessage = assertThrows(UnauthorizedException.class, () -> {
			MultipartManagerV2Impl.validateStartedBy(user, mockCompositeStatus);
		}).getMessage();

		assertEquals(
				"Only the user that started a multipart upload can get part upload pre-signed URLs for that file upload.",
				errorMessage);
	}

	@Test
	public void testPrepareCompleteStatusHappy() {

		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(3);
		when(mockStatus.getState()).thenReturn(MultipartUploadState.COMPLETED);
		when(mockStatus.getResultFileHandleId()).thenReturn("12345");

		// Call under test
		MultipartUploadStatus status = MultipartManagerV2Impl.prepareCompleteStatus(mockCompositeStatus);

		assertNotNull(status);
		assertEquals(MultipartUploadState.COMPLETED, status.getState());
		assertEquals("12345", status.getResultFileHandleId());
		verify(mockStatus).setPartsState("111");
	}

	@Test
	public void testPrepareCompleteStatusNotComplete() {
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(3);
		when(mockStatus.getState()).thenReturn(MultipartUploadState.UPLOADING);
		when(mockStatus.getResultFileHandleId()).thenReturn("12345");

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			MultipartManagerV2Impl.prepareCompleteStatus(mockCompositeStatus);
		}).getMessage();

		assertEquals("Expected a COMPLETED state", errorMessage);
	}

	@Test
	public void testPrepareCompleteStatusMissingFileHandle() {
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockStatus.getResultFileHandleId()).thenReturn(null);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			MultipartManagerV2Impl.prepareCompleteStatus(mockCompositeStatus);
		}).getMessage();

		assertEquals("ResultFileHandleId is required.", errorMessage);
	}

	@Test
	public void testPrepareCompleteStatusMissingNumberOfParts() {
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockCompositeStatus.getNumberOfParts()).thenReturn(null);
		when(mockStatus.getResultFileHandleId()).thenReturn("12345");

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			MultipartManagerV2Impl.prepareCompleteStatus(mockCompositeStatus);
		}).getMessage();

		assertEquals("NumberOfParts is required.", errorMessage);
	}

	@Test
	public void testValidatePartsHappy() {
		List<PartMD5> addedParts = Arrays.asList(new PartMD5(2, "partMD5HexTwo"), new PartMD5(1, "partMD5HexOne"));
		int numberOfParts = 2;

		// Call under test
		MultipartManagerV2Impl.validateParts(numberOfParts, addedParts);
	}

	@Test
	public void testValidatePartsMissing() {
		List<PartMD5> addedParts = Arrays.asList(new PartMD5(2, "partMD5HexTwo"), new PartMD5(1, "partMD5HexOne"));
		int numberOfParts = 3;

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			MultipartManagerV2Impl.validateParts(numberOfParts, addedParts);
		}).getMessage();

		assertEquals("Missing 1 part(s).  All parts must be successfully added before a file upload can be completed.",
				errorMessage);
	}

	@Test
	public void testCreateFileHandle() {

		FileHandleCreateRequest request = new FileHandleCreateRequest("fileName", "contentType", "md5", 123L, true);

		String bucket = "bucket";
		String key = "key";
		long fileSize = 123;
		Long fileHandleId = 123456L;

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockCompositeStatus.getBucket()).thenReturn(bucket);
		when(mockCompositeStatus.getKey()).thenReturn(key);
		when(mockCompositeStatus.getUploadType()).thenReturn(UploadType.S3);
		when(mockIdGenerator.generateNewId(any())).thenReturn(fileHandleId);
		when(mockFileHandleDao.createFile(any())).thenReturn(mockFileHandle);

		// Call under test
		CloudProviderFileHandleInterface result = manager.createFileHandle(fileSize, mockCompositeStatus, request);

		assertEquals(mockFileHandle, result);

		ArgumentCaptor<S3FileHandle> capture = ArgumentCaptor.forClass(S3FileHandle.class);

		verify(mockIdGenerator).generateNewId(IdType.FILE_IDS);
		verify(mockFileHandleDao).createFile(capture.capture());

		S3FileHandle capturedFileHandle = capture.getValue();

		assertEquals(fileHandleId.toString(), capturedFileHandle.getId());
		assertEquals(bucket, capturedFileHandle.getBucketName());
		assertEquals(key, capturedFileHandle.getKey());
		assertEquals(request.getContentMD5(), capturedFileHandle.getContentMd5());
		assertEquals(request.getContentType(), capturedFileHandle.getContentType());
		assertEquals(123L, capturedFileHandle.getStorageLocationId());
		assertEquals(fileSize, capturedFileHandle.getContentSize());
		assertEquals(user.getId().toString(), capturedFileHandle.getCreatedBy());
		assertNotNull(capturedFileHandle.getCreatedOn());
		assertNotNull(capturedFileHandle.getEtag());
		assertEquals(request.getFileName(), capturedFileHandle.getFileName());
		assertNull(capturedFileHandle.getPreviewId());
	}

	@Test
	public void testCreateFileHandleNoPreview() {
		FileHandleCreateRequest request = new FileHandleCreateRequest("fileName", "contentType", "md5", 123L, false);

		String bucket = "bucket";
		String key = "key";
		long fileSize = 123;
		Long fileHandleId = 123456L;

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockCompositeStatus.getBucket()).thenReturn(bucket);
		when(mockCompositeStatus.getKey()).thenReturn(key);
		when(mockCompositeStatus.getUploadType()).thenReturn(UploadType.S3);
		when(mockIdGenerator.generateNewId(any())).thenReturn(fileHandleId);
		when(mockFileHandleDao.createFile(any())).thenReturn(mockFileHandle);

		// Call under test
		CloudProviderFileHandleInterface result = manager.createFileHandle(fileSize, mockCompositeStatus, request);

		assertEquals(mockFileHandle, result);

		ArgumentCaptor<S3FileHandle> capture = ArgumentCaptor.forClass(S3FileHandle.class);

		verify(mockFileHandleDao).createFile(capture.capture());

		// preview should not be generated
		assertEquals(capture.getValue().getPreviewId(), fileHandleId.toString());
	}

	@Test
	public void testCreateFileHandleNullPreview() {
		FileHandleCreateRequest request = new FileHandleCreateRequest("fileName", "contentType", "md5", 123L, null);

		String bucket = "bucket";
		String key = "key";
		long fileSize = 123;
		Long fileHandleId = 123456L;

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockCompositeStatus.getBucket()).thenReturn(bucket);
		when(mockCompositeStatus.getKey()).thenReturn(key);
		when(mockCompositeStatus.getUploadType()).thenReturn(UploadType.S3);
		when(mockIdGenerator.generateNewId(any())).thenReturn(fileHandleId);
		when(mockFileHandleDao.createFile(any())).thenReturn(mockFileHandle);

		// Call under test
		CloudProviderFileHandleInterface result = manager.createFileHandle(fileSize, mockCompositeStatus, request);
		assertEquals(mockFileHandle, result);

		ArgumentCaptor<S3FileHandle> capture = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockFileHandleDao).createFile(capture.capture());

		// preview should be generated
		assertNull(capture.getValue().getPreviewId());
	}

	@Test
	public void testCreateFileHandleGoogleCloud() {

		FileHandleCreateRequest request = new FileHandleCreateRequest("fileName", "contentType", "md5", 123L, true);

		String bucket = "bucket";
		String key = "key";
		long fileSize = 123;
		Long fileHandleId = 123456L;

		when(mockStatus.getStartedBy()).thenReturn(user.getId().toString());
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockCompositeStatus.getBucket()).thenReturn(bucket);
		when(mockCompositeStatus.getKey()).thenReturn(key);
		when(mockCompositeStatus.getUploadType()).thenReturn(UploadType.GOOGLECLOUDSTORAGE);
		when(mockIdGenerator.generateNewId(any())).thenReturn(fileHandleId);
		when(mockFileHandleDao.createFile(any())).thenReturn(mockFileHandle);

		// Call under test
		CloudProviderFileHandleInterface result = manager.createFileHandle(fileSize, mockCompositeStatus, request);

		assertEquals(mockFileHandle, result);

		ArgumentCaptor<GoogleCloudFileHandle> capture = ArgumentCaptor.forClass(GoogleCloudFileHandle.class);

		verify(mockIdGenerator).generateNewId(IdType.FILE_IDS);
		verify(mockFileHandleDao).createFile(capture.capture());

		GoogleCloudFileHandle capturedFileHandle = capture.getValue();

		assertEquals(fileHandleId.toString(), capturedFileHandle.getId());
		assertEquals(bucket, capturedFileHandle.getBucketName());
		assertEquals(key, capturedFileHandle.getKey());
		assertEquals(request.getContentMD5(), capturedFileHandle.getContentMd5());
		assertEquals(request.getContentType(), capturedFileHandle.getContentType());
		assertEquals(123L, capturedFileHandle.getStorageLocationId());
		assertEquals(fileSize, capturedFileHandle.getContentSize());
		assertEquals(user.getId().toString(), capturedFileHandle.getCreatedBy());
		assertNotNull(capturedFileHandle.getCreatedOn());
		assertNotNull(capturedFileHandle.getEtag());
		assertEquals(request.getFileName(), capturedFileHandle.getFileName());
		assertNull(capturedFileHandle.getPreviewId());

	}

	@Test
	public void testCreateFileHandleWithUnsupportedType() {

		FileHandleCreateRequest request = new FileHandleCreateRequest("fileName", "contentType", "md5", 123L, true);

		long fileSize = 123;

		when(mockCompositeStatus.getUploadType()).thenReturn(UploadType.HTTPS);

		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// Call under test
			manager.createFileHandle(fileSize, mockCompositeStatus, request);
		}).getMessage();

		assertEquals("Cannot create a FileHandle from a multipart upload with upload type HTTPS", errorMessage);

		verifyZeroInteractions(mockIdGenerator);
		verifyZeroInteractions(mockFileHandleDao);

	}
	
	@Test
	public void testClearMultipartUploadWithCompleted() {
		String uploadId = "uploadId";
		
		MultipartUploadState state  = MultipartUploadState.COMPLETED;
		
		when(mockStatus.getState()).thenReturn(state);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);
		doNothing().when(mockMultipartUploadDAO).deleteUploadStatus(any());
		
		// Call under test
		manager.clearMultipartUpload(uploadId);
		
		verify(mockMultipartUploadDAO).getUploadStatus(uploadId);
		verify(mockHandlerProvider, never()).getHandlerForType(any());
		verifyZeroInteractions(mockHandler);
		verify(mockMultipartUploadDAO).deleteUploadStatus(uploadId);
	}
	
	@Test
	public void testClearMultipartUploadWithUploading() {
		String uploadId = "uploadId";
		
		MultiPartRequestType reqType = MultiPartRequestType.UPLOAD;
		MultipartUploadState state  = MultipartUploadState.UPLOADING;
		
		when(mockStatus.getState()).thenReturn(state);
		when(mockCompositeStatus.getMultipartUploadStatus()).thenReturn(mockStatus);
		when(mockCompositeStatus.getRequestType()).thenReturn(reqType);
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenReturn(mockCompositeStatus);
		doReturn(mockHandler).when(mockHandlerProvider).getHandlerForType(any());
		doNothing().when(mockHandler).tryAbortMultipartRequest(any());
		doNothing().when(mockMultipartUploadDAO).deleteUploadStatus(any());
		
		// Call under test
		manager.clearMultipartUpload(uploadId);
		
		verify(mockMultipartUploadDAO).getUploadStatus(uploadId);
		verify(mockHandlerProvider).getHandlerForType(reqType);
		verify(mockHandler).tryAbortMultipartRequest(mockCompositeStatus);
		verify(mockMultipartUploadDAO).deleteUploadStatus(uploadId);
	}
	
	@Test
	public void testClearMultipartUploadWithNullUploadId() {
		String uploadId = null;
		
		String errorMesssage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.clearMultipartUpload(uploadId);
		}).getMessage();

		assertEquals("The upload id is required.", errorMesssage);

	}
	
	@Test
	public void testClearMultipartUploadWithNonExisting() {
		String uploadId = "uploadId";
		
		when(mockMultipartUploadDAO.getUploadStatus(any())).thenThrow(NotFoundException.class);
		
		// Call under test
		manager.clearMultipartUpload(uploadId);
		
		verify(mockMultipartUploadDAO).getUploadStatus(uploadId);
		verifyZeroInteractions(mockHandlerProvider);
		verifyNoMoreInteractions(mockMultipartUploadDAO);
	}
	
	@Test
	public void testGetUploadsModifedBefore() {
		int numberOfDays = 1;
		long batchSize = 10;
		
		List<String> expected = Arrays.asList("upload1", "upload2");
		
		when(mockMultipartUploadDAO.getUploadsModifiedBefore(anyInt(), anyLong())).thenReturn(expected);
		
		// Call under test
		List<String> result = manager.getUploadsModifiedBefore(numberOfDays, batchSize);
		
		assertEquals(expected, result);
		
		verify(mockMultipartUploadDAO).getUploadsModifiedBefore(numberOfDays, batchSize);
	}
	
	@Test
	public void testGetUploadsModifedBeforeWithNegativeNumberOfDays() {
		int numberOfDays = -1;
		long batchSize = 10;
				
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getUploadsModifiedBefore(numberOfDays, batchSize);
		}).getMessage();
		
		assertEquals("The number of days must be equal or greater than zero.", errorMessage);
		
	}
	
	@Test
	public void testGetUploadsModifedBeforeWithNegativeBatchSize() {
		int numberOfDays = 1;
		long batchSize = -1;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getUploadsModifiedBefore(numberOfDays, batchSize);
		}).getMessage();
		
		assertEquals("The batch size must be greater than zero.", errorMessage);
		
	}
	
	@Test
	public void testGetUploadsModifedBeforeWithZeroBatchSize() {
		int numberOfDays = 1;
		long batchSize = 0;
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			manager.getUploadsModifiedBefore(numberOfDays, batchSize);
		}).getMessage();
		
		assertEquals("The batch size must be greater than zero.", errorMessage);
		
	}
}
