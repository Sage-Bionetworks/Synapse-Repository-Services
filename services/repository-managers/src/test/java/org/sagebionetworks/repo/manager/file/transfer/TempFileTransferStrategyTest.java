package org.sagebionetworks.repo.manager.file.transfer;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.util.FixedMemoryPool;
import org.sagebionetworks.repo.util.FixedMemoryPool.BlockConsumer;
import org.sagebionetworks.repo.util.FixedMemoryPool.NoBlocksAvailableException;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.StringInputStream;

/**
 * Unit test for TempFileTransferStrategy
 * @author John
 *
 */
public class TempFileTransferStrategyTest {

	public static final int FIVE_MB = ((int) Math.pow(2, 20))*5;
	public static final double NANOSECONDS_PER_MILLISECOND = 1000000;
	
	AmazonS3Client mockS3Client;
	TempFileTransferStrategy strategy;
	TransferRequest transferRequest;
	InputStream inputStream;
	String inputStreamContent;
	String expectedMD5;
	Long expectedContentLength;
	
	@Before
	public void before() throws UnsupportedEncodingException, NoSuchAlgorithmException{
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		strategy = new TempFileTransferStrategy(mockS3Client);
		inputStreamContent = "This will be our simple stream for TempFileTransferStrategyTest";
		// Calculate the expected MD5 of the content.
		MessageDigest md5Digets =  MessageDigest.getInstance("MD5");
		byte[] contentBytes = inputStreamContent.getBytes();
		expectedContentLength = new Long(contentBytes.length);
		md5Digets.update(contentBytes);
		expectedMD5 = BinaryUtils.toHex(md5Digets.digest());
		
		inputStream = new StringInputStream(inputStreamContent);
		transferRequest = new TransferRequest();
		transferRequest.setS3bucketName("bucket");
		transferRequest.setS3key("key");
		transferRequest.setInputStream(inputStream);
		transferRequest.setContentType("contentType");
		transferRequest.setContentMD5(expectedMD5);
		transferRequest.setFileName("foo.txt");
		
		// Setup the S3 calls
//		InitiateMultipartUploadResult result = new InitiateMultipartUploadResult();
//		result.setBucketName(transferRequest.getS3bucketName());
//		result.setKey(transferRequest.getS3key());
//		when(mockS3Client.initiateMultipartUpload(any(InitiateMultipartUploadRequest.class))).thenReturn(result);
//		UploadPartResult uploadPart = new UploadPartResult();
//		uploadPart.setETag("etag");
//		when(mockS3Client.uploadPart(any(UploadPartRequest.class))).thenReturn(uploadPart);
		
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTransferToS3RequestNull() throws Exception{
		// Pass a null request
		strategy.transferToS3(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTransferToS3BufferNull() throws Exception{
		strategy.transferToS3(transferRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTransferToS3RequestNullBucket() throws Exception{
		transferRequest.setS3bucketName(null);
		strategy.transferToS3(transferRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTransferToS3RequestNullKey() throws Exception{
		transferRequest.setS3key(null);
		strategy.transferToS3(transferRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTransferToS3RequestNullInput() throws Exception{
		transferRequest.setInputStream(null);
		strategy.transferToS3(transferRequest);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTransferToS3RequestNullName() throws Exception{
		transferRequest.setFileName(null);
		strategy.transferToS3(transferRequest);
	}
	
	@Test
	public void testTransferToS3() throws Exception {
		// This call should work and fill in all of the data based on what is passed.
		S3FileMetadata meta = strategy.transferToS3(transferRequest);
		assertNotNull(meta);
		assertEquals(transferRequest.getS3bucketName(), meta.getBucketName());
		assertEquals(transferRequest.getS3key(), meta.getKey());
		assertEquals(expectedMD5, meta.getContentMd5());
		assertEquals(expectedContentLength, meta.getContentSize());
		assertEquals(transferRequest.getContentType(), meta.getContentType());
		assertEquals(transferRequest.getFileName(), meta.getFileName());
	}
	

	@Test
	public void testInvalidPassedMD5DeleteFile() throws IOException, ServiceUnavailableException{
		// Validate that we delete the file sent to S3 when the  MD5 does not match
		transferRequest.setContentMD5("wrongMD5");
		try{
			strategy.transferToS3(transferRequest);
			fail("This should have failed as the MD5 did not match");
		}catch(IllegalArgumentException e){
			// check the message.
			assertTrue("The messages should contain the wrong MD5",e.getMessage().indexOf("wrongMD5") > -1);
			assertTrue("The messages should contain the calcualted MD5",e.getMessage().indexOf(expectedMD5) > -1);
		}
	}

}
