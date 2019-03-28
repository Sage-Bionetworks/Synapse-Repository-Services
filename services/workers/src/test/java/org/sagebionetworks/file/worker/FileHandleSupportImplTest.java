package org.sagebionetworks.file.worker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.manager.file.FileHandleAuthorizationManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.model.GetObjectRequest;

public class FileHandleSupportImplTest {
	
	FileHandleDao mockFileHandleDao;
	SynapseS3Client mockS3client;
	FileHandleAuthorizationManager mockFileHandleAuthorizationManager;
	FileHandleManager mockFileHandleManager;
	
	FileHandleSupport bulkDownloadDao;
	
	
	@Before
	public void before(){
		mockFileHandleDao = Mockito.mock(FileHandleDao.class);
		mockS3client = Mockito.mock(SynapseS3Client.class);
		mockFileHandleAuthorizationManager = Mockito.mock(FileHandleAuthorizationManager.class);
		mockFileHandleManager = Mockito.mock(FileHandleManager.class);
		
		bulkDownloadDao = new FileHandleSupportImpl();
		ReflectionTestUtils.setField(bulkDownloadDao, "fileHandleDao", mockFileHandleDao);
		ReflectionTestUtils.setField(bulkDownloadDao, "s3client", mockS3client);
		ReflectionTestUtils.setField(bulkDownloadDao, "fileHandleAuthorizationManager", mockFileHandleAuthorizationManager);
		ReflectionTestUtils.setField(bulkDownloadDao, "fileHandleManager", mockFileHandleManager);
	}
	
	@Test
	public void testZipRoundTrip() throws IOException{
		File one = null;
		File two = null;
		File zip = null;
		FileOutputStream oneOut = null;
		FileOutputStream twoOut = null;
		ZipOutputStream zipOut = null;
		ZipInputStream zipIn = null;
		try{
			String oneContents = "data for one";
			String twoContents = "data for two";
			// create one.
			one = bulkDownloadDao.createTempFile("One", ".txt");
			oneOut = new FileOutputStream(one);
			IOUtils.write(oneContents, oneOut);
			oneOut.close();
			// create two
			two = bulkDownloadDao.createTempFile("two", ".txt");
			twoOut = new FileOutputStream(two);
			IOUtils.write(twoContents, twoOut);
			// The output zip
			zip = bulkDownloadDao.createTempFile("Zip", ".zip");
			zipOut = bulkDownloadDao.createZipOutputStream(zip);
			
			// add the files to the zip.
			String entryNameOne = "p1/One.txt";
			bulkDownloadDao.addFileToZip(zipOut, one, entryNameOne);
			String entryNameTwo = "p2/Two.txt";
			bulkDownloadDao.addFileToZip(zipOut, two, entryNameTwo);
			zipOut.close();
			
			// unzip 
			zipIn = new ZipInputStream(new FileInputStream(zip));
			ZipEntry entry = zipIn.getNextEntry();
			assertEquals(entryNameOne, entry.getName());
			assertEquals(oneContents, IOUtils.toString(zipIn));
			zipIn.closeEntry();
			entry = zipIn.getNextEntry();
			assertEquals(entryNameTwo, entry.getName());
			assertEquals(twoContents, IOUtils.toString(zipIn));
			zipIn.closeEntry();
			
		}finally{
			IOUtils.closeQuietly(oneOut);
			IOUtils.closeQuietly(twoOut);
			IOUtils.closeQuietly(zipOut);
			IOUtils.closeQuietly(zipIn);
			if(one != null){
				one.delete();
			}
			if(two != null){
				two.delete();
			}
			if(zip != null){
				zip.delete();
			}
		}
	}
	
	@Test
	public void testGetS3FileHandle(){
		String fileHandleId = "123";
		S3FileHandle s3Handle = new S3FileHandle();
		s3Handle.setId(fileHandleId);
		
		when(mockFileHandleDao.get(fileHandleId)).thenReturn(s3Handle);
		S3FileHandle result = bulkDownloadDao.getS3FileHandle(fileHandleId);
		assertEquals(s3Handle, result);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testGetS3FileHandleNotS3(){
		String fileHandleId = "123";
		ExternalFileHandle handle = new ExternalFileHandle();
		handle.setId(fileHandleId);
		when(mockFileHandleDao.get(fileHandleId)).thenReturn(handle);
		bulkDownloadDao.getS3FileHandle(fileHandleId);
	}
	
	@Test
	public void testDownloadToTempFile() throws IOException{
		String fileHandleId = "123";
		S3FileHandle s3Handle = new S3FileHandle();
		s3Handle.setId(fileHandleId);
		s3Handle.setKey("someKey");
		s3Handle.setBucketName("someBucket");
		
		File result = null;
		try{
			result = bulkDownloadDao.downloadToTempFile(s3Handle);
			assertNotNull(result);
			verify(mockS3client).getObject(any(GetObjectRequest.class), any(File.class));
		}finally{
			if(result != null){
				result.delete();
			}
		}
	}
	
}
