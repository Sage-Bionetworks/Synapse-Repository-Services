package org.sagebionetworks.file.manager;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Mimetypes;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.BinaryUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class FileUploadManagerImplAutowireTest {
	
	List<S3FileMetadata> toDelete;
	
	@Autowired
	FileUploadManager fileUploadManager;
	
	@Autowired
	AmazonS3Client s3Client;
	
	@Before
	public void before(){
		toDelete = new LinkedList<S3FileMetadata>();
	}
	
	@After
	public void after(){
		if(toDelete != null && s3Client != null){
			// Delete any files created
			for(S3FileMetadata meta: toDelete){
				s3Client.deleteObject(meta.getBucketName(), meta.getKey());
			}
		}
	}
	
	@Test
	public void testUploadToS3BufferEqualsFileSize() throws IOException, NoSuchAlgorithmException{
		// Test the case where the buffer size and the file size are the same.
		int bufferSize = 1048576*5;
		int fileSize = bufferSize+100;
		byte[] bytes = new byte[fileSize];
		byte b = 123;
		// Fill it with data
		Arrays.fill(bytes, b);
		// calculate the expected MD5;
		String expectedMD5 = BinaryUtils.toHex((MessageDigest.getInstance("MD5").digest(bytes)));
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		// Create the metadata
		S3FileMetadata metadata = FileUploadManagerImpl.createMetadata(Mimetypes.MIMETYPE_OCTET_STREAM, "123", "testUploadToS3BufferEqualsFileSize");
		// Now do the upload
		long start = System.currentTimeMillis();
		fileUploadManager.uploadFileAsMultipart(metadata, bais, bufferSize);
		long elapse = System.currentTimeMillis() - start;
		System.out.println("Uploaded "+fileSize+" bytes in "+elapse+" MS");
		toDelete.add(metadata);
		// Was the MD5 Filled in correctly?
		assertEquals(expectedMD5, metadata.getContentMd5());
		assertEquals(new Long(fileSize), metadata.getContentSize());
		// Make sure the file is in S3
		S3Object result = s3Client.getObject(metadata.getBucketName(), metadata.getKey());
		assertNotNull(result);
		assertEquals(result.getObjectMetadata().getContentLength(), fileSize);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// Read it back
		S3ObjectInputStream s3In = result.getObjectContent();
		try{
			byte[] buffer = new byte[1024];
			int read = -1;
			while((read = s3In.read(buffer)) > 0){
				baos.write(buffer, 0, read);
			}
			// Do a deep equals
			assertTrue(Arrays.equals(bytes, baos.toByteArray()));
		}finally{
			s3In.close();
		}
	}
	
	@Test
	public void testUploadToS3BufferLessThanFileSize() throws IOException, NoSuchAlgorithmException{
		// Test the case where the buffer size and the file size are the same.
		int oneMegabyte = (int) Math.pow(2, 20);
		int fileSize = oneMegabyte*3+123;
		int bufferSize = oneMegabyte;
		byte[] bytes = new byte[fileSize];
		byte b = 102;
		// Fill it with data
		Arrays.fill(bytes, b);
		// calculate the expected MD5;
		String expectedMD5 = BinaryUtils.toHex((MessageDigest.getInstance("MD5").digest(bytes)));
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		// Create the metadata
		S3FileMetadata metadata = FileUploadManagerImpl.createMetadata(Mimetypes.MIMETYPE_OCTET_STREAM, "123", "testUploadToS3BufferLessThanFileSize");
		// Now do the upload
		long start = System.currentTimeMillis();
		fileUploadManager.uploadFileAsMultipart(metadata, bais, bufferSize);
		long elapse = System.currentTimeMillis() - start;
		System.out.println("Uploaded "+fileSize+" bytes in "+elapse+" MS");
		toDelete.add(metadata);
		// Was the MD5 Filled in correctly?
		assertEquals(expectedMD5, metadata.getContentMd5());
		assertEquals(new Long(fileSize), metadata.getContentSize());
		// Make sure the file is in S3
		S3Object result = s3Client.getObject(metadata.getBucketName(), metadata.getKey());
		assertNotNull(result);
		assertEquals(result.getObjectMetadata().getContentLength(), fileSize);
		byte[] returnBuffer = new byte[fileSize];
		// Read it back
		S3ObjectInputStream s3In = result.getObjectContent();
		try{
			int read = result.getObjectContent().read(returnBuffer);
			assertEquals(fileSize, read);
			// Do a deep equals
			assertTrue(Arrays.equals(bytes, returnBuffer));
		}finally{
			s3In.close();
		}
	}

}
