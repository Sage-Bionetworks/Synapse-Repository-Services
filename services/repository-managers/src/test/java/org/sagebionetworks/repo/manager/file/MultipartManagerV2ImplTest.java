package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sagebionetworks.repo.manager.file.MultipartManagerV2Impl.calculateMD5AsHex;
import static org.sagebionetworks.repo.manager.file.MultipartManagerV2Impl.createRequestJSON;

import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.AddPartState;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadState;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartMD5;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.UploadType;
import org.sagebionetworks.upload.multipart.CloudServiceMultipartUploadDAOProvider;
import org.sagebionetworks.upload.multipart.MultipartUploadUtils;
import org.sagebionetworks.upload.multipart.S3MultipartUploadDAOImpl;

import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MultipartManagerV2ImplTest {
	
	@Mock
	UserInfo userInfo;
	@Mock
	MultipartUploadDAO mockMultiparUploadDAO;
	@Mock
	ProjectSettingsManager mockProjectSettingsManager;
	@Mock
	S3MultipartUploadDAOImpl mockS3multipartUploadDAO;
	@Mock
	CloudServiceMultipartUploadDAOProvider mockUploadDAOProvider;
	@Mock
	FileHandleDao mockFileHandleDao;
	@Mock
	IdGenerator mockIdGenerator;

	@InjectMocks
	MultipartManagerV2Impl manager;

	MultipartUploadRequest request;	
	String requestJson;
	String requestHash;

	boolean forceRestart;
	
	String uploadId;
	String uploadToken;
	CompositeMultipartUploadStatus composite;
	CompositeMultipartUploadStatus completeComposite;
	S3FileHandle fileHandle;
	GoogleCloudFileHandle googleCloudFileHandle;
	List<PartMD5> addedParts;
	
	@BeforeEach
	public void before(){
		MockitoAnnotations.initMocks(this);
		when(userInfo.getId()).thenReturn(456L);
		
		request = new MultipartUploadRequest();
		request.setFileName("foo.txt");
		request.setFileSizeBytes((long) (1024*1024*100+15));
		request.setContentMD5Hex("someMD5Hex");
		request.setPartSizeBytes((long) (1024*1024*5));
		request.setStorageLocationId(789L);
		request.setContentType("plain/text");
		
		requestJson = createRequestJSON(request);
		requestHash = calculateMD5AsHex(request);
		
		uploadId = "123456";
		uploadToken = "someUploadToken";
		
		MultipartUploadStatus status = new MultipartUploadStatus();
		status.setUploadId(uploadId);
		status.setStartedBy(""+userInfo.getId());
		composite = new CompositeMultipartUploadStatus();
		composite.setMultipartUploadStatus(status);
		composite.setNumberOfParts(2);
		composite.setBucket("someBucket");
		composite.setKey("someKey");
		composite.setUploadToken(uploadToken);
		composite.setUploadType(UploadType.S3);
		
		fileHandle = new S3FileHandle();
		fileHandle.setId("9999");
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(9999L);
		when(mockFileHandleDao.createFile(any(S3FileHandle.class))).thenReturn(fileHandle);

		googleCloudFileHandle = new GoogleCloudFileHandle();
		googleCloudFileHandle.setId("9999");
		when(mockFileHandleDao.createFile(any(GoogleCloudFileHandle.class))).thenReturn(googleCloudFileHandle);

		// setup a completed upload.
		MultipartUploadStatus completeStatus = new MultipartUploadStatus();
		completeStatus.setUploadId(status.getUploadId());
		completeStatus.setStartedBy(""+userInfo.getId());
		completeStatus.setResultFileHandleId(fileHandle.getId());
		completeStatus.setStartedBy(""+userInfo.getId());
		completeStatus.setState(MultipartUploadState.COMPLETED);
		completeStatus.setStartedOn(new Date());
		completeComposite = new CompositeMultipartUploadStatus();
		completeComposite.setMultipartUploadStatus(completeStatus);
		completeComposite.setBucket(composite.getBucket());
		completeComposite.setKey(composite.getKey());
		completeComposite.setUploadToken(uploadToken);
		completeComposite.setNumberOfParts(composite.getNumberOfParts());
		
		addedParts = Lists.newArrayList(new PartMD5(2, "partMD5HexTwo"), new PartMD5(1, "partMD5HexOne"));
		when(mockMultiparUploadDAO.getAddedPartMD5s(uploadId)).thenReturn(addedParts);
		when(mockMultiparUploadDAO.getUploadRequest(uploadId)).thenReturn(requestJson);
		when(mockMultiparUploadDAO.getUploadStatus(uploadId)).thenReturn(composite);
		when(mockMultiparUploadDAO.setUploadComplete(uploadId, fileHandle.getId())).thenReturn(completeComposite);
		
		// capture all data from a created
		doAnswer(new Answer<CompositeMultipartUploadStatus>(){
			@Override
			public CompositeMultipartUploadStatus answer(
					InvocationOnMock invocation) throws Throwable {
				CreateMultipartRequest cmr = (CreateMultipartRequest) invocation.getArguments()[0];

				MultipartUploadStatus status = new MultipartUploadStatus();
				status.setStartedBy(""+cmr.getUserId());
				status.setStartedOn(new Date());
				status.setState(MultipartUploadState.UPLOADING);
				status.setUploadId("123456");
				status.setUpdatedOn(status.getStartedOn());
				CompositeMultipartUploadStatus composite = new CompositeMultipartUploadStatus();
				composite.setMultipartUploadStatus(status);
				composite.setBucket(cmr.getBucket());
				composite.setKey(cmr.getKey());
				composite.setNumberOfParts(cmr.getNumberOfParts());
				composite.setUploadToken("anUploadToken");
				return composite;
			}}).when(mockMultiparUploadDAO).createUploadStatus(any(CreateMultipartRequest.class));

		when(mockUploadDAOProvider.getCloudServiceMultipartUploadDao(UploadType.S3)).thenReturn(mockS3multipartUploadDAO);
		when(mockS3multipartUploadDAO.initiateMultipartUpload(anyString(), anyString(), any(MultipartUploadRequest.class))).thenReturn(uploadToken);
		
		// Simulate the number of parts
		doAnswer(new Answer<String>(){
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				String uploadId = (String) invocation.getArguments()[0];
				int numberOfParts = (Integer) invocation.getArguments()[1];
				char[] chars = new char[numberOfParts];
				Arrays.fill(chars, '0');
				return new String(chars);
			}}).when(mockMultiparUploadDAO).getPartsState(anyString(), anyInt());
					
		// simulate a presigned url.
		doAnswer(new Answer<URL>() {
			@Override
			public URL answer(InvocationOnMock invocation) throws Throwable {
				String bucket = (String) invocation.getArguments()[0];
				String key = (String) invocation.getArguments()[1];
				return new URL("http", "amazon.com", bucket+"/"+key);
			}
		}).when(mockS3multipartUploadDAO).createPreSignedPutUrl(anyString(), anyString(), anyString());
		
		forceRestart = false;
	}
	
	@Test
	public void testStartOrResumeMultipartUploadAnonymous(){
		// set the user to anonymous
		when(userInfo.getId()).thenReturn(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		//call under test
		assertThrows(UnauthorizedException.class, () -> manager.startOrResumeMultipartUpload(userInfo, request, forceRestart));
	}

	@Test
	public void testStartOrResumeMultipartUpload_EmptyFileName(){
		request.setFileName("");

		//call under test
		assertThrows(IllegalArgumentException.class, () -> manager.startOrResumeMultipartUpload(userInfo, request, forceRestart));
	}

	@Test
	public void testStartOrResumeMultipartUpload_EmptyMD5(){
		request.setContentMD5Hex("");

		//call under test
		assertThrows(IllegalArgumentException.class, () -> manager.startOrResumeMultipartUpload(userInfo, request, forceRestart));
	}

	@Test
	public void testStartOrResumeMultipartUpload_Non_ascii(){
		request.setFileName("文件.txt");

		//call under test
		assertThrows(IllegalArgumentException.class, () -> manager.startOrResumeMultipartUpload(userInfo, request, forceRestart));
	}

	@Test
	public void testCalculateMD5AsHex(){
		// This md5 was generated from the json string of the request.
		String expected = "7c6ab3ed2219a64b4e86726e4eb0fee0";
		//call under test
		String md5Hex = MultipartManagerV2Impl.calculateMD5AsHex(request);
		assertEquals(expected, md5Hex);
	}
	
	@Test
	public void testStartOrResumeMultipartUploadNotStarted(){
		// setup the case where the status does not exist
		when(mockMultiparUploadDAO.getUploadStatus(anyLong(), anyString())).thenReturn(null);
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		assertEquals(MultipartUploadState.UPLOADING, status.getState());
		assertEquals("000000000000000000000", status.getPartsState());
		assertEquals("123456", status.getUploadId());

		// the status should not be reset.
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
		
		// since the upload does not exist it should get created.
		ArgumentCaptor<CreateMultipartRequest> requestCapture = ArgumentCaptor.forClass(CreateMultipartRequest.class);
		verify(mockMultiparUploadDAO).createUploadStatus(requestCapture.capture());
		assertEquals(uploadToken, requestCapture.getValue().getUploadToken());
		assertEquals(StackConfigurationSingleton.singleton().getS3Bucket(), requestCapture.getValue().getBucket());
		assertNotNull(requestCapture.getValue().getKey());
		assertEquals(requestJson, requestCapture.getValue().getRequestBody());
		assertEquals(userInfo.getId(), requestCapture.getValue().getUserId());
		assertEquals(requestHash, requestCapture.getValue().getHash());
	}
	
	@Test
	public void testStartOrResumeMultipartUploadAlreadyStarted(){
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(anyLong(), anyString())).thenReturn(composite);
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertEquals("00", status.getPartsState());
		assertNotNull(status);
		// the status should not be reset.
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
		verify(mockMultiparUploadDAO, never()).createUploadStatus(any(CreateMultipartRequest.class));
		verify(mockMultiparUploadDAO).getPartsState(anyString(), anyInt());
	}
	
	@Test
	public void testStartOrResumeMultipartUploadAlreadyStartedCompleted(){
		// setup the case where the status already exists and it is complete
		composite.getMultipartUploadStatus().setState(MultipartUploadState.COMPLETED);
		composite.getMultipartUploadStatus().setResultFileHandleId("9876");
		when(mockMultiparUploadDAO.getUploadStatus(anyLong(), anyString())).thenReturn(composite);
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		assertEquals(MultipartUploadState.COMPLETED, status.getState());
		assertEquals("11", status.getPartsState());
		assertEquals("9876", status.getResultFileHandleId());

		// the status should not be reset.
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
		verify(mockMultiparUploadDAO, never()).createUploadStatus(any(CreateMultipartRequest.class));
		// When the file is completed, the database should not be used to get the partsState.
		verify(mockMultiparUploadDAO, never()).getPartsState(anyString(), anyInt());
	}
	
	@Test
	public void testStartOrResumeMultipartUploadForceRestartFalse(){
		forceRestart = false;
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
	}
	
	@Test
	public void testStartOrResumeMultipartUploadForceRestartTrue(){
		forceRestart = true;
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		verify(mockMultiparUploadDAO).deleteUploadStatus(anyLong(), anyString());
	}
	
	@Test
	public void testGetCompletePartStateString(){
		assertEquals("1111", MultipartManagerV2Impl.getCompletePartStateString(4));
	}
	
	@Test
	public void testGetBatchPresignedUploadUrlsUnauthorized(){
//		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// set this upload started by another user.
		composite.getMultipartUploadStatus().setStartedBy(""+(userInfo.getId()+1));
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uploadId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uploadId);
		request.setPartNumbers(Lists.newArrayList(1L, 2L));
		// call under test
		assertThrows(UnauthorizedException.class, () -> manager.getBatchPresignedUploadUrls(userInfo, request));
	}
	
	@Test
	public void testGetBatchPresignedUploadUrls(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(Lists.newArrayList(1L, 2L));
		request.setContentType("plain/text");
		// call under test
		BatchPresignedUploadUrlResponse response = manager.getBatchPresignedUploadUrls(userInfo, request);
		assertNotNull(response);
		assertNotNull(response.getPartPresignedUrls());
		assertEquals(2, response.getPartPresignedUrls().size());
		PartPresignedUrl partUrl = response.getPartPresignedUrls().get(0);
		assertEquals(new Long(1), partUrl.getPartNumber());
		assertEquals("http://amazon.comsomeBucket/someKey/1", partUrl.getUploadPresignedUrl());
		partUrl = response.getPartPresignedUrls().get(1);
		assertEquals(new Long(2), partUrl.getPartNumber());
		assertEquals("http://amazon.comsomeBucket/someKey/2", partUrl.getUploadPresignedUrl());
	}
	
	@Test
	public void testGetBatchPresignedUploadUrlsNull(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(null);
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getBatchPresignedUploadUrls(userInfo, request));
	}
	
	@Test
	public void testGetBatchPresignedUploadUrlsEmptyl(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(new LinkedList<Long>());
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getBatchPresignedUploadUrls(userInfo, request));
	}
	
	@Test
	public void testGetBatchPresignedUploadUrlsZeroPart(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(Lists.newArrayList(0L));
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getBatchPresignedUploadUrls(userInfo, request));
	}
	
	@Test
	public void testGetBatchPresignedUploadUrlsPartTooBig(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(Lists.newArrayList(new Long(composite.getNumberOfParts()+1)));
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.getBatchPresignedUploadUrls(userInfo, request));
	}
	
	@Test
	public void testValidatePartNumberPartZero(){
		int partNumber = 0;
		int numberOfParts = 1;
		//call under test.
		assertThrows(IllegalArgumentException.class, () -> MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts));
	}
	
	@Test
	public void testValidatePartNumberNumberPartsZero(){
		int partNumber = 1;
		int numberOfParts = 0;
		//call under test.
		assertThrows(IllegalArgumentException.class, () -> MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts));
	}

	@Test
	public void testValidatePartNumberTooLarge(){
		int partNumber = 2;
		int numberOfParts = 1;
		//call under test.
		assertThrows(IllegalArgumentException.class, () -> MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts));
	}

	@Test
	public void testValidatePartNumberHappy(){
		int partNumber = 1;
		int numberOfParts = 1;
		//call under test.
		MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts);
	}

	@Test
	public void testValidateStartedByHappy(){
		MultipartManagerV2Impl.validateStartedBy(userInfo, composite);
	}

	@Test
	public void testValidateStartedByUserNull(){
		userInfo = null;
		assertThrows(IllegalArgumentException.class, () -> MultipartManagerV2Impl.validateStartedBy(userInfo, composite));
	}

	@Test
	public void testValidateStartedByCompositeNull(){
		composite = null;
		assertThrows(IllegalArgumentException.class, () -> MultipartManagerV2Impl.validateStartedBy(userInfo, composite));
	}

	@Test
	public void testValidateStartedByAnotherUser(){
		// started by another user.
		composite.getMultipartUploadStatus().setStartedBy(""+userInfo.getId()+1);;
		assertThrows(UnauthorizedException.class, () -> MultipartManagerV2Impl.validateStartedBy(userInfo, composite));
	}

	@Test
	public void testAddMultipartPartUnauthorizedException(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		// set the startedBy to be another user.
		composite.getMultipartUploadStatus().setStartedBy(""+userInfo.getId()+1);
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		assertThrows(UnauthorizedException.class, () -> manager.addMultipartPart(userInfo, uplaodId, 1, partMD5Hex));
	}

	@Test
	public void testAddMultipartPartBadPartNumber(){
		int partNumber = composite.getNumberOfParts()+1;
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";

		assertThrows(IllegalArgumentException.class, () -> manager.addMultipartPart(userInfo, uplaodId, partNumber, partMD5Hex));
	}

	@Test
	public void testAddMultipartPartPartNumberLessThanOne(){
		int partNumber = 0;
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";

		assertThrows(IllegalArgumentException.class, () -> manager.addMultipartPart(userInfo, uplaodId, partNumber, partMD5Hex));
	}

	@Test
	public void testAddMultipartPartHappy(){
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		int partNumber = 2;
		String partKey = MultipartUploadUtils.createPartKey(composite.getKey(), partNumber);

		AddPartResponse response = manager.addMultipartPart(userInfo, uploadId, partNumber, partMD5Hex);
		assertNotNull(response);
		assertEquals(uploadId, response.getUploadId());
		assertNotNull(response.getPartNumber());
		assertEquals(partNumber, response.getPartNumber().intValue());
		assertEquals(AddPartState.ADD_SUCCESS, response.getAddPartState());

		ArgumentCaptor<AddPartRequest> capture = ArgumentCaptor.forClass(AddPartRequest.class);
		verify(mockS3multipartUploadDAO).validateAndAddPart(capture.capture());
		assertEquals(composite.getBucket(), capture.getValue().getBucket());
		assertEquals(composite.getKey(), capture.getValue().getKey());
		assertEquals(partKey, capture.getValue().getPartKey());
		assertEquals(partMD5Hex, capture.getValue().getPartMD5Hex());
		assertEquals(composite.getUploadToken(), capture.getValue().getUploadToken());

		// the part state should be saved
		verify(mockMultiparUploadDAO).addPartToUpload(uploadId, partNumber, partMD5Hex);
	}

	@Test
	public void testAddMultipartPartComplete(){
		// setup a complete state
		composite.getMultipartUploadStatus().setState(MultipartUploadState.COMPLETED);
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		int partNumber = 2;
		// call under test
		assertThrows(IllegalArgumentException.class, () -> manager.addMultipartPart(userInfo, uploadId, partNumber, partMD5Hex));
	}
	
	@Test
	public void testAddMultipartPartFailed(){
		String uploadId = composite.getMultipartUploadStatus().getUploadId();
		
		//setup an error on add.
		Exception error = new RuntimeException("Something went wrong");
		doThrow(error).when(mockS3multipartUploadDAO).validateAndAddPart(any(AddPartRequest.class));
		
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uploadId)).thenReturn(composite);
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		int partNumber = 2;
		String partKey = MultipartUploadUtils.createPartKey(composite.getKey(), partNumber);
		
		AddPartResponse response = manager.addMultipartPart(userInfo, uploadId, partNumber, partMD5Hex);
		assertNotNull(response);
		assertEquals(uploadId, response.getUploadId());
		assertNotNull(response.getPartNumber());
		assertEquals(partNumber, response.getPartNumber().intValue());
		assertEquals(AddPartState.ADD_FAILED, response.getAddPartState());
		assertEquals(error.getMessage(), response.getErrorMessage());
		
		ArgumentCaptor<AddPartRequest> capture = ArgumentCaptor.forClass(AddPartRequest.class);
		verify(mockS3multipartUploadDAO).validateAndAddPart(capture.capture());
		assertEquals(composite.getBucket(), capture.getValue().getBucket());
		assertEquals(composite.getKey(), capture.getValue().getKey());
		assertEquals(partKey, capture.getValue().getPartKey());
		assertEquals(partMD5Hex, capture.getValue().getPartMD5Hex());
		assertEquals(composite.getUploadToken(), capture.getValue().getUploadToken());
		
		// the part state should be saved
		verify(mockMultiparUploadDAO).setPartToFailed(eq(uploadId), eq(partNumber), startsWith("java.lang.RuntimeException: Something went wrong"));
	}
	
	@Test
	public void testValidatePartsHappy(){
		List<PartMD5> addedParts = Lists.newArrayList(new PartMD5(2, "partMD5HexTwo"), new PartMD5(1, "partMD5HexOne"));
		int numberOfParts = 2;
		MultipartManagerV2Impl.validateParts(numberOfParts, addedParts);
	}
	
	@Test
	public void testValidatePartsMissing(){
		List<PartMD5> addedParts = Lists.newArrayList(new PartMD5(2, "partMD5HexTwo"), new PartMD5(1, "partMD5HexOne"));
		int numberOfParts = 3;
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> MultipartManagerV2Impl.validateParts(numberOfParts, addedParts));
		assertTrue(e.getMessage().contains("Missing 1 part(s)"));
	}
	
	@Test
	public void testPrepareCompleteStatusHappy(){
		composite.getMultipartUploadStatus().setState(MultipartUploadState.COMPLETED);
		composite.getMultipartUploadStatus().setResultFileHandleId("12345");
		composite.setNumberOfParts(3);
		//call under test
		MultipartUploadStatus status = MultipartManagerV2Impl.prepareCompleteStatus(composite);
		assertNotNull(status);
		assertEquals(MultipartUploadState.COMPLETED, status.getState());
		assertEquals("12345", status.getResultFileHandleId());
		assertEquals("111", status.getPartsState());
	}
	
	@Test
	public void testPrepareCompleteStatusNotComplete(){
		composite.getMultipartUploadStatus().setState(MultipartUploadState.UPLOADING);
		composite.getMultipartUploadStatus().setResultFileHandleId("12345");
		composite.setNumberOfParts(3);
		//call under test.
		assertThrows(IllegalArgumentException.class, () -> MultipartManagerV2Impl.prepareCompleteStatus(composite));
	}
	
	@Test
	public void testPrepareCompleteStatusMissingFileHandle(){
		composite.getMultipartUploadStatus().setState(MultipartUploadState.COMPLETED);
		composite.getMultipartUploadStatus().setResultFileHandleId(null);
		composite.setNumberOfParts(3);
		//call under test.
		assertThrows(IllegalArgumentException.class, () -> MultipartManagerV2Impl.prepareCompleteStatus(composite));
	}
	
	@Test
	public void testPrepareCompleteStatusMissingNumberOfParts(){
		composite.getMultipartUploadStatus().setState(MultipartUploadState.COMPLETED);
		composite.getMultipartUploadStatus().setResultFileHandleId("12345");
		composite.setNumberOfParts(null);
		//call under test.
		assertThrows(IllegalArgumentException.class, () -> MultipartManagerV2Impl.prepareCompleteStatus(composite));
	}
	
	@Test
	public void testGetRequestForUpload(){
		String uploadId = composite.getMultipartUploadStatus().getUploadId();
		when(mockMultiparUploadDAO.getUploadRequest(uploadId)).thenReturn(requestJson);
		// call under test
		MultipartUploadRequest returnedRequest = manager.getRequestForUpload(uploadId);
		assertEquals(request, returnedRequest);
	}
	
	@Test
	public void testCreateFileHandle(){

		long fileSize = 123;
		// call under test
		S3FileHandle result = (S3FileHandle) manager.createFileHandle(fileSize, composite, request);
		assertEquals(fileHandle, result);
		
		ArgumentCaptor<S3FileHandle> capture = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockFileHandleDao).createFile(capture.capture());
		
		S3FileHandle capturedFileHandle = capture.getValue();
		assertEquals(composite.getBucket(), capturedFileHandle.getBucketName());
		assertEquals(composite.getKey(), capturedFileHandle.getKey());
		assertEquals(request.getContentMD5Hex(), capturedFileHandle.getContentMd5());
		assertEquals(request.getContentType(), capturedFileHandle.getContentType());
		assertEquals(new Long(fileSize), capturedFileHandle.getContentSize());
		assertEquals(""+userInfo.getId(), capturedFileHandle.getCreatedBy());
		assertNotNull(capturedFileHandle.getCreatedOn());
		assertNotNull(capturedFileHandle.getEtag());
		assertEquals(request.getFileName(), capturedFileHandle.getFileName());
		assertNull(capturedFileHandle.getPreviewId());
	}
	
	@Test
	public void testcreateFileHandleNoPreview(){
		long fileSize = 123;
		request.setGeneratePreview(false);
		// call under test
		S3FileHandle result = (S3FileHandle) manager.createFileHandle(fileSize, composite, request);
		assertEquals(fileHandle, result);
		
		ArgumentCaptor<S3FileHandle> capture = ArgumentCaptor.forClass(S3FileHandle.class);
		verify(mockFileHandleDao).createFile(capture.capture());
		
		// preview should not be generated
		assertEquals(capture.getValue().getPreviewId(), result.getId());
	}
	@Test
	public void testCreateFileHandleGoogleCloud(){

		long fileSize = 123;
		composite.setUploadType(UploadType.GOOGLECLOUDSTORAGE);
		// call under test
		GoogleCloudFileHandle result = (GoogleCloudFileHandle) manager.createFileHandle(fileSize, composite, request);
		assertEquals(googleCloudFileHandle, result);

		ArgumentCaptor<GoogleCloudFileHandle> capture = ArgumentCaptor.forClass(GoogleCloudFileHandle.class);
		verify(mockFileHandleDao).createFile(capture.capture());

		GoogleCloudFileHandle capturedFileHandle = capture.getValue();
		assertEquals(composite.getBucket(), capturedFileHandle.getBucketName());
		assertEquals(composite.getKey(), capturedFileHandle.getKey());
		assertEquals(request.getContentMD5Hex(), capturedFileHandle.getContentMd5());
		assertEquals(request.getContentType(), capturedFileHandle.getContentType());
		assertEquals(new Long(fileSize), capturedFileHandle.getContentSize());
		assertEquals(""+userInfo.getId(), capturedFileHandle.getCreatedBy());
		assertNotNull(capturedFileHandle.getCreatedOn());
		assertNotNull(capturedFileHandle.getEtag());
		assertEquals(request.getFileName(), capturedFileHandle.getFileName());
		assertNull(capturedFileHandle.getPreviewId());
	}
	
	@Test
	public void testCompleteMultipartUploadHappy(){		
		//call under test
		MultipartUploadStatus status = manager.completeMultipartUpload(userInfo, uploadId);
		assertNotNull(status);
		assertEquals(MultipartUploadState.COMPLETED, status.getState());
		assertEquals("11", status.getPartsState());
		assertEquals(fileHandle.getId(), status.getResultFileHandleId());
		
		verify(mockMultiparUploadDAO).getUploadStatus(uploadId);
		verify(mockMultiparUploadDAO).getAddedPartMD5s(uploadId);
		verify(mockFileHandleDao).createFile(any(S3FileHandle.class));
		
		ArgumentCaptor<CompleteMultipartRequest> completeCpature = ArgumentCaptor.forClass(CompleteMultipartRequest.class);
		verify(mockS3multipartUploadDAO).completeMultipartUpload(completeCpature.capture());
		assertEquals(composite.getBucket(), completeCpature.getValue().getBucket());
		assertEquals(composite.getKey(), completeCpature.getValue().getKey());
		assertEquals(uploadToken, completeCpature.getValue().getUploadToken());
		assertEquals(addedParts, completeCpature.getValue().getAddedParts());
		
		verify(mockMultiparUploadDAO).setUploadComplete(uploadId, fileHandle.getId());
	}
	
	@Test
	public void testCompleteMultipartUploadUnAuthorized(){
		// started by another.
		composite.getMultipartUploadStatus().setStartedBy(""+(userInfo.getId()+1));
	
		//call under test
		assertThrows(UnauthorizedException.class, () -> manager.completeMultipartUpload(userInfo, uploadId));
	}
	
	@Test
	public void testCompleteMultipartUploadAlreadyComplete(){
		// the upload is already complete.s
		composite.getMultipartUploadStatus().setState(MultipartUploadState.COMPLETED);
		composite.getMultipartUploadStatus().setResultFileHandleId(fileHandle.getId());
		//call under test
		MultipartUploadStatus status = manager.completeMultipartUpload(userInfo, uploadId);
		assertNotNull(status);
		assertEquals(MultipartUploadState.COMPLETED, status.getState());
		assertEquals("11", status.getPartsState());
		assertEquals(fileHandle.getId(), status.getResultFileHandleId());
		
		verify(mockMultiparUploadDAO).getUploadStatus(uploadId);
		verify(mockMultiparUploadDAO, never()).getAddedPartMD5s(uploadId);
		verify(mockFileHandleDao, never()).createFile(any(S3FileHandle.class));
		verify(mockS3multipartUploadDAO, never()).completeMultipartUpload(any(CompleteMultipartRequest.class));
		verify(mockMultiparUploadDAO, never()).setUploadComplete(uploadId, fileHandle.getId());		
	}
	
	@Test
	public void testCompleteMultipartUploadNotReady(){
		// the upload is missing a file.
		addedParts.remove(1);
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> manager.completeMultipartUpload(userInfo, uploadId));
		assertTrue(e.getMessage().contains("Missing 1 part(s"));
		verify(mockMultiparUploadDAO).getUploadStatus(uploadId);
		verify(mockMultiparUploadDAO).getAddedPartMD5s(uploadId);
		verify(mockFileHandleDao, never()).createFile(any(S3FileHandle.class));
		verify(mockS3multipartUploadDAO, never()).completeMultipartUpload(any(CompleteMultipartRequest.class));
		verify(mockMultiparUploadDAO, never()).setUploadComplete(uploadId, fileHandle.getId());		
	}
}
