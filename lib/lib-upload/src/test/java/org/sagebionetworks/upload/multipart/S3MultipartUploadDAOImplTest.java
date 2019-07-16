package org.sagebionetworks.upload.multipart;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.PartMD5;
import org.sagebionetworks.util.ContentDispositionUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.google.common.collect.Lists;

public class S3MultipartUploadDAOImplTest {
	
	@Mock
	SynapseS3Client mockS3Client;
	@Mock
	InitiateMultipartUploadResult mockResult;
	
	S3MultipartUploadDAOImpl dao;
	
	String bucket;
	String key;
	MultipartUploadRequest request;
	String uploadId;
	String filename;

	
	@Before
	public void before(){
		MockitoAnnotations.initMocks(this);
		
		bucket = "someBucket";
		key = "somKey";
		filename = "foo.txt";

		request = new MultipartUploadRequest();
		request.setContentMD5Hex("8356accbaa8bfc6ddc6c612224c6c9b3");
		request.setFileName(filename);
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
		assertEquals(ContentDispositionUtils.getContentDispositionValue(filename), capture.getValue().getObjectMetadata().getContentDisposition());
		assertEquals("text/plain", capture.getValue().getObjectMetadata().getContentType());
		assertEquals("g1asy6qL/G3cbGEiJMbJsw==", capture.getValue().getObjectMetadata().getContentMD5());
		assertEquals(CannedAccessControlList.BucketOwnerFullControl, capture.getValue().getCannedACL());
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
	public void testInitiateMultipartUploadContentTypeEmptyString(){
		request.setContentType("");
		String result = dao.initiateMultipartUpload(bucket, key, request);
		assertEquals(uploadId, result);
		ArgumentCaptor<InitiateMultipartUploadRequest> capture = ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
		verify(mockS3Client).initiateMultipartUpload(capture.capture());
		assertEquals("application/octet-stream", capture.getValue().getObjectMetadata().getContentType());
	}


	@Test
	public void testCreatePreSignedPutUrl() throws AmazonClientException, MalformedURLException{
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL("http", "amazon.com", "bucket/key"));
		String contentType = null;
		//call under test.
		URL url = dao.createPreSignedPutUrl("bucket", "key", contentType);
		assertNotNull(url);
		ArgumentCaptor<GeneratePresignedUrlRequest> capture = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
		verify(mockS3Client).generatePresignedUrl(capture.capture());
		assertEquals("bucket", capture.getValue().getBucketName());
		assertEquals("key", capture.getValue().getKey());
		assertNull(capture.getValue().getContentType());
		// expiration should be set for the future.
		Date now = new Date(System.currentTimeMillis());
		assertTrue(now.before( capture.getValue().getExpiration()));
	}

