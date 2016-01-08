package org.sagebionetworks.repo.manager.file.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.file.TempFileProvider;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

import com.amazonaws.services.s3.AmazonS3Client;
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
	TempFileProvider mockTempFileProvider;
	TransferRequest transferRequest;
	FileOutputStream mockFileOutputStream;
	File mockTempFile;
	InputStream inputStream;
	String inputStreamContent;
	String expectedMD5;
	Long expectedContentLength;
	
	@Before
	public void before() throws NoSuchAlgorithmException, IOException{
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		mockTempFileProvider = Mockito.mock(TempFileProvider.class);
		mockTempFile = Mockito.mock(File.class);
		mockFileOutputStream = Mockito.mock(FileOutputStream.class);
		when(mockTempFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockTempFile);
		when(mockTempFileProvider.createFileOutputStream(any(File.class))).thenReturn(mockFileOutputStream);
		strategy = new TempFileTransferStrategy(mockS3Client, mockTempFileProvider);
		inputStreamContent = "This will be our simple stream for TempFileTransferStrategyTest";
		// Calculate the expected MD5 of the content.
		MessageDigest md5Digets =  MessageDigest.getInstance("MD5");
		byte[] contentBytes = inputStreamContent.getBytes();
		expectedContentLength = new Long(contentBytes.length);
		when(mockTempFile.length()).thenReturn(expectedContentLength);
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

	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testTransferToS3RequestNull() throws Exception{
		// Pass a null request
		strategy.transferToS3(null);
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
		S3FileHandle meta = strategy.transferToS3(transferRequest);
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
		transferRequest.setContentMD5("1234");
		try{
			strategy.transferToS3(transferRequest);
			fail("This should have failed as the MD5 did not match");
		}catch(IllegalArgumentException e){
			// Even though it failed the stream must get closed and the file must get deleted
			// The stream must be closed
			verify(mockFileOutputStream, times(1)).close();
			// The temp file must get deleted
			verify(mockTempFile, times(1)).delete();
			// check the message.
			System.out.println(e.getMessage());
			assertTrue("The messages should contain the wrong MD5",e.getMessage().indexOf("1234") > -1);
			assertTrue("The messages should contain the calculated MD5",e.getMessage().indexOf(expectedMD5) > -1);
		}
	}
	
	@Test
	public void testDeleteAndCloseTempFile() throws ServiceUnavailableException, IOException{
		strategy.transferToS3(transferRequest);
		// The stream must be closed
		verify(mockFileOutputStream, times(1)).close();
		// The temp file must get deleted
		verify(mockTempFile, times(1)).delete();
	}
	

}
