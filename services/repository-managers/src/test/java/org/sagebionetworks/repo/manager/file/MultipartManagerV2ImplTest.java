package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.sagebionetworks.repo.manager.file.MultipartManagerV2Impl.*;

import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.ProjectSettingsManager;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.dbo.file.CreateMultipartRequest;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.file.AddPartResponse;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlRequest;
import org.sagebionetworks.repo.model.file.BatchPresignedUploadUrlResponse;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadStatus;
import org.sagebionetworks.repo.model.file.PartPresignedUrl;
import org.sagebionetworks.repo.model.file.Multipart.AddPartState;
import org.sagebionetworks.repo.model.file.Multipart.State;
import org.sagebionetworks.upload.multipart.AddPartRequest;
import org.sagebionetworks.upload.multipart.S3MultipartUploadDAO;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.common.collect.Lists;

public class MultipartManagerV2ImplTest {
	
	@Mock
	UserInfo userInfo;
	@Mock
	MultipartUploadDAO mockMultiparUploadDAO;
	@Mock
	ProjectSettingsManager mockProjectSettingsManager;
	@Mock
	S3MultipartUploadDAO mockS3multipartUploadDAO;

	MultipartUploadRequest request;	
	String requestJson;
	String requestHash;
	MultipartManagerV2Impl manager;
	Boolean forceRestart;
	
	String uploadToken;
	CompositeMultipartUploadStatus composite;
	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		when(userInfo.getId()).thenReturn(456L);
		
		request = new MultipartUploadRequest();
		request.setFileName("foo.txt");
		request.setFileSizeBytes((long) (1024*1024*100+15));
		request.setContentMD5Hex("someMD5Hex");
		request.setPartSizeBytes((long) (1024*1024*5));
		request.setStorageLocationId(789L);
		
		requestJson = createRequestJSON(request);
		requestHash = calculateMD5AsHex(request);
		
		MultipartUploadStatus status = new MultipartUploadStatus();
		status.setUploadId("123456");
		status.setStartedBy(""+userInfo.getId());
		composite = new CompositeMultipartUploadStatus();
		composite.setMultipartUploadStatus(status);
		composite.setNumberOfParts(10);
		composite.setBucket("someBucket");
		composite.setKey("someKey");
		
		uploadToken = "someUploadToken";
		
