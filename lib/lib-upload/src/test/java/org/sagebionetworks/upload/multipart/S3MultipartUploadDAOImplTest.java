package org.sagebionetworks.upload.multipart;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;

public class S3MultipartUploadDAOImplTest {
	
	@Mock
	AmazonS3Client mockS3Client;
	@Mock
	InitiateMultipartUploadResult mockResult;
	
	S3MultipartUploadDAOImpl dao;
	
	String bucket;
	String key;
	MultipartUploadRequest request;
	String uploadId;

	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		bucket = "someBucket";
		key = "somKey";
		request = new MultipartUploadRequest();
		request.setContentMD5Hex("8356accbaa8bfc6ddc6c612224c6c9b3");
		request.setFileName("foo.txt");
		request.setContentType("text/plain");
		uploadId = "someUploadId";
		
		dao = new S3MultipartUploadDAOImpl();
		ReflectionTestUtils.setField(dao, "s3Client", mockS3Client);
		
		when(mockResult.getUploadId()).thenReturn(uploadId);
		
		when(mockS3Client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(mockResult);
	}
	
	@Test
	public void testInitiateMultipartUpload(){
		String result = dao.initiateMultipartUpload(bucket, key, request);
		assertEquals(uploadId, result);
		ArgumentCaptor<InitiateMultipartUploadRequest> capture = ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
		verify(mockS3Client).initiateMultipartUpload(capture.capture());
		assertEquals(bucket, capture.getValue().getBucketName());
		assertEquals(key, capture.getValue().getKey());
		assertEquals("attachment; filename=foo.txt", capture.getValue().getObjectMetadata().getContentDisposition());
		assertEquals("text/plain", capture.getValue().getObjectMetadata().getContentType());
		assertEquals("g1asy6qL/G3cbGEiJMbJsw==", capture.getValue().getObjectMetadata().getContentMD5());
	}
	
	@Test
	public void testInitiateMultipartUploadContentTypeNull(){
		request.setContentType(null);
		String result = dao.initiateMultipartUpload(bucket, key, request);
		assertEquals(uploadId, result);
		ArgumentCaptor<InitiateMultipartUploadRequest> capture = ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
		verify(mockS3Client).initiateMultipartUpload(capture.capture());
		assertEquals("application/octet-stream", capture.getValue().getObjectMetadata().getContentType());
	}

}
