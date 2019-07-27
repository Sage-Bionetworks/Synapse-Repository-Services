package org.sagebionetworks.repo.manager.file.preview;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.util.ResourceTracker;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.util.FileProvider;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;

@RunWith(MockitoJUnitRunner.class)
public class PreviewManagerImplTest {
	
	FileHandleDao stubFileMetadataDao;
	@Mock
	private SynapseS3Client mockS3Client;
	@Mock
	private SynapseGoogleCloudStorageClient mockGoogleCloudClient;
	@Mock
	private FileProvider mockFileProvider;
	@Mock
	private PreviewGenerator mockPreviewGenerator;
	@Mock
	private File mockUploadFile;
	@Mock
	private S3Object mockS3Object;
	@Mock
	private Blob mockBlob;
	@Mock
	private ReadChannel mockGoogleCloudReadChannel;
	@Mock
	private FileOutputStream mockOutputStream;
	@Mock
	private S3ObjectInputStream mockS3ObjectInputStream;
	@Mock
	private IdGenerator mockIdGenerator;

	PreviewManagerImpl previewManager;

	Long maxPreviewSize = 100l;
	String testContentType = "text/plain";
	PreviewOutputMetadata previewContentType = new PreviewOutputMetadata("application/zip", ".zip");
	S3FileHandle testMetadata;
	GoogleCloudFileHandle testGoogleCloudMetadata;
	Long resultPreviewSize = 15l;
	
	@Before
	public void before() throws IOException{
		stubFileMetadataDao = new StubFileMetadataDao();
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
		when(mockGoogleCloudClient.getObject(any(String.class), any(String.class))).thenReturn(mockBlob);
		when(mockBlob.reader()).thenReturn(mockGoogleCloudReadChannel);
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(testContentType, maxPreviewSize + 1)).thenReturn(maxPreviewSize + 1);
		when(mockPreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		List<PreviewGenerator> genList = new LinkedList<PreviewGenerator>();
		genList.add(mockPreviewGenerator);
		
		previewManager = new PreviewManagerImpl(stubFileMetadataDao, mockS3Client, mockGoogleCloudClient, mockFileProvider, genList, maxPreviewSize);

		// This is a test file metadata
		testMetadata = TestUtils.createS3FileHandle("createdBy", null);
		testMetadata.setContentType(testContentType);
		testMetadata.setContentSize(10L);

		testGoogleCloudMetadata = TestUtils.createGoogleCloudFileHandle("createdBy", null);
		testGoogleCloudMetadata.setContentType(testContentType);
		testGoogleCloudMetadata.setContentSize(10L);

		// Add this to the stub
		testMetadata = (S3FileHandle) stubFileMetadataDao.createFile(testMetadata);
		testGoogleCloudMetadata = (GoogleCloudFileHandle) stubFileMetadataDao.createFile(testGoogleCloudMetadata);

		ReflectionTestUtils.setField(previewManager, "idGenerator", mockIdGenerator);
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(789L);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testMetadataNull() throws Exception{
		previewManager.generatePreview(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testContentTypeNullNull() throws Exception{
		testMetadata.setContentType(null);
		previewManager.generatePreview(testMetadata);
	}
	
	@Test
	public void testContentTypeEmpty() throws Exception {
		testMetadata.setContentType("");
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertNull(pfm);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testContentSizelNull() throws Exception{
		testMetadata.setContentSize(null);
		previewManager.generatePreview(testMetadata);
	}
	
	@Test
	public void testUnsupportedType() throws Exception{
		// Set to an unsupported content type;
		testMetadata.setContentType("fake/type");
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertTrue(pfm == null);
	}
	
	@Test
	public void testContentSizeTooLarge() throws Exception{
		// set the file size to be one byte too large.
		long size = maxPreviewSize + 1;
		testMetadata.setContentSize(size);
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertNull(pfm);
	}

	@Test
	public void testContentSizeNoTooLarge() throws Exception {
		// set the file size to be one byte too large.
		long size = maxPreviewSize;
		testMetadata.setContentSize(size);
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertNotNull(pfm);
	}

	@Test (expected=TemporarilyUnavailableException.class)
	public void testTemporarilyUnavailable() throws Exception{
		// Simulate a TemporarilyUnavailable exception.
		previewManager.resourceTracker = Mockito.mock(ResourceTracker.class);
		when(previewManager.resourceTracker.allocateAndUseResources(any(Callable.class), any(Long.class))).thenThrow(new TemporarilyUnavailableException());
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertTrue(pfm == null);
	}

	@Test
	public void testExceedsMaximumResources() throws Exception{
		// Simulate a ExceedsMaximumResources exception.
		previewManager.resourceTracker = Mockito.mock(ResourceTracker.class);
		when(previewManager.resourceTracker.allocateAndUseResources(any(Callable.class), any(Long.class))).thenThrow(new ExceedsMaximumResources());
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertTrue(pfm == null);
	}
	
	@Test
	public void testStreamsClosed() throws Exception{
		// Simulate an S3 exception.  The streams must be closed even when there is an error
		when(mockS3Client.putObject(any(PutObjectRequest.class))).thenThrow(new RuntimeException("Something went wrong!"));
		try{
			previewManager.generatePreview(testMetadata);
			fail("RuntimeException should have been thrown");
		}catch(RuntimeException e){
			// expected
		}
		// Validate the streams were closed
		verify(mockOutputStream, atLeast(1)).close();
		verify(mockS3ObjectInputStream, atLeast(1)).abort();
	}

	@Test
	public void testTempFilesDeleted() throws Exception{
		// Simulate an S3 exception.  The temp files must be deleted.
		when(mockS3Client.putObject(any(PutObjectRequest.class))).thenThrow(new RuntimeException("Something went wrong!"));
		try{
			CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
			fail("RuntimeException should have been thrown");
		}catch(RuntimeException e){
			// expected
		}
		// Validate the temp files were deleted
		verify(mockUploadFile, atLeast(1)).delete();
	}
	
	@Test
	public void testExpectedS3Preview() throws Exception{
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertNotNull(pfm);
		assertNotNull(pfm.getId());
		assertEquals(previewContentType.getContentType(), pfm.getContentType());
		assertEquals(testMetadata.getCreatedBy(), pfm.getCreatedBy());
		assertNotNull(pfm.getCreatedOn());
		assertEquals("preview"+previewContentType.getExtension(), pfm.getFileName());
		assertEquals(resultPreviewSize, pfm.getContentSize());
		// Make sure the preview is in the dao
		CloudProviderFileHandleInterface fromDao = (CloudProviderFileHandleInterface) stubFileMetadataDao.get(pfm.getId());
		assertEquals(pfm, fromDao);
	}

	@Test
	public void testExpectedGoogleCloudPreview() throws Exception {
		when(mockPreviewGenerator.generatePreview(any(InputStream.class), eq(mockOutputStream))).thenReturn(previewContentType);

		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testGoogleCloudMetadata);
		assertNotNull(pfm);
		assertNotNull(pfm.getId());
		assertEquals(previewContentType.getContentType(), pfm.getContentType());
		assertEquals(testGoogleCloudMetadata.getCreatedBy(), pfm.getCreatedBy());
		assertNotNull(pfm.getCreatedOn());
		assertEquals("preview"+previewContentType.getExtension(), pfm.getFileName());
		assertEquals(resultPreviewSize, pfm.getContentSize());
		// Make sure the preview is in the dao
		CloudProviderFileHandleInterface fromDao = (CloudProviderFileHandleInterface) stubFileMetadataDao.get(pfm.getId());
		assertEquals(pfm, fromDao);
	}

}
