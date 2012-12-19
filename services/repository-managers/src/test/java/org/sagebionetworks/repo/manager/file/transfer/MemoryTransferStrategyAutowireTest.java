package org.sagebionetworks.repo.manager.file.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.util.FixedMemoryPool;
import org.sagebionetworks.repo.web.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.StringInputStream;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class MemoryTransferStrategyAutowireTest {
	
	@Autowired
	MemoryTransferStrategy memoryTransferStrategy;
	@Autowired
	FixedMemoryPool fileTransferFixedMemoryPool;
	@Autowired
	AmazonS3Client s3Client;
	
	String inputStreamContent;
	InputStream inputStream;
	Long expectedContentLength;
	String expectedMD5;
	
	List<S3FileMetadata> toDelete;
	
	@Before
	public void before() throws UnsupportedEncodingException, NoSuchAlgorithmException{
		// The pool should not be null
		assertNotNull(fileTransferFixedMemoryPool);
		toDelete = new ArrayList<S3FileMetadata>();
		inputStreamContent = "This will be our simple stream";
		inputStream = new StringInputStream(inputStreamContent);
		// Calculate the expected MD5 of the content.
		MessageDigest md5Digets =  MessageDigest.getInstance("MD5");
		byte[] contentBytes = inputStreamContent.getBytes();
		expectedContentLength = new Long(contentBytes.length);
		md5Digets.update(contentBytes);
		expectedMD5 = BinaryUtils.toHex(md5Digets.digest());
	}
	
	@After
	public void after(){
		if(s3Client != null && toDelete != null){
			for(S3FileMetadata meta: toDelete){
				s3Client.deleteObject(meta.getBucketName(), meta.getKey());
			}
		}
	}
	
	@Test
	public void testTransferToS3() throws ServiceUnavailableException{
		TransferRequest request = new TransferRequest();
		String contentType = "text/plain";
		request.setContentType(contentType);
		request.setS3bucketName(StackConfiguration.getS3Bucket());
		request.setContentMD5(expectedMD5);
		request.setFileName("foo.txt");
		String key = "123/"+UUID.randomUUID();
		System.out.println(key);
		request.setS3key(key);
		request.setInputStream(inputStream);
		// Upload the file
		S3FileMetadata meta = memoryTransferStrategy.transferToS3(request);
		assertNotNull(meta);
		toDelete.add(meta);
		assertEquals(request.getS3bucketName(), meta.getBucketName());
		assertEquals(request.getS3key(), meta.getKey());
		assertEquals(expectedMD5, meta.getContentMd5());
		assertEquals(expectedContentLength, meta.getContentSize());
		assertEquals(contentType, meta.getContentType());
		// Make sure we can get the object from S3
		ObjectMetadata objectMeta = s3Client.getObjectMetadata(request.getS3bucketName(), request.getS3key());
		assertNotNull(objectMeta);
		assertEquals(expectedContentLength.longValue(), objectMeta.getContentLength());
		assertEquals(contentType, objectMeta.getContentType());
		// The file name should be in the content disposition.
		assertEquals(TransferUtils.CONTENT_DISPOSITION_PREFIX+request.getFileName(), objectMeta.getContentDisposition());
	}

}
