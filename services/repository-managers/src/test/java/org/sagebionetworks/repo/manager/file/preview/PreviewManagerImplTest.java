package org.sagebionetworks.repo.manager.file.preview;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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
	PreviewOutputMetadata previewContentType = new PreviewOutputMetadata("application/zip", ".zip");
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
		
		previewManager = new PreviewManagerImpl(stubFileMetadataDao, mockS3Client, mockFileProvider, genList, maxPreviewSize);
		
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
	
	@Test
	public void testContentTypeEmpty() throws Exception {
		testMetadata.setContentType("");
		PreviewFileHandle pfm = previewManager.generatePreview(testMetadata);
		assertNull(pfm);
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
		assertEquals(previewContentType.getContentType(), pfm.getContentType());
		assertEquals(testMetadata.getCreatedBy(), pfm.getCreatedBy());
		assertNotNull(pfm.getCreatedOn());
		assertEquals("preview"+previewContentType.getExtension(), pfm.getFileName());
		assertEquals(resultPreviewSize, pfm.getContentSize());
		// Make sure the preview is in the dao
		PreviewFileHandle fromDao = (PreviewFileHandle) stubFileMetadataDao.get(pfm.getId());
		assertEquals(pfm, fromDao);
	}
}