	@Test
	public void testCreatePreSignedPutUrlEmptyContentType() throws AmazonClientException, MalformedURLException{
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL("http", "amazon.com", "bucket/key"));
		String contentType = "";
		//call under test.
		URL url = dao.createPreSignedPutUrl("bucket", "key", contentType);
		assertNotNull(url);
		ArgumentCaptor<GeneratePresignedUrlRequest> capture = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
		verify(mockS3Client).generatePresignedUrl(capture.capture());
		assertEquals("bucket", capture.getValue().getBucketName());
		assertEquals("key", capture.getValue().getKey());
		assertNull(capture.getValue().getContentType());
		// expiration should be set for the future.
		Date now = new Date(System.currentTimeMillis());
		assertTrue(now.before( capture.getValue().getExpiration()));
	}
	
	@Test
	public void testCreatePreSignedPutUrlWithContentType() throws AmazonClientException, MalformedURLException{
		when(mockS3Client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(new URL("http", "amazon.com", "bucket/key"));
		String contentType = "text/plain";
		//call under test.
		URL url = dao.createPreSignedPutUrl("bucket", "key", contentType);
		assertNotNull(url);
		ArgumentCaptor<GeneratePresignedUrlRequest> capture = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
		verify(mockS3Client).generatePresignedUrl(capture.capture());
		assertEquals("bucket", capture.getValue().getBucketName());
		assertEquals("key", capture.getValue().getKey());
		assertEquals("text/plain", capture.getValue().getContentType());
		// expiration should be set for the future.
		Date now = new Date(System.currentTimeMillis());
		assertTrue(now.before( capture.getValue().getExpiration()));
	}
	
	
	@Test
	public void testAddPart(){
		CopyPartResult result = new CopyPartResult();
		when(mockS3Client.copyPart(any(CopyPartRequest.class))).thenReturn(result);

		String uploadId = "3553";
		String uploadToken = "uploadToken";
		String partKey = key+"/101";
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		int partNumber = 101;
		long totalNumberOfParts = 1001;
		AddPartRequest request  = new AddPartRequest(uploadId, uploadToken, bucket, key, partKey, partMD5Hex, partNumber, totalNumberOfParts);
		// call under test.
		dao.validateAndAddPart(request);
		
		ArgumentCaptor<CopyPartRequest> capture = ArgumentCaptor.forClass(CopyPartRequest.class);
		verify(mockS3Client).copyPart(capture.capture());
		assertEquals(bucket, capture.getValue().getSourceBucketName());
		assertEquals(partKey, capture.getValue().getSourceKey());
		assertEquals(bucket, capture.getValue().getDestinationBucketName());
		assertEquals(key, capture.getValue().getDestinationKey());
		assertEquals(uploadToken, capture.getValue().getUploadId());
		assertEquals(partNumber, capture.getValue().getPartNumber());
		assertEquals(Lists.newArrayList(partMD5Hex), capture.getValue().getMatchingETagConstraints());
		verify(mockS3Client).deleteObject(bucket, partKey);
	}
	
	@Test
	public void testAddPartWrongEtag(){
		// returning a null indicates an abort.
		CopyPartResult result = null;
		when(mockS3Client.copyPart(any(CopyPartRequest.class))).thenReturn(result);

		String uploadId = "3553";
		String uplaodToken = "uploadToken";
		String partKey = key+"/101";
		String partMD5Hex = "8356accbaa8bfc6ddc6c612224c6c9b3";
		int partNumber = 101;
		long totalNumberOfParts = 1001;
		AddPartRequest request  = new AddPartRequest(uploadId, uplaodToken, bucket, key, partKey, partMD5Hex, partNumber, totalNumberOfParts);
		// call under test.
		try {
			dao.validateAndAddPart(request);
			fail();
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains("The provided MD5 does not match"));
		}
		verify(mockS3Client, never()).deleteObject(any(), any());
	}
	
	
	@Test
	public void testCompleteMultipartUpload(){
		String key = "someKey";
		String bucket = "someBucket";
		// this method should lookup the file size
		long fileSize = 12345L;
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(fileSize);
		when(mockS3Client.getObjectMetadata(bucket, key)).thenReturn(metadata);
		String md5Hex1 = "e31f1bce63e28dd8157876638818284c";
		String md5Hex2 = "c295c08ccfd979130729592bf936b85f";
		String etag1 = md5Hex1;
		String etag2 = md5Hex2;
		
		CompleteMultipartRequest request = new CompleteMultipartRequest();
		request.setAddedParts(Lists.newArrayList(new PartMD5(1,md5Hex1), new PartMD5(2,md5Hex2)));
		request.setBucket(bucket);
		request.setKey(key);
		request.setUploadToken("uplaodToken");
		// call under test
		long resultFileSize = dao.completeMultipartUpload(request);
		assertEquals(fileSize, resultFileSize);
		
		// check the passed arguments
		ArgumentCaptor<CompleteMultipartUploadRequest> capture = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
		verify(mockS3Client).completeMultipartUpload(capture.capture());
		assertEquals(bucket, capture.getValue().getBucketName());
		assertEquals(key, capture.getValue().getKey());
		// the part md5 should be converted to etags (base64)
		List<PartETag> capturedEtags = capture.getValue().getPartETags();
		assertNotNull(capturedEtags);
		assertEquals(2, capturedEtags.size());
		// one
		PartETag partEtag = capturedEtags.get(0);
		assertEquals(1, partEtag.getPartNumber());
		assertEquals(etag1, partEtag.getETag());
		// two
		partEtag = capturedEtags.get(1);
		assertEquals(2, partEtag.getPartNumber());
		assertEquals(etag2, partEtag.getETag());
	}
	
	/**
	 * Test added for PLFM-4038
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testCompleteMultipartUploadAmazonS3Exception(){
		String key = "someKey";
		String bucket = "someBucket";
		// this method should lookup the file size
		long fileSize = 12345L;
		ObjectMetadata metadata = new ObjectMetadata();
		metadata.setContentLength(fileSize);
		when(mockS3Client.getObjectMetadata(bucket, key)).thenReturn(metadata);
		String md5Hex1 = "e31f1bce63e28dd8157876638818284c";
		String md5Hex2 = "c295c08ccfd979130729592bf936b85f";
		
		CompleteMultipartRequest request = new CompleteMultipartRequest();
		request.setAddedParts(Lists.newArrayList(new PartMD5(1,md5Hex1), new PartMD5(2,md5Hex2)));
		request.setBucket(bucket);
		request.setKey(key);
		request.setUploadToken("uplaodToken");
		// setup an AmazonS3Exception on complete.
		when(mockS3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenThrow(new AmazonS3Exception("Your proposed upload is smaller than the minimum allowed size") );
		// call under test
		dao.completeMultipartUpload(request);
	}

}
