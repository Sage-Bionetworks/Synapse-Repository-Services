package org.sagebionetworks.upload.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.collections4.ListUtils;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.dbo.file.CompositeMultipartUploadStatus;
import org.sagebionetworks.repo.model.file.AbortMultipartRequest;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.PartMD5;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.util.ContentDispositionUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.AmazonServiceException.ErrorType;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.Region;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class S3MultipartUploadDAOImplTest {
	
	@Mock
	private SynapseS3Client mockS3Client;
	@Mock
	private InitiateMultipartUploadResult mockResult;
	@InjectMocks
	private S3MultipartUploadDAOImpl dao;
	@Mock
	private LoggerProvider mockLoggerProvider;
	@Mock
	private Logger mockLogger;
	
	private String bucket;
	private String key;
	private MultipartUploadRequest request;
	private String uploadId;
	private String filename;

	@Mock
	private ObjectMetadata mockObjectMetadata;
	
	@BeforeEach
	public void before() {
		
		bucket = "someBucket";
		key = "somKey";
		filename = "foo.txt";

		request = new MultipartUploadRequest();
		request.setContentMD5Hex("8356accbaa8bfc6ddc6c612224c6c9b3");
		request.setFileName(filename);
		request.setContentType("text/plain");
		uploadId = "someUploadId";
		
		when(mockLoggerProvider.getLogger(any())).thenReturn(mockLogger);
		dao.configureLogger(mockLoggerProvider);
	}
	
	@AfterEach
	public void after() {
		verify(mockLoggerProvider).getLogger(S3MultipartUploadDAOImpl.class.getName());
	}
	
	@Test
	public void testInitiateMultipartUpload(){
		when(mockResult.getUploadId()).thenReturn(uploadId);
		when(mockS3Client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(mockResult);
		
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
		when(mockResult.getUploadId()).thenReturn(uploadId);
		when(mockS3Client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(mockResult);
		
		request.setContentType(null);
		String result = dao.initiateMultipartUpload(bucket, key, request);
		assertEquals(uploadId, result);
		ArgumentCaptor<InitiateMultipartUploadRequest> capture = ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
		verify(mockS3Client).initiateMultipartUpload(capture.capture());
		assertEquals("application/octet-stream", capture.getValue().getObjectMetadata().getContentType());
	}

	@Test
	public void testInitiateMultipartUploadContentTypeEmptyString(){
		when(mockResult.getUploadId()).thenReturn(uploadId);
		when(mockS3Client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(mockResult);
		
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
		PresignedUrl url = dao.createPartUploadPreSignedUrl("bucket", "key", contentType);
		assertNotNull(url.getUrl());
		assertNull(url.getSignedHeaders());
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
		PresignedUrl url = dao.createPartUploadPreSignedUrl("bucket", "key", contentType);
		assertNotNull(url.getUrl());
		assertNull(url.getSignedHeaders());
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
		PresignedUrl url = dao.createPartUploadPreSignedUrl("bucket", "key", contentType);
		assertNotNull(url.getUrl());
		assertEquals(Collections.singletonMap("Content-Type", contentType), url.getSignedHeaders());
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
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			dao.validateAndAddPart(request);
		}).getMessage();
		
		assertEquals("The provided MD5 does not match the MD5 of the uploaded part.  Please re-upload the part.", errorMessage);
				
		verify(mockS3Client, never()).deleteObject(any(), any());
	}
	
	@Test
	public void testValidatePartCopy() {
		long partNumber = 1;
		String partMD5Hex = "md5";

		dao.validatePartCopy(new CompositeMultipartUploadStatus(), partNumber, partMD5Hex);
		
		verifyZeroInteractions(mockS3Client);
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
	@Test
	public void testCompleteMultipartUploadAmazonS3Exception() {
		String key = "someKey";
		String bucket = "someBucket";
		
		String md5Hex1 = "e31f1bce63e28dd8157876638818284c";
		String md5Hex2 = "c295c08ccfd979130729592bf936b85f";
		
		CompleteMultipartRequest request = new CompleteMultipartRequest();
		request.setAddedParts(Lists.newArrayList(new PartMD5(1,md5Hex1), new PartMD5(2,md5Hex2)));
		request.setBucket(bucket);
		request.setKey(key);
		request.setUploadToken("uplaodToken");
		// setup an AmazonS3Exception on complete.
		when(mockS3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class))).thenThrow(new AmazonS3Exception("Your proposed upload is smaller than the minimum allowed size") );
		
		assertThrows(IllegalArgumentException.class, () -> {			
			// call under test
			dao.completeMultipartUpload(request);
		});
	}
	
	@Test
	public void testInitiateMultipartUploadCopy() {
		S3FileHandle sourceFile = new S3FileHandle();
		
		sourceFile.setBucketName("sourceBucket");
		sourceFile.setFileName("filename");
		sourceFile.setContentType("text/plain");
		sourceFile.setContentMd5("8356accbaa8bfc6ddc6c612224c6c9b3");
		
		MultipartUploadCopyRequest request = new MultipartUploadCopyRequest();
		request.setFileName("targetFileName");
		
		when(mockResult.getUploadId()).thenReturn(uploadId);
		when(mockS3Client.initiateMultipartUpload(any())).thenReturn(mockResult);
		when(mockS3Client.getRegionForBucket(any())).thenReturn(Region.US_Standard);
		
		// Call under test
		String result = dao.initiateMultipartUploadCopy(bucket, key, request, sourceFile);
		
		assertEquals(uploadId, result);
		
		ArgumentCaptor<InitiateMultipartUploadRequest> capture = ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
		
		verify(mockS3Client).getRegionForBucket(sourceFile.getBucketName());
		verify(mockS3Client).getRegionForBucket(bucket);
		verify(mockS3Client).initiateMultipartUpload(capture.capture());
		
		InitiateMultipartUploadRequest s3Request = capture.getValue();
		
		assertEquals(bucket, s3Request.getBucketName());
		assertEquals(key, s3Request.getKey());
		assertEquals(ContentDispositionUtils.getContentDispositionValue(request.getFileName()), s3Request.getObjectMetadata().getContentDisposition());
		assertEquals("text/plain", s3Request.getObjectMetadata().getContentType());
		assertEquals("g1asy6qL/G3cbGEiJMbJsw==", s3Request.getObjectMetadata().getContentMD5());
		assertEquals(CannedAccessControlList.BucketOwnerFullControl, capture.getValue().getCannedACL());
	}
	
	
	@Test
	public void testInitiateMultipartUploadCopyWithUnsupportedFileHandleType() {
		ExternalFileHandle sourceFile = new ExternalFileHandle();
		
		sourceFile.setFileName("filename");
		sourceFile.setContentType("text/plain");
		sourceFile.setContentMd5("8356accbaa8bfc6ddc6c612224c6c9b3");
		
		MultipartUploadCopyRequest request = new MultipartUploadCopyRequest();
		request.setFileName("targetFileName");
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {			
			// Call under test
			dao.initiateMultipartUploadCopy(bucket, key, request, sourceFile);
		}).getMessage();
		
		assertEquals("The file handle must point to an S3 location.", errorMessage);
		
		verifyZeroInteractions(mockS3Client);
	}
	
	@Test
	public void testInitiateMultipartUploadCopyWithDifferentRegions() {
		S3FileHandle sourceFile = new S3FileHandle();
		
		sourceFile.setBucketName("sourceBucket");
		sourceFile.setFileName("filename");
		sourceFile.setContentType("text/plain");
		sourceFile.setContentMd5("8356accbaa8bfc6ddc6c612224c6c9b3");
		
		MultipartUploadCopyRequest request = new MultipartUploadCopyRequest();
		request.setFileName("targetFileName");
		
		when(mockS3Client.getRegionForBucket(any())).thenReturn(Region.US_Standard, Region.US_East_2);
		
		String errorMessage = assertThrows(UnsupportedOperationException.class, () -> {			
			// Call under test
			dao.initiateMultipartUploadCopy(bucket, key, request, sourceFile);
		}).getMessage();
		
		verify(mockS3Client).getRegionForBucket(sourceFile.getBucketName());
		verify(mockS3Client).getRegionForBucket(bucket);
		
		assertEquals("Copying a file that is stored in a different region than the destination is not supported.", errorMessage);

	}
	
	@Test
	public void testInitiateMultipartUploadCopySameBucket() {
		S3FileHandle sourceFile = new S3FileHandle();
		
		sourceFile.setBucketName(bucket);
		sourceFile.setFileName("filename");
		sourceFile.setContentType("text/plain");
		sourceFile.setContentMd5("8356accbaa8bfc6ddc6c612224c6c9b3");
		
		MultipartUploadCopyRequest request = new MultipartUploadCopyRequest();
		request.setFileName("targetFileName");
		
		when(mockResult.getUploadId()).thenReturn(uploadId);
		when(mockS3Client.initiateMultipartUpload(any())).thenReturn(mockResult);
		
		// Call under test
		String result = dao.initiateMultipartUploadCopy(bucket, key, request, sourceFile);
		
		assertEquals(uploadId, result);
		
		ArgumentCaptor<InitiateMultipartUploadRequest> capture = ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
		
		// Since the source and target bucket are the same there is no need to invoke the getRegionForBucket
		verify(mockS3Client, times(0)).getRegionForBucket(any());
		verify(mockS3Client).initiateMultipartUpload(capture.capture());
		
		InitiateMultipartUploadRequest s3Request = capture.getValue();
		
		assertEquals(bucket, s3Request.getBucketName());
		assertEquals(key, s3Request.getKey());
		assertEquals(ContentDispositionUtils.getContentDispositionValue(request.getFileName()), s3Request.getObjectMetadata().getContentDisposition());
		assertEquals("text/plain", s3Request.getObjectMetadata().getContentType());
		assertEquals("g1asy6qL/G3cbGEiJMbJsw==", s3Request.getObjectMetadata().getContentMD5());
		assertEquals(CannedAccessControlList.BucketOwnerFullControl, capture.getValue().getCannedACL());
	}
	
	
	@Test
	public void testCreatePartUploadCopyPresignedUrl() throws MalformedURLException {

		Long fileHandleId = 123L;
		Long fileSize = 1024 * 1024L;
		Long partSize = 1024L;
		
		String sourceBucket = "sourceBucket";
		String sourceKey = "sourceKey";
		String sourceEtag = "sourceEtag";
		long partNumber = 1;
		String contentType = "plain/text";
		
		CompositeMultipartUploadStatus status = new CompositeMultipartUploadStatus();
		
		status.setUploadToken(uploadId);
		status.setBucket(bucket);
		status.setKey(key);
		status.setPartSize(partSize);
		
		status.setSourceFileHandleId(fileHandleId);
		status.setSourceFileEtag(sourceEtag);
		status.setSourceFileSize(fileSize);
		status.setSourceBucket(sourceBucket);
		status.setSourceKey(sourceKey);
		
		URL url = new URL("http", "amazon.com", bucket + "/" + key);
		
		when(mockS3Client.generatePresignedUrl(any())).thenReturn(url);
		
		// Call under test
		PresignedUrl result = dao.createPartUploadCopyPresignedUrl(status, partNumber, contentType);
		
		assertEquals(url, result.getUrl());
		
		Map<String, String> expectedSignedHeaders = ImmutableMap.of(
				"x-amz-copy-source", sourceBucket + "/" + sourceKey,
				"x-amz-copy-source-range", "bytes=0-1023",
				"x-amz-copy-source-if-match", sourceEtag,
				"Content-Type", contentType
		);
		
		assertEquals(expectedSignedHeaders, result.getSignedHeaders());
		
		ArgumentCaptor<GeneratePresignedUrlRequest> captor = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
		
		verify(mockS3Client).generatePresignedUrl(captor.capture());
		
		GeneratePresignedUrlRequest request = captor.getValue();
		
		assertEquals(bucket, request.getBucketName());
		assertEquals(key, request.getKey());
		assertEquals(HttpMethod.PUT, request.getMethod());
		assertEquals(contentType, request.getContentType());
		assertNotNull(request.getExpiration());
		
		assertEquals(ImmutableMap.of(
				"partNumber", String.valueOf(partNumber),
				"uploadId", uploadId
		), request.getRequestParameters());
		
		assertEquals(ImmutableMap.of(
				"x-amz-copy-source", sourceBucket + "/" + sourceKey,
				"x-amz-copy-source-range", "bytes=0-1023",
				"x-amz-copy-source-if-match", sourceEtag
		), request.getCustomRequestHeaders());
		
	}
	
	@Test
	public void testCreatePartUploadCopyPresignedUrlWithNoSourceFile() {

		Long fileHandleId = null;
		Long fileSize = 1024 * 1024L;
		Long partSize = 1024L;
		
		String sourceBucket = "sourceBucket";
		String sourceKey = "sourceKey";
		long partNumber = 1;
		String contentType = "plain/text";
		
		CompositeMultipartUploadStatus status = new CompositeMultipartUploadStatus();
		
		status.setUploadToken(uploadId);
		status.setBucket(bucket);
		status.setKey(key);
		status.setPartSize(partSize);
		
		status.setSourceFileHandleId(fileHandleId);
		status.setSourceFileSize(fileSize);
		status.setSourceBucket(sourceBucket);
		status.setSourceKey(sourceKey);
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			dao.createPartUploadCopyPresignedUrl(status, partNumber, contentType);
		}).getMessage();
		
		assertEquals("Expected a source file, found none.", errorMessage);
		
	}
	
	@Test
	public void testCreatePartUploadCopyPresignedUrlWithNoFileSize() {

		Long fileHandleId = 123L;
		Long fileSize = null;
		Long partSize = 1024L;
		
		String sourceBucket = "sourceBucket";
		String sourceKey = "sourceKey";
		String sourceEtag = "sourceEtag";
		long partNumber = 1;
		String contentType = "plain/text";
		
		CompositeMultipartUploadStatus status = new CompositeMultipartUploadStatus();
		
		status.setUploadToken(uploadId);
		status.setBucket(bucket);
		status.setKey(key);
		status.setPartSize(partSize);
		
		status.setSourceFileHandleId(fileHandleId);
		status.setSourceFileEtag(sourceEtag);
		status.setSourceFileSize(fileSize);
		status.setSourceBucket(sourceBucket);
		status.setSourceKey(sourceKey);
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			dao.createPartUploadCopyPresignedUrl(status, partNumber, contentType);
		}).getMessage();
		
		assertEquals("Expected the source file size, found none.", errorMessage);
		
	}
	
	@Test
	public void testCreatePartUploadCopyPresignedUrlWithNoPartSize() {

		Long fileHandleId = 123L;
		Long fileSize = 1024 * 1024L;
		Long partSize = null;
		
		String sourceBucket = "sourceBucket";
		String sourceKey = "sourceKey";
		String sourceEtag = "sourceEtag";
		long partNumber = 1;
		String contentType = "plain/text";
		
		CompositeMultipartUploadStatus status = new CompositeMultipartUploadStatus();
		
		status.setUploadToken(uploadId);
		status.setBucket(bucket);
		status.setKey(key);
		status.setPartSize(partSize);
		
		status.setSourceFileHandleId(fileHandleId);
		status.setSourceFileEtag(sourceEtag);
		status.setSourceFileSize(fileSize);
		status.setSourceBucket(sourceBucket);
		status.setSourceKey(sourceKey);
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			dao.createPartUploadCopyPresignedUrl(status, partNumber, contentType);
		}).getMessage();
		
		assertEquals("Expected a part size, found none.", errorMessage);
		
	}
	
	@Test
	public void testCreatePartUploadCopyPresignedUrlWithSourceBucket() {

		Long fileHandleId = 123L;
		Long fileSize = 1024 * 1024L;
		Long partSize = 1024L;
		
		String sourceBucket = null;
		String sourceKey = "sourceKey";
		String sourceEtag = "sourceEtag";
		long partNumber = 1;
		String contentType = "plain/text";
		
		CompositeMultipartUploadStatus status = new CompositeMultipartUploadStatus();
		
		status.setUploadToken(uploadId);
		status.setBucket(bucket);
		status.setKey(key);
		status.setPartSize(partSize);
		
		status.setSourceFileHandleId(fileHandleId);
		status.setSourceFileEtag(sourceEtag);
		status.setSourceFileSize(fileSize);
		status.setSourceBucket(sourceBucket);
		status.setSourceKey(sourceKey);
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			dao.createPartUploadCopyPresignedUrl(status, partNumber, contentType);
		}).getMessage();
		
		assertEquals("Expected the source file bucket, found none.", errorMessage);
		
	}
	
	@Test
	public void testCreatePartUploadCopyPresignedUrlWithSourceBucketKey() {

		Long fileHandleId = 123L;
		Long fileSize = 1024 * 1024L;
		Long partSize = 1024L;
		
		String sourceBucket = "sourceBucket";
		String sourceKey = null;
		String sourceEtag = "sourceEtag";
		long partNumber = 1;
		String contentType = "plain/text";
		
		CompositeMultipartUploadStatus status = new CompositeMultipartUploadStatus();
		
		status.setUploadToken(uploadId);
		status.setBucket(bucket);
		status.setKey(key);
		status.setPartSize(partSize);
		
		status.setSourceFileHandleId(fileHandleId);
		status.setSourceFileEtag(sourceEtag);
		status.setSourceFileSize(fileSize);
		status.setSourceBucket(sourceBucket);
		status.setSourceKey(sourceKey);
		
		String errorMessage = assertThrows(IllegalStateException.class, () -> {			
			// Call under test
			dao.createPartUploadCopyPresignedUrl(status, partNumber, contentType);
		}).getMessage();
		
		assertEquals("Expected the source file bucket key, found none.", errorMessage);
		
	}
	
	@Test
	public void testAbortMultipartRequestWithNoKeys() {
		
		doNothing().when(mockS3Client).abortMultipartUpload(any());
		
		AbortMultipartRequest request = new AbortMultipartRequest(uploadId, "token", bucket, key);
		
		// Call under test
		dao.tryAbortMultipartRequest(request);
		
		verify(mockS3Client, never()).deleteObjects(any());
		
		ArgumentCaptor<AbortMultipartUploadRequest> requestCaptor = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
		
		verify(mockS3Client).abortMultipartUpload(requestCaptor.capture());
		
		AbortMultipartUploadRequest captured = requestCaptor.getValue();
		
		assertEquals(request.getBucket(), captured.getBucketName());
		assertEquals(request.getKey(), captured.getKey());
		assertEquals(request.getUploadToken(), captured.getUploadId());
	}
	
	@Test
	public void testAbortMultipartRequestWithKeys() {
		
		doNothing().when(mockS3Client).abortMultipartUpload(any());
		
		List<String> partKeys = Arrays.asList("part1", "part2");
		
		AbortMultipartRequest request = new AbortMultipartRequest(uploadId, "token", bucket, key).withPartKeys(partKeys);
		
		// Call under test
		dao.tryAbortMultipartRequest(request);
		
		ArgumentCaptor<DeleteObjectsRequest> deleteRequestCaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
		
		verify(mockS3Client).deleteObjects(deleteRequestCaptor.capture());
		
		DeleteObjectsRequest capturedDeleteRequest = deleteRequestCaptor.getValue();
		
		assertEquals(request.getBucket(), capturedDeleteRequest.getBucketName());
		assertEquals(partKeys, capturedDeleteRequest.getKeys().stream().map(k->k.getKey()).collect(Collectors.toList()));
		
		ArgumentCaptor<AbortMultipartUploadRequest> abortRequestCaptor = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
		
		verify(mockS3Client).abortMultipartUpload(abortRequestCaptor.capture());
		
		AbortMultipartUploadRequest capturedAbortRequest = abortRequestCaptor.getValue();
		
		assertEquals(request.getBucket(), capturedAbortRequest.getBucketName());
		assertEquals(request.getKey(), capturedAbortRequest.getKey());
		assertEquals(request.getUploadToken(), capturedAbortRequest.getUploadId());
	}
	
	@Test
	public void testAbortMultipartRequestWithLotsOfKeys() {
		
		doNothing().when(mockS3Client).abortMultipartUpload(any());
		
		List<String> partKeys = IntStream.range(1, S3MultipartUploadDAOImpl.S3_BATCH_DELETE_SIZE * 2).boxed().map(n -> String.valueOf(n)).collect(Collectors.toList());
		
		AbortMultipartRequest request = new AbortMultipartRequest(uploadId, "token", bucket, key).withPartKeys(partKeys);
		
		// Call under test
		dao.tryAbortMultipartRequest(request);
		
		ArgumentCaptor<DeleteObjectsRequest> deleteRequestCaptor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
		
		List<List<String>> partitions = ListUtils.partition(partKeys, S3MultipartUploadDAOImpl.S3_BATCH_DELETE_SIZE);
		
		verify(mockS3Client, times(partitions.size())).deleteObjects(deleteRequestCaptor.capture());
				
		List<DeleteObjectsRequest> capturedDeleteRequest = deleteRequestCaptor.getAllValues();
		
		for (int i=0; i< capturedDeleteRequest.size(); i++) {
			assertEquals(request.getBucket(), capturedDeleteRequest.get(i).getBucketName());
			assertEquals(partitions.get(i), capturedDeleteRequest.get(i).getKeys().stream().map(k -> k.getKey()).collect(Collectors.toList()));		
		}
		
		
		ArgumentCaptor<AbortMultipartUploadRequest> abortRequestCaptor = ArgumentCaptor.forClass(AbortMultipartUploadRequest.class);
		
		verify(mockS3Client).abortMultipartUpload(abortRequestCaptor.capture());
		
		AbortMultipartUploadRequest capturedAbortRequest = abortRequestCaptor.getValue();
		
		assertEquals(request.getBucket(), capturedAbortRequest.getBucketName());
		assertEquals(request.getKey(), capturedAbortRequest.getKey());
		assertEquals(request.getUploadToken(), capturedAbortRequest.getUploadId());
	}
	
	@Test
	public void testAbortMultipartRequestWithPartException() {
		AmazonServiceException ex = new AmazonServiceException("Something went wrong");
		
		ex.setErrorType(ErrorType.Service);
		
		doThrow(ex).when(mockS3Client).deleteObjects(any());
		
		List<String> partKeys = Arrays.asList("part1", "part2");
		
		AbortMultipartRequest request = new AbortMultipartRequest(uploadId, "token", bucket, key).withPartKeys(partKeys);
				
		// Call under test
		dao.tryAbortMultipartRequest(request);
		
		verify(mockS3Client).deleteObjects(any());
		verify(mockS3Client).abortMultipartUpload(any());
		verify(mockLogger).warn(ex.getMessage(), ex);
		
	}
	
	@Test
	public void testAbortMultipartRequestWithAbportException() {
		AmazonServiceException ex = new AmazonServiceException("Something went wrong");
				
		doThrow(ex).when(mockS3Client).abortMultipartUpload(any());
		
		List<String> partKeys = Arrays.asList("part1", "part2");
		
		AbortMultipartRequest request = new AbortMultipartRequest(uploadId, "token", bucket, key).withPartKeys(partKeys);
							
		// Call under test
		dao.tryAbortMultipartRequest(request);
		
		verify(mockS3Client).deleteObjects(any());
		verify(mockS3Client).abortMultipartUpload(any());
		verify(mockLogger).warn(ex.getMessage(), ex);
	}
	
	
	@Test
	public void testGetObjectEtag() {
		String etag = "etag";
		
		when(mockS3Client.getObjectMetadata(any(), any())).thenReturn(mockObjectMetadata);
		when(mockObjectMetadata.getETag()).thenReturn(etag);
		
		// Call under test
		String result = dao.getObjectEtag(bucket, key);
	
		assertEquals(etag, result);
		
		verify(mockS3Client).getObjectMetadata(bucket, key);
		verify(mockObjectMetadata).getETag();
		verifyNoMoreInteractions(mockS3Client);
		verifyNoMoreInteractions(mockObjectMetadata);
	}

}
