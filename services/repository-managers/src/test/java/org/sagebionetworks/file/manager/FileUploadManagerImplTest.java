package org.sagebionetworks.file.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.file.FileUploadManagerImpl;
import org.sagebionetworks.repo.manager.file.FileUploadResults;
import org.sagebionetworks.repo.manager.file.transfer.FileTransferStrategy;
import org.sagebionetworks.repo.manager.file.transfer.TransferRequest;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

import com.amazonaws.services.s3.internal.Mimetypes;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.StringInputStream;

/**
 * A unit test for the FileUploadManagerImpl.
 * 
 * @author jmhill
 *
 */
public class FileUploadManagerImplTest {
	
	FileUploadManagerImpl manager;
	FileItemIterator mockIterator;
	FileItemStream mockFileStream;
	FileItemStream mockParamParentIdStream;
	UserInfo mockUser;
	FileMetadataDao mockfileMetadataDao;
	FileTransferStrategy mockPrimaryStrategy;
	FileTransferStrategy mockFallbackStrategy;
	S3FileMetadata validResults;
	
	
	@Before
	public void before() throws UnsupportedEncodingException, IOException, NoSuchAlgorithmException{
		mockIterator = Mockito.mock(FileItemIterator.class);
		mockfileMetadataDao = Mockito.mock(FileMetadataDao.class);
		
		// The user is not really a mock
		mockUser = new UserInfo(false);
		mockUser.setUser(new User());
		mockUser.setIndividualGroup(new UserGroup());
		mockUser.getIndividualGroup().setId("987");
		
		// Other helper mocks
		// First mock a file stream
		mockFileStream = Mockito.mock(FileItemStream.class);
		// This file stream is actually a file and not a parameter
		String contentType = "text/plain";
		String fileName = "someTextFile.txt";
		String contentString = "I am a very short string";
		byte[] contentBytes = contentString.getBytes();
		String contentMD5 = BinaryUtils.toHex((MessageDigest.getInstance("MD5").digest(contentBytes)));
		
		when(mockFileStream.isFormField()).thenReturn(false);
		when(mockFileStream.openStream()).thenReturn(new StringInputStream(contentString));
		when(mockFileStream.getContentType()).thenReturn(contentType);
		when(mockFileStream.getName()).thenReturn(fileName);
		when(mockFileStream.getFieldName()).thenReturn("file");
		// Mock a parameter as a stream.
		mockParamParentIdStream = Mockito.mock(FileItemStream.class);
		// This file stream is actually a file and not a parameter
		when(mockParamParentIdStream.isFormField()).thenReturn(true);
		when(mockParamParentIdStream.openStream()).thenReturn(new StringInputStream("syn123"));
		when(mockParamParentIdStream.getContentType()).thenReturn(null);
		when(mockParamParentIdStream.getName()).thenReturn(null);
		when(mockParamParentIdStream.getFieldName()).thenReturn("parentId");
		
		mockPrimaryStrategy = Mockito.mock(FileTransferStrategy.class);
		mockFallbackStrategy = Mockito.mock(FileTransferStrategy.class);
		// setup the primary to succeed
		validResults = new S3FileMetadata();
		validResults.setId("123");
		validResults.setCreatedBy(mockUser.getIndividualGroup().getId());
		validResults.setCreatedOn(new Date());
		validResults.setContentType(contentType);
		validResults.setContentSize(new Long(contentBytes.length));
		validResults.setContentMd5(contentMD5);
		validResults.setFileName(fileName);
		validResults.setBucketName("bucket");
		validResults.setKey("key");
		
		// the manager to test.
		manager = new FileUploadManagerImpl(mockfileMetadataDao, mockPrimaryStrategy, mockFallbackStrategy);
	}
	
	/**
	 * Test that finding a file stream without the required parameters triggers an exception.
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws ServiceUnavailableException 
	 */
	@Test (expected=IllegalArgumentException.class)
	public void testUploadfilesMissingExpectedParams() throws FileUploadException, IOException, ServiceUnavailableException{
		// These are the parameters we expect to find before reading a file
		HashSet<String> expectedParams = new HashSet<String>();
		expectedParams.add("parentId");
		// For this test the parent id parameter is missing
		when(mockIterator.hasNext()).thenReturn(true, false);
		when(mockIterator.next()).thenReturn(mockFileStream);
		manager.uploadfiles(mockUser, expectedParams, mockIterator);
	}
	
