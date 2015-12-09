package org.sagebionetworks.upload.multipart;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.util.BinaryUtils;
import com.google.common.collect.Lists;

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
	
	@Test
	public void testcreatePreSignedPutUrl() throws AmazonClientException, MalformedURLException{
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL("http", "amazon.com", "bucket/key"));
		//call under test.
		URL url = dao.createPreSignedPutUrl("bucket", "key");
		assertNotNull(url);
		ArgumentCaptor<GeneratePresignedUrlRequest> capture = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
		verify(mockS3Client).generatePresignedUrl(capture.capture());
		assertEquals("bucket", capture.getValue().getBucketName());
		assertEquals("key", capture.getValue().getKey());
		// expiration should be set for the future.
		Date now = new Date(System.currentTimeMillis());
		assertTrue(now.before( capture.getValue().getExpiration()));
	}
	
	@Test
	public void testAddPart(){
		CopyPartResult result = new CopyPartResult();
		when(mockS3Client.copyPart(any(CopyPartRequest.class))).thenReturn(result);
		
		String uplaodToken = "uploadToken";
		String partKey = key+"/101";
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		String partEtag = BinaryUtils.toBase64(BinaryUtils.fromHex(partMD5Hex));
		int partNumber = 101;
		AddPartRequest request  = new AddPartRequest(uplaodToken, bucket, key, partKey, partMD5Hex, partNumber);
		// call under test.
		dao.addPart(request);
		
		ArgumentCaptor<CopyPartRequest> capture = ArgumentCaptor.forClass(CopyPartRequest.class);
		verify(mockS3Client).copyPart(capture.capture());
		assertEquals(bucket, capture.getValue().getSourceBucketName());
		assertEquals(partKey, capture.getValue().getSourceKey());
		assertEquals(bucket, capture.getValue().getDestinationBucketName());
		assertEquals(key, capture.getValue().getDestinationKey());
		assertEquals(uplaodToken, capture.getValue().getUploadId());
		assertEquals(partNumber, capture.getValue().getPartNumber());
		assertEquals(Lists.newArrayList(partEtag), capture.getValue().getMatchingETagConstraints());
	}
	
	@Test
	public void testAddPartWrongEtag(){
		// returning a null indicates an abort.
		CopyPartResult result = null;
		when(mockS3Client.copyPart(any(CopyPartRequest.class))).thenReturn(result);
		
		String uplaodToken = "uploadToken";
		String partKey = key+"/101";
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		String partEtag = BinaryUtils.toBase64(BinaryUtils.fromHex(partMD5Hex));
		int partNumber = 101;
		AddPartRequest request  = new AddPartRequest(uplaodToken, bucket, key, partKey, partMD5Hex, partNumber);
		// call under test.
		try {
			dao.addPart(request);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("The provided MD5 does not match"));
		}
	}

}
