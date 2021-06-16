package org.sagebionetworks.repo.manager.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;

import com.amazonaws.services.s3.model.GetObjectRequest;

@ExtendWith(MockitoExtension.class)
public class FileHandleSupportImplTest {
	
	@Mock
	private FileHandleDao mockFileHandleDao;
	@Mock
	private SynapseS3Client mockS3client;
	@Mock
	private FileHandleAuthorizationManager mockFileHandleAuthorizationManager;
	@Mock
	private FileHandleManager mockFileHandleManager;
	
	@InjectMocks
	private FileHandleSupportImpl fileHandleSupport;
	
	
	@BeforeEach
	public void before(){

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
			one = fileHandleSupport.createTempFile("One", ".txt");
			oneOut = new FileOutputStream(one);
			IOUtils.write(oneContents, oneOut);
			oneOut.close();
			// create two
			two = fileHandleSupport.createTempFile("two", ".txt");
			twoOut = new FileOutputStream(two);
			IOUtils.write(twoContents, twoOut);
			// The output zip
			zip = fileHandleSupport.createTempFile("Zip", ".zip");
			zipOut = fileHandleSupport.createZipOutputStream(zip);
			
			// add the files to the zip.
			String entryNameOne = "p1/One.txt";
			fileHandleSupport.addFileToZip(zipOut, one, entryNameOne);
			String entryNameTwo = "p2/Two.txt";
			fileHandleSupport.addFileToZip(zipOut, two, entryNameTwo);
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
		S3FileHandle result = fileHandleSupport.getS3FileHandle(fileHandleId);
		assertEquals(s3Handle, result);
	}
	
	@Test
	public void testGetS3FileHandleNotS3(){
		String fileHandleId = "123";
		ExternalFileHandle handle = new ExternalFileHandle();
		handle.setId(fileHandleId);
		when(mockFileHandleDao.get(fileHandleId)).thenReturn(handle);
		assertThrows(IllegalArgumentException.class, ()->{
			fileHandleSupport.getS3FileHandle(fileHandleId);
		});
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
			result = fileHandleSupport.downloadToTempFile(s3Handle);
			assertNotNull(result);
			verify(mockS3client).getObject(any(GetObjectRequest.class), any(File.class));
		}finally{
			if(result != null){
				result.delete();
			}
		}
	}
	
	@Test
	public void test() {
		
	}
}