	/**
	 * For this test all of the expected parameters are provided, and the primary strategy works.
	 * @throws FileUploadException
	 * @throws IOException
	 * @throws ServiceUnavailableException 
	 */
	@Test
	public void testUploadfilesWithExpectedParams() throws FileUploadException, IOException, ServiceUnavailableException{
		// These are the parameters we expect to find before reading a file
		HashSet<String> expectedParams = new HashSet<String>();
		expectedParams.add("parentId");
		// For this test the parent id parameter is missing
		when(mockIterator.hasNext()).thenReturn(true, true, false);
		// return the parentId param before the file.
		when(mockIterator.next()).thenReturn(mockParamParentIdStream, mockFileStream);
		// set the primary to fire.
		when(mockPrimaryStrategy.transferToS3(any(TransferRequest.class))).thenReturn(validResults);
		// Set the secondary to throw an exception
		when(mockFallbackStrategy.transferToS3(any(TransferRequest.class))).thenThrow(new ServiceUnavailableException());
		when(mockfileMetadataDao.createFile(validResults)).thenReturn(validResults);
		FileUploadResults results = manager.uploadfiles(mockUser, expectedParams, mockIterator);
		assertNotNull(results);
		assertNotNull(results.getParameters());
		assertNotNull(results.getFiles());
		// Was parentId extracted as expected?
		assertEquals("syn123", results.getParameters().get("parentId"));
		// Do we have one file
		assertEquals(1, results.getFiles().size());
		S3FileMetadata fileData = results.getFiles().get(0);
		assertEquals("text/plain", fileData.getContentType());
		assertNotNull(fileData.getKey());
		assertNotNull(fileData.getBucketName());
		
		verify(mockfileMetadataDao, times(1)).createFile(validResults);
		verify(mockPrimaryStrategy, times(1)).transferToS3(any(TransferRequest.class));
		// the fall back should not have been called.
		verify(mockFallbackStrategy, never()).transferToS3(any(TransferRequest.class));
	}
	
	@Test
	public void testFallbackStragegy() throws FileUploadException, IOException, ServiceUnavailableException{
		// These are the parameters we expect to find before reading a file
		HashSet<String> expectedParams = new HashSet<String>();
		when(mockIterator.hasNext()).thenReturn(true, false);
		// return the parentId param before the file.
		when(mockIterator.next()).thenReturn(mockFileStream);
		// Set the primary to fail.
		when(mockPrimaryStrategy.transferToS3(any(TransferRequest.class))).thenThrow(new ServiceUnavailableException());
		// Set the secondary to work
		when(mockFallbackStrategy.transferToS3(any(TransferRequest.class))).thenReturn(validResults);
		when(mockfileMetadataDao.createFile(validResults)).thenReturn(validResults);
		FileUploadResults results = manager.uploadfiles(mockUser, expectedParams, mockIterator);
		assertNotNull(results);
		assertNotNull(results.getParameters());
		assertNotNull(results.getFiles());
		// Do we have one file
		assertEquals(1, results.getFiles().size());
		S3FileMetadata fileData = results.getFiles().get(0);
		assertEquals("text/plain", fileData.getContentType());
		assertNotNull(fileData.getKey());
		assertNotNull(fileData.getBucketName());
		
		verify(mockfileMetadataDao, times(1)).createFile(validResults);
		verify(mockPrimaryStrategy, times(1)).transferToS3(any(TransferRequest.class));
		// the fall back should have been called this time
		verify(mockFallbackStrategy,times(1)).transferToS3(any(TransferRequest.class));
	}
	
	@Test (expected=ServiceUnavailableException.class)
	public void testBothStrategiesFailed() throws FileUploadException, IOException, ServiceUnavailableException{
		// These are the parameters we expect to find before reading a file
		HashSet<String> expectedParams = new HashSet<String>();
		when(mockIterator.hasNext()).thenReturn(true, false);
		// return the parentId param before the file.
		when(mockIterator.next()).thenReturn(mockFileStream);
		// Set the primary to fail.
		when(mockPrimaryStrategy.transferToS3(any(TransferRequest.class))).thenThrow(new ServiceUnavailableException());
		// Set the fallback to fail.
		when(mockFallbackStrategy.transferToS3(any(TransferRequest.class))).thenThrow(new ServiceUnavailableException());
		when(mockfileMetadataDao.createFile(validResults)).thenReturn(validResults);
		FileUploadResults results = manager.uploadfiles(mockUser, expectedParams, mockIterator);
	}
	@Test
	public void testCreateMetadata() throws UnsupportedEncodingException{
		// Create the metadata
		InputStream stream = new StringInputStream("stream");
		TransferRequest metadata = FileUploadManagerImpl.createRequest(Mimetypes.MIMETYPE_OCTET_STREAM, "123", "testCreateMetadata", stream);
		assertNotNull(metadata);
		assertEquals(StackConfiguration.getS3Bucket(), metadata.getS3bucketName());
		assertEquals(Mimetypes.MIMETYPE_OCTET_STREAM, metadata.getContentType());
		assertNotNull(metadata.getS3key());
		assertTrue(metadata.getS3key().endsWith("testCreateMetadata"));
		assertTrue(metadata.getS3key().startsWith("123/"));
		assertEquals(stream, metadata.getInputStream());
	}

}
