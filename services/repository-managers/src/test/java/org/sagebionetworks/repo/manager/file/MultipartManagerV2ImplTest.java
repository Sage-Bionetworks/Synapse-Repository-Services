package org.sagebionetworks.repo.manager.file;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dbo.file.MultipartUploadDAO;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.springframework.test.util.ReflectionTestUtils;

public class MultipartManagerV2ImplTest {
	
	@Mock
	UserInfo userInfo;
	@Mock
	MultipartUploadDAO mockMultiparUploadDAO;

	MultipartUploadRequest request;	
	MultipartManagerV2Impl manager;
	Boolean forceRestart;
	
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
		
		forceRestart = null;
		manager = new MultipartManagerV2Impl();
		ReflectionTestUtils.setField(manager, "multiparUploadDAO", mockMultiparUploadDAO);
		
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
		String md5Hex = MultipartManagerV2Impl.calculateMD5AsHex(request);
		assertEquals(expected, md5Hex);
	}
	
	@Test
	public void testCalculateNumberOfPartsSmall(){
		long fileSize = 1;
		long partSize = MultipartManagerV2Impl.MIN_PART_SIZE_BYTES;
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(1, numberOfParts);
	}

	@Test
	public void testCalculateNumberOfPartsNoRemainder(){
		long fileSize = MultipartManagerV2Impl.MIN_PART_SIZE_BYTES*2;
		long partSize = MultipartManagerV2Impl.MIN_PART_SIZE_BYTES;
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(2, numberOfParts);
	}
	
	@Test
	public void testCalculateNumberOfPartsWithRemainder(){
		long fileSize = MultipartManagerV2Impl.MIN_PART_SIZE_BYTES*2+1;
		long partSize = MultipartManagerV2Impl.MIN_PART_SIZE_BYTES;
		int numberOfParts = MultipartManagerV2Impl.calculateNumberOfParts(fileSize, partSize);
		assertEquals(3, numberOfParts);
	}
}
