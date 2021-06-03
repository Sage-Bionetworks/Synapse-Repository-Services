package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.aws.SynapseS3Client;
import org.sagebionetworks.googlecloud.SynapseGoogleCloudStorageClient;
import org.sagebionetworks.ids.IdGenerator;
import org.sagebionetworks.ids.IdType;
import org.sagebionetworks.repo.model.StorageLocationDAO;
import org.sagebionetworks.repo.model.dbo.dao.TestUtils;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.CloudProviderFileHandleInterface;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.util.ResourceTracker;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.util.FileProvider;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.StorageClass;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;

@ExtendWith(MockitoExtension.class)
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
	@Mock
	private StorageLocationDAO mockStorageLocationDao;

	PreviewManagerImpl previewManager;

	Long maxPreviewSize = 100l;
	String testContentType = "text/plain";
	PreviewOutputMetadata previewContentType = new PreviewOutputMetadata("application/zip", ".zip");
	S3FileHandle testMetadata;
	GoogleCloudFileHandle testGoogleCloudMetadata;
	Long resultPreviewSize = 15l;
	
	@BeforeEach
	public void before() throws IOException{
		stubFileMetadataDao = new StubFileMetadataDao();
		
		List<PreviewGenerator> genList = Collections.singletonList(mockPreviewGenerator);
		
		previewManager = new PreviewManagerImpl(stubFileMetadataDao, mockS3Client, mockGoogleCloudClient, mockFileProvider, mockIdGenerator, mockStorageLocationDao, genList, maxPreviewSize);

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
				
	}
	
	@Test
	public void testMetadataNull() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {			
			previewManager.generatePreview(null);
		});
		
	}
	
	@Test
	public void testContentTypeNullNull() throws Exception{
		testMetadata.setContentType(null);
		
		assertThrows(IllegalArgumentException.class, () -> {	
			previewManager.generatePreview(testMetadata);
		});
	}
	
	@Test
	public void testContentTypeEmpty() throws Exception {
		testMetadata.setContentType("");
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertNull(pfm);
	}

	@Test
	public void testContentSizelNull() throws Exception{
		testMetadata.setContentSize(null);
		
		assertThrows(IllegalArgumentException.class, () -> {	
			previewManager.generatePreview(testMetadata);
		});
	}
	
	@Test
	public void testUnsupportedType() throws Exception{
		// Set to an unsupported content type;
		testMetadata.setContentType("fake/type");
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertNull(pfm);
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
	public void testContentSizeNotTooLarge() throws Exception {
		long size = maxPreviewSize;
		testMetadata.setContentSize(size);
		
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		when(mockPreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(789L);
		
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertNotNull(pfm);
	}

	@Test
	public void testTemporarilyUnavailable() throws Exception{
		
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		
		// Simulate a TemporarilyUnavailable exception.
		previewManager.resourceTracker = Mockito.mock(ResourceTracker.class);
		when(previewManager.resourceTracker.allocateAndUseResources(any(), anyLong())).thenThrow(new TemporarilyUnavailableException());
		
		
		assertThrows(TemporarilyUnavailableException.class, () -> {			
			previewManager.generatePreview(testMetadata);
		});
	}
	
	@Test
	public void testExceedsMaximumResources() throws Exception{

		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		
		// Simulate a ExceedsMaximumResources exception.
		previewManager.resourceTracker = Mockito.mock(ResourceTracker.class);
		when(previewManager.resourceTracker.allocateAndUseResources(any(), anyLong())).thenThrow(new ExceedsMaximumResources());
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertNull(pfm);
	}
	
	@Test
	public void testStreamsClosed() throws Exception{
		
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		when(mockPreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		
		// Simulate an S3 exception.  The streams must be closed even when there is an error
		when(mockS3Client.putObject(any(PutObjectRequest.class))).thenThrow(new RuntimeException("Something went wrong!"));
		
		assertThrows(RuntimeException.class, () -> {
			previewManager.generatePreview(testMetadata);
		});
		
		// Validate the streams were closed
		verify(mockOutputStream, atLeast(1)).close();
		verify(mockS3ObjectInputStream, atLeast(1)).abort();
	}

	@Test
	public void testTempFilesDeleted() throws Exception{
		
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		when(mockPreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		
		// Simulate an S3 exception.  The temp files must be deleted.
		when(mockS3Client.putObject(any(PutObjectRequest.class))).thenThrow(new RuntimeException("Something went wrong!"));
		
		assertThrows(RuntimeException.class, () -> {
			previewManager.generatePreview(testMetadata);
		});
		
		// Validate the temp files were deleted
		verify(mockUploadFile, atLeast(1)).delete();
	}
	
	@Test
	public void testExpectedS3Preview() throws Exception{
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		when(mockPreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(789L);
		
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
	public void testExpectedS3PreviewWithS3StorageLocation() throws Exception {
		
		testMetadata.setStorageLocationId(123L);
		
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		when(mockPreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(789L);
		when(mockStorageLocationDao.get(any())).thenReturn(new S3StorageLocationSetting());
		
		// Call under test
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		
		assertNotNull(pfm);
		
		ArgumentCaptor<PutObjectRequest> putRequestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
		
		verify(mockStorageLocationDao).get(123L);
		verify(mockS3Client).putObject(putRequestCaptor.capture());
		
		assertEquals(StorageClass.IntelligentTiering.toString(), putRequestCaptor.getValue().getStorageClass());
	}

	@Test
	public void testExpectedGoogleCloudPreview() throws Exception {
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockGoogleCloudClient.getObject(any(String.class), any(String.class))).thenReturn(mockBlob);
		when(mockBlob.reader()).thenReturn(mockGoogleCloudReadChannel);
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		when(mockPreviewGenerator.generatePreview(any(InputStream.class), eq(mockOutputStream))).thenReturn(previewContentType);		
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(789L);
		

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
