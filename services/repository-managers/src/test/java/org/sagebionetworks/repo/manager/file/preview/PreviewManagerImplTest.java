package org.sagebionetworks.repo.manager.file.preview;


import static org.junit.Assert.*;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.util.ResourceTracker;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.util.TempFileProvider;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class PreviewManagerImplTest {
	
	PreviewManagerImpl previewManager;
	FileHandleDao stubFileMetadataDao;
	AmazonS3Client mockS3Client;
	TempFileProvider mockFileProvider;
	PreviewGenerator mockPreviewGenerator;
	File mockDownloadFile;
	File mockUploadFile;
	FileOutputStream mockOutputStream;
	FileInputStream mockInputStream;
	Long maxPreviewSize = 100l;
	float multiplerForContentType = 1.5f;
	String testContentType = "text/plain";
	String previewContentType = "application/zip";
	S3FileHandle testMetadata;
	Long resultPreviewSize = 15l;
	
	@Before
	public void before() throws IOException{
		stubFileMetadataDao = new StubFileMetadataDao();
		mockS3Client = Mockito.mock(AmazonS3Client.class);
		mockFileProvider = Mockito.mock(TempFileProvider.class);
		mockPreviewGenerator = Mockito.mock(PreviewGenerator.class);
		mockDownloadFile = Mockito.mock(File.class);
		mockUploadFile = Mockito.mock(File.class);
		mockOutputStream = Mockito.mock(FileOutputStream.class);
		mockInputStream = Mockito.mock(FileInputStream.class);
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockDownloadFile, mockUploadFile);
		when(mockFileProvider.createFileInputStream(mockDownloadFile)).thenReturn(mockInputStream);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockPreviewGenerator.supportsContentType(testContentType)).thenReturn(true);
		when(mockPreviewGenerator.getMemoryMultiplierForContentType(testContentType)).thenReturn(multiplerForContentType);
		when(mockPreviewGenerator.generatePreview(mockInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		List<PreviewGenerator> genList = new LinkedList<PreviewGenerator>();
		genList.add(mockPreviewGenerator);
		Map<String, String> contentType2FileExtension = new HashMap<String, String>();
		contentType2FileExtension.put("text/csv", ".csv");	//usually populated via injection (see managers-spb.xml)
		
		Set<String> codeFileExtensions = new HashSet<String>();
		codeFileExtensions.add(".r");
		codeFileExtensions.add(".java");
		previewManager = new PreviewManagerImpl(stubFileMetadataDao, mockS3Client, mockFileProvider, genList, maxPreviewSize, contentType2FileExtension, codeFileExtensions);
		
		// This is a test file metadata
		testMetadata = new S3FileHandle();
		testMetadata.setBucketName("bucketName");
		testMetadata.setContentType(testContentType);
		testMetadata.setContentMd5("contentMD5");
		testMetadata.setContentSize(10l);
		testMetadata.setCreatedBy("createdBy");
		testMetadata.setEtag("etag");
		testMetadata.setFileName("fileName");
		testMetadata.setKey("key");
		// Add this to the stub
		testMetadata = stubFileMetadataDao.createFile(testMetadata);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testMetadataNull() throws Exception{
		PreviewFileHandle pfm = previewManager.generatePreview(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testContentTypeNullNull() throws Exception{
		testMetadata.setContentType(null);
		PreviewFileHandle pfm = previewManager.generatePreview(testMetadata);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testContentSizelNull() throws Exception{
		testMetadata.setContentSize(null);
		PreviewFileHandle pfm = previewManager.generatePreview(testMetadata);
	}
	
	@Test
	public void testUnsupportedType() throws Exception{
		// Set to an unsupported content type;
		testMetadata.setContentType("fake/type");
		PreviewFileHandle pfm = previewManager.generatePreview(testMetadata);
		assertTrue(pfm == null);
	}
	
	@Test
	public void testContentSizeTooLarge() throws Exception{
		// set the file size to be one byte too large.
		long size = ((long) ((maxPreviewSize+1)/multiplerForContentType));
		testMetadata.setContentSize(size);
		PreviewFileHandle pfm = previewManager.generatePreview(testMetadata);
		assertTrue(pfm == null);
	}
	
	@Test (expected=TemporarilyUnavailableException.class)
	public void testTemporarilyUnavailable() throws Exception{
		// Simulate a TemporarilyUnavailable exception.
		previewManager.resourceTracker = Mockito.mock(ResourceTracker.class);
		when(previewManager.resourceTracker.allocateAndUseResources(any(Callable.class), any(Long.class))).thenThrow(new TemporarilyUnavailableException());
		PreviewFileHandle pfm = previewManager.generatePreview(testMetadata);
		assertTrue(pfm == null);
	}

	@Test
	public void testExceedsMaximumResources() throws Exception{
		// Simulate a ExceedsMaximumResources exception.
		previewManager.resourceTracker = Mockito.mock(ResourceTracker.class);
		when(previewManager.resourceTracker.allocateAndUseResources(any(Callable.class), any(Long.class))).thenThrow(new ExceedsMaximumResources());
		PreviewFileHandle pfm = previewManager.generatePreview(testMetadata);
		assertTrue(pfm == null);
	}
	
	@Test
	public void testStreamsClosed() throws Exception{
		// Simulate an S3 exception.  The streams must be closed even when there is an error
		when(mockS3Client.putObject(any(PutObjectRequest.class))).thenThrow(new RuntimeException("Something went wrong!"));
		try{
			PreviewFileHandle pfm = previewManager.generatePreview(testMetadata);
			fail("RuntimeException should have been thrown");
		}catch(RuntimeException e){
			// expected
		}
		// Validate the streams were closed
		verify(mockOutputStream, atLeast(1)).close();
		verify(mockInputStream, atLeast(1)).close();
	}

	@Test
	public void testTempFilesDeleted() throws Exception{
		// Simulate an S3 exception.  The temp files must be deleted.
		when(mockS3Client.putObject(any(PutObjectRequest.class))).thenThrow(new RuntimeException("Something went wrong!"));
		try{
			PreviewFileHandle pfm = previewManager.generatePreview(testMetadata);
			fail("RuntimeException should have been thrown");
		}catch(RuntimeException e){
			// expected
		}
		// Validate the streams were closed
		verify(mockDownloadFile, atLeast(1)).delete();
		verify(mockUploadFile, atLeast(1)).delete();
	}
	
	@Test
	public void testExpectedPreview() throws Exception{
		PreviewFileHandle pfm = previewManager.generatePreview(testMetadata);
		assertNotNull(pfm);
		assertNotNull(pfm.getId());
		assertEquals(previewContentType, pfm.getContentType());
		assertEquals(testMetadata.getCreatedBy(), pfm.getCreatedBy());
		assertNotNull(pfm.getCreatedOn());
		assertEquals(testMetadata.getFileName(), pfm.getFileName());
		assertEquals(resultPreviewSize, pfm.getContentSize());
		// Make sure the preview is in the dao
		PreviewFileHandle fromDao = (PreviewFileHandle) stubFileMetadataDao.get(pfm.getId());
		assertEquals(pfm, fromDao);
	}
	
	@Test
	public void testFilenameBasedOnContentType() throws Exception{
		String testFilename = "myFile.zip";
		String previewGeneratorOutputContentType = "text/csv";
		String outputFilename = previewManager.getFilenameBasedOnContentType(testFilename, previewGeneratorOutputContentType);
		assertTrue(outputFilename.endsWith(".csv"));
		
		testFilename = "myFile.txt";
		previewGeneratorOutputContentType = "text/plain";
		outputFilename = previewManager.getFilenameBasedOnContentType(testFilename, previewGeneratorOutputContentType);
		assertTrue(outputFilename.endsWith(".txt"));
	}
	
	@Test
	public void testFindPreviewGenerator() throws Exception {
		List<PreviewGenerator> genList = new LinkedList<PreviewGenerator>();
		PreviewGenerator defaultGenerator = new TextPreviewGenerator();
		PreviewGenerator imagePreviewGenerator = new ImagePreviewGenerator();
		genList.add(imagePreviewGenerator);
		genList.add(defaultGenerator);
		
		previewManager.setGeneratorList(genList);
		//if it doesn't look like a code file, generator should be null
		PreviewGenerator gen = previewManager.findPreviewGenerator("application/octet-stream", "myBinaryFile.exe");
		assertNull(gen);
		//if the content type says that it's an image file, it should pick the image preview (and not use the filename)
		gen = previewManager.findPreviewGenerator("image/png", "myCodeFile.r");
		assertEquals(imagePreviewGenerator, gen);
		
		//expected standard case
		gen = previewManager.findPreviewGenerator("application/octet-stream", "myCodeFile.r");
		assertEquals(defaultGenerator, gen);
		
		//case shouldn't matter
		gen = previewManager.findPreviewGenerator("application/octet-stream", "myCodeFile.R");
		assertEquals(defaultGenerator, gen);
	}
}
