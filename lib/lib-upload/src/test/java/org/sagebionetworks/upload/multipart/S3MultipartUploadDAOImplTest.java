package org.sagebionetworks.upload.multipart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.file.AddPartRequest;
import org.sagebionetworks.repo.model.file.CompleteMultipartRequest;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.MultipartUploadCopyRequest;
import org.sagebionetworks.repo.model.file.MultipartUploadRequest;
import org.sagebionetworks.repo.model.file.PartMD5;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.util.ContentDispositionUtils;

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
import com.amazonaws.services.s3.model.Region;
import com.google.common.collect.Lists;

@ExtendWith(MockitoExtension.class)
public class S3MultipartUploadDAOImplTest {
	
	@Mock
	private SynapseS3Client mockS3Client;
	@Mock
	private InitiateMultipartUploadResult mockResult;
	@InjectMocks
	private S3MultipartUploadDAOImpl dao;
	
	private String bucket;
	private String key;
	private MultipartUploadRequest request;
	private String uploadId;
	private String filename;

	
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
		
		String errorMessage = assertThrows(IllegalArgumentException.class, () -> {
			// call under test.
			dao.validateAndAddPart(request);
		}).getMessage();
		
		assertEquals("The provided MD5 does not match the MD5 of the uploaded part.  Please re-upload the part.", errorMessage);
				
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
		
		String errorMessage = assertThrows(UnsupportedOperationException.class, () -> {			
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

}