		// capture all data from a created
		doAnswer(new Answer<CompositeMultipartUploadStatus>(){
			@Override
			public CompositeMultipartUploadStatus answer(
					InvocationOnMock invocation) throws Throwable {
				CreateMultipartRequest cmr = (CreateMultipartRequest) invocation.getArguments()[0];

				MultipartUploadStatus status = new MultipartUploadStatus();
				status.setStartedBy(""+cmr.getUserId());
				status.setStartedOn(new Date());
				status.setState(State.UPLOADING);
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
		}).when(mockS3multipartUploadDAO).createPreSignedPutUrl(anyString(), anyString());
		//when(mockS3multipartUploadDAO.createPreSignedPutUrl(anyString(), partKey)).
		
		forceRestart = null;
		manager = new MultipartManagerV2Impl();
		ReflectionTestUtils.setField(manager, "multipartUploadDAO", mockMultiparUploadDAO);
		ReflectionTestUtils.setField(manager, "projectSettingsManager", mockProjectSettingsManager);
		ReflectionTestUtils.setField(manager, "s3multipartUploadDAO", mockS3multipartUploadDAO);
		
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testStartOrResumeMultipartUploadAnonymous(){
		// set the user to anonymous
		when(userInfo.getId()).thenReturn(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
		//call under test
		manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
	}
	

	@Test
	public void testCalculateMD5AsHex(){
		// This md5 was generated from the json string of the request.
		String expected = "8356accbaa8bfc6ddc6c612224c6c9b3";
		//call under test
		String md5Hex = MultipartManagerV2Impl.calculateMD5AsHex(request);
		assertEquals(expected, md5Hex);
	}
	
	@Test
	public void testCalculateNumberOfPartsSmall(){
		long fileSize = 1;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(1, numberOfParts);
	}

	@Test
	public void testCalculateNumberOfPartsNoRemainder(){
		long fileSize = MIN_PART_SIZE_BYTES*2;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(2, numberOfParts);
	}
	
	@Test
	public void testCalculateNumberOfPartsWithRemainder(){
		long fileSize = MIN_PART_SIZE_BYTES*2+1;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(3, numberOfParts);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateNumberOfLessThanOne(){
		long fileSize = 0;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testCalculateNumberOfPartTooSmall(){
		long fileSize = 1;
		long partSize = MIN_PART_SIZE_BYTES-1;
		//call under test
		MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
	}
	
	@Test
	public void testCalculateNumberOfPartAtMax(){
		long fileSize = MIN_PART_SIZE_BYTES*MAX_NUMBER_OF_PARTS;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(MAX_NUMBER_OF_PARTS, numberOfParts);
	}
	
	@Test
	public void testCalculateNumberOfPartOverMax(){
		long fileSize = MIN_PART_SIZE_BYTES*MAX_NUMBER_OF_PARTS+1;
		long partSize = MIN_PART_SIZE_BYTES;
		//call under test
		try {
			MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
			fail("Should have thrown an exception");
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("10001"));
		}
	}
	
	@Test
	public void testStartOrResumeMultipartUploadNotStarted(){
		// setup the case where the status does not exist
		when(mockMultiparUploadDAO.getUploadStatus(anyLong(), anyString())).thenReturn(null);
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		assertEquals(State.UPLOADING, status.getState());
		assertEquals("000000000000000000000", status.getPartsState());
		assertEquals("123456", status.getUploadId());
		
		// the status should not be reset.
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
		
		// since the upload does not exist it should get created.
		ArgumentCaptor<CreateMultipartRequest> requestCapture = ArgumentCaptor.forClass(CreateMultipartRequest.class);
		verify(mockMultiparUploadDAO).createUploadStatus(requestCapture.capture());
		assertEquals(uploadToken, requestCapture.getValue().getUploadToken());
		assertEquals(StackConfiguration.getS3Bucket(), requestCapture.getValue().getBucket());
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
		assertEquals("0000000000", status.getPartsState());
		assertNotNull(status);
		// the status should not be reset.
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
		verify(mockMultiparUploadDAO, never()).createUploadStatus(any(CreateMultipartRequest.class));
		verify(mockMultiparUploadDAO).getPartsState(anyString(), anyInt());
	}
	
	@Test
	public void testStartOrResumeMultipartUploadAlreadyStartedCompleted(){
		// setup the case where the status already exists and it is complete
		composite.getMultipartUploadStatus().setState(State.COMPLETED);
		composite.getMultipartUploadStatus().setResultFileHandleId("9876");
		when(mockMultiparUploadDAO.getUploadStatus(anyLong(), anyString())).thenReturn(composite);
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		assertEquals(State.COMPLETED, status.getState());
		assertEquals("1111111111", status.getPartsState());
		assertEquals("9876", status.getResultFileHandleId());

		// the status should not be reset.
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
		verify(mockMultiparUploadDAO, never()).createUploadStatus(any(CreateMultipartRequest.class));
		// When the file is completed, the database should not be used to get the partsState.
		verify(mockMultiparUploadDAO, never()).getPartsState(anyString(), anyInt());
	}
	
	@Test
	public void testStartOrResumeMultipartUploadForceRestartFalse(){
		forceRestart = Boolean.FALSE;
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		verify(mockMultiparUploadDAO, never()).deleteUploadStatus(anyLong(), anyString());
	}
	
	@Test
	public void testStartOrResumeMultipartUploadForceRestartTrue(){
		forceRestart = Boolean.TRUE;
		// call under test
		MultipartUploadStatus status = manager.startOrResumeMultipartUpload(userInfo, request, forceRestart);
		assertNotNull(status);
		verify(mockMultiparUploadDAO).deleteUploadStatus(anyLong(), anyString());
	}
	
	@Test
	public void testGetCompletePartStateString(){
		assertEquals("1111", MultipartManagerV2Impl.getCompletePartStateString(4));
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testGetBatchPresignedUploadUrlsUnauthorized(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// set this upload started by another user.
		composite.getMultipartUploadStatus().setStartedBy(""+(userInfo.getId()+1));
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(Lists.newArrayList(1L, 2L));
		// call under test
		manager.getBatchPresignedUploadUrls(userInfo, request);
	}
	
	@Test
	public void testGetBatchPresignedUploadUrls(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(Lists.newArrayList(1L, 2L));
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
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetBatchPresignedUploadUrlsNull(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(null);
		// call under test
		manager.getBatchPresignedUploadUrls(userInfo, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetBatchPresignedUploadUrlsEmptyl(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(new LinkedList<Long>());
		// call under test
		manager.getBatchPresignedUploadUrls(userInfo, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetBatchPresignedUploadUrlsZeroPart(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(Lists.newArrayList(0L));
		// call under test
		manager.getBatchPresignedUploadUrls(userInfo, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetBatchPresignedUploadUrlsPartTooBig(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		
		BatchPresignedUploadUrlRequest request = new BatchPresignedUploadUrlRequest();
		request.setUploadId(uplaodId);
		request.setPartNumbers(Lists.newArrayList(new Long(composite.getNumberOfParts()+1)));
		// call under test
		manager.getBatchPresignedUploadUrls(userInfo, request);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePartNumberPartZero(){
		int partNumber = 0;
		int numberOfParts = 1;
		//call under test.
		MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePartNumberNumberPartsZero(){
		int partNumber = 1;
		int numberOfParts = 0;
		//call under test.
		MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidatePartNumberTooLarge(){
		int partNumber = 2;
		int numberOfParts = 1;
		//call under test.
		MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts);
	}
	
	@Test
	public void testValidatePartNumberHappy(){
		int partNumber = 1;
		int numberOfParts = 1;
		//call under test.
		MultipartManagerV2Impl.validatePartNumber(partNumber, numberOfParts);
	}
	
	@Test
	public void testCreatePartKey(){
		assertEquals("baseKey/9999", MultipartManagerV2Impl.createPartKey("baseKey", 9999));
	}
	
	@Test
	public void testValidateStartedByHappy(){
		MultipartManagerV2Impl.validateStartedBy(userInfo, composite);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateStartedByUserNull(){
		userInfo = null;
		MultipartManagerV2Impl.validateStartedBy(userInfo, composite);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testValidateStartedByCompositeNull(){
		composite = null;
		MultipartManagerV2Impl.validateStartedBy(userInfo, composite);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testValidateStartedByAnotherUser(){
		// started by another user.
		composite.getMultipartUploadStatus().setStartedBy(""+userInfo.getId()+1);;
		MultipartManagerV2Impl.validateStartedBy(userInfo, composite);
	}
	
	@Test (expected=UnauthorizedException.class)
	public void testAddMultipartPartUnauthorizedException(){
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		// set the startedBy to be another user.
		composite.getMultipartUploadStatus().setStartedBy(""+userInfo.getId()+1);
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		manager.addMultipartPart(userInfo, uplaodId, 1, partMD5Hex);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testAddMultipartPartBadPartNumber(){
		int partNumber = composite.getNumberOfParts()+1;
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		
		manager.addMultipartPart(userInfo, uplaodId, partNumber, partMD5Hex);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testAddMultipartPartPartNumberLessThanOne(){
		int partNumber = 0;
		String uplaodId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uplaodId)).thenReturn(composite);
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		
		manager.addMultipartPart(userInfo, uplaodId, partNumber, partMD5Hex);
	}
	
	@Test
	public void testAddMultipartPartHappy(){
		String uploadId = composite.getMultipartUploadStatus().getUploadId();
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uploadId)).thenReturn(composite);
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		int partNumber = 2;
		String partKey = createPartKey(composite.getKey(), partNumber);
		
		AddPartResponse response = manager.addMultipartPart(userInfo, uploadId, partNumber, partMD5Hex);
		assertNotNull(response);
		assertEquals(uploadId, response.getUploadId());
		assertNotNull(response.getPartNumber());
		assertEquals(partNumber, response.getPartNumber().intValue());
		assertEquals(AddPartState.ADD_SUCCESS, response.getAddPartState());
		
		ArgumentCaptor<AddPartRequest> capture = ArgumentCaptor.forClass(AddPartRequest.class);
		verify(mockS3multipartUploadDAO).addPart(capture.capture());
		assertEquals(composite.getBucket(), capture.getValue().getBucket());
		assertEquals(composite.getKey(), capture.getValue().getKey());
		assertEquals(partKey, capture.getValue().getPartKey());
		assertEquals(partMD5Hex, capture.getValue().getPartMD5Hex());
		assertEquals(composite.getUploadToken(), capture.getValue().getUploadToken());
		
		// the part state should be saved
		verify(mockMultiparUploadDAO).addPartToUpload(uploadId, partNumber, partMD5Hex);
		
		// the part should get deleted
		verify(mockS3multipartUploadDAO).deleteObject(composite.getBucket(), partKey);
	}
	
	@Test
	public void testAddMultipartPartFailed(){
		String uploadId = composite.getMultipartUploadStatus().getUploadId();
		
		//setup an error on add.
		Exception error = new RuntimeException("Something went wrong");
		doThrow(error).when(mockS3multipartUploadDAO).addPart(any(AddPartRequest.class));
		
		// setup the case where the status already exists
		when(mockMultiparUploadDAO.getUploadStatus(uploadId)).thenReturn(composite);
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		int partNumber = 2;
		String partKey = createPartKey(composite.getKey(), partNumber);
		
		AddPartResponse response = manager.addMultipartPart(userInfo, uploadId, partNumber, partMD5Hex);
		assertNotNull(response);
		assertEquals(uploadId, response.getUploadId());
		assertNotNull(response.getPartNumber());
		assertEquals(partNumber, response.getPartNumber().intValue());
		assertEquals(AddPartState.ADD_FAILED, response.getAddPartState());
		assertEquals(error.getMessage(), response.getErrorMessage());
		
		ArgumentCaptor<AddPartRequest> capture = ArgumentCaptor.forClass(AddPartRequest.class);
		verify(mockS3multipartUploadDAO).addPart(capture.capture());
		assertEquals(composite.getBucket(), capture.getValue().getBucket());
		assertEquals(composite.getKey(), capture.getValue().getKey());
		assertEquals(partKey, capture.getValue().getPartKey());
		assertEquals(partMD5Hex, capture.getValue().getPartMD5Hex());
		assertEquals(composite.getUploadToken(), capture.getValue().getUploadToken());
		
		// the part state should be saved
		verify(mockMultiparUploadDAO).setPartToFailed(eq(uploadId), eq(partNumber), startsWith("java.lang.RuntimeException: Something went wrong"));
		
	}
}
