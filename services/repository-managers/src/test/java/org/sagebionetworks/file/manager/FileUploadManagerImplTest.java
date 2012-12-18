package org.sagebionetworks.file.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.file.FileUploadManagerImpl;
import org.sagebionetworks.repo.manager.file.FileUploadResults;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dao.FileMetadataDao;
import org.sagebionetworks.repo.model.file.S3FileMetadata;
import org.sagebionetworks.repo.web.ServiceUnavailableException;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.internal.Mimetypes;
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
	
	
	@Before
	public void before() throws UnsupportedEncodingException, IOException{
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
		when(mockFileStream.isFormField()).thenReturn(false);
		when(mockFileStream.openStream()).thenReturn(new StringInputStream("I am a very short string"));
		when(mockFileStream.getContentType()).thenReturn("text/plain");
		when(mockFileStream.getName()).thenReturn("someTextFile.txt");
		when(mockFileStream.getFieldName()).thenReturn("file");
		// Mock a parameter as a stream.
		mockParamParentIdStream = Mockito.mock(FileItemStream.class);
		// This file stream is actually a file and not a parameter
		when(mockParamParentIdStream.isFormField()).thenReturn(true);
		when(mockParamParentIdStream.openStream()).thenReturn(new StringInputStream("syn123"));
		when(mockParamParentIdStream.getContentType()).thenReturn(null);
		when(mockParamParentIdStream.getName()).thenReturn(null);
		when(mockParamParentIdStream.getFieldName()).thenReturn("parentId");
		
		// the manager to test.
		manager = new FileUploadManagerImpl(mockfileMetadataDao);
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
		manager.uploadfiles(mockUser, expectedParams, mockIterator, 1l);
	}
	
	/**
	 * For this test all of the expected parameters are provided.
	 * @throws FileUploadException
	 * @throws IOException
	 */
	@Test
	public void testUploadfilesWithExpectedParams() throws FileUploadException, IOException{
		// These are the parameters we expect to find before reading a file
		HashSet<String> expectedParams = new HashSet<String>();
		expectedParams.add("parentId");
		// For this test the parent id parameter is missing
		when(mockIterator.hasNext()).thenReturn(true, true, false);
		// return the parentId param before the file.
		when(mockIterator.next()).thenReturn(mockParamParentIdStream, mockFileStream);
		FileUploadResults results = manager.uploadfiles(mockUser, expectedParams, mockIterator, 1l);
		assertNotNull(results);
		assertNotNull(results.getParameters());
		assertNotNull(results.getFiles());
		// Was parentId extracted as epected?
		assertEquals("syn123", results.getParameters().get("parentId"));
		// Do we have one file
		assertEquals(1, results.getFiles().size());
		S3FileMetadata fileData = results.getFiles().get(0);
		assertEquals("text/plain", fileData.getContentType());
		assertNotNull(fileData.getKey());
		assertNotNull(fileData.getBucketName());
		assertTrue(fileData.getKey().endsWith("someTextFile.txt"));
		assertTrue(fileData.getKey().startsWith("987/"));
	}
	
	@Test
	public void testCreateMetadata(){
		// Create the metadata
		S3FileMetadata metadata = FileUploadManagerImpl.createRequest(Mimetypes.MIMETYPE_OCTET_STREAM, "123", "testCreateMetadata");
		assertNotNull(metadata);
		assertEquals(StackConfiguration.getS3Bucket(), metadata.getBucketName());
		assertEquals(Mimetypes.MIMETYPE_OCTET_STREAM, metadata.getContentType());
		assertEquals("123", metadata.getCreatedBy());
		assertNotNull(metadata.getKey());
		assertTrue(metadata.getKey().endsWith("testCreateMetadata"));
		assertTrue(metadata.getKey().startsWith("123/"));
	}

}
