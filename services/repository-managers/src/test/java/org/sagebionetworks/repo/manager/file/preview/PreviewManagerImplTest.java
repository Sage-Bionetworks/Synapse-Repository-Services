package org.sagebionetworks.repo.manager.file.preview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
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
import org.sagebionetworks.repo.model.project.ExternalGoogleCloudStorageLocationSetting;
import org.sagebionetworks.repo.model.project.S3StorageLocationSetting;
import org.sagebionetworks.repo.util.ResourceTracker;
import org.sagebionetworks.repo.util.ResourceTracker.ExceedsMaximumResources;
import org.sagebionetworks.repo.web.TemporarilyUnavailableException;
import org.sagebionetworks.util.FileProvider;

import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
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
	@Mock
	private InitiateMultipartUploadResult mockMultipartResult;
	@Mock
	private UploadPartResult mockUploadPartResult;

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
		when(mockS3Client.initiateMultipartUpload(any())).thenReturn(mockMultipartResult);
		when(mockS3Client.uploadPart(any())).thenReturn(mockUploadPartResult);
		
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
		when(mockS3Client.initiateMultipartUpload(any())).thenThrow(new RuntimeException("Something went wrong!"));
		
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
		when(mockS3Client.initiateMultipartUpload(any())).thenThrow(new RuntimeException("Something went wrong!"));
		
		assertThrows(RuntimeException.class, () -> {
			previewManager.generatePreview(testMetadata);
		});
		
		// Validate the temp files were deleted
		verify(mockUploadFile, atLeast(1)).delete();
	}
	
	@Test
	public void testExpectedS3Preview() throws Exception{
		
		String uploadId = "uploadId";
		
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		when(mockPreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(789L);
		when(mockMultipartResult.getUploadId()).thenReturn(uploadId);
		when(mockS3Client.initiateMultipartUpload(any())).thenReturn(mockMultipartResult);
		when(mockS3Client.uploadPart(any())).thenReturn(mockUploadPartResult);
		
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertNotNull(pfm);
		assertNotNull(pfm.getId());
		assertEquals(previewContentType.getContentType(), pfm.getContentType());
		assertEquals(testMetadata.getCreatedBy(), pfm.getCreatedBy());
		assertNotNull(pfm.getCreatedOn());
		assertEquals("preview"+previewContentType.getExtension(), pfm.getFileName());
		assertEquals(resultPreviewSize, pfm.getContentSize());
		
		ArgumentCaptor<InitiateMultipartUploadRequest> initRequestCaptor = ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
		
		verify(mockS3Client).initiateMultipartUpload(initRequestCaptor.capture());
		
		assertEquals(pfm.getBucketName(), initRequestCaptor.getValue().getBucketName());
		assertEquals(pfm.getKey(), initRequestCaptor.getValue().getKey());
		assertEquals(CannedAccessControlList.BucketOwnerFullControl, initRequestCaptor.getValue().getCannedACL());
		
		ArgumentCaptor<UploadPartRequest> partRequestCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
		
		verify(mockS3Client).uploadPart(partRequestCaptor.capture());
		
		assertEquals(uploadId, partRequestCaptor.getValue().getUploadId());
		assertEquals(pfm.getBucketName(), partRequestCaptor.getValue().getBucketName());
		assertEquals(pfm.getKey(), partRequestCaptor.getValue().getKey());
		assertEquals(1, partRequestCaptor.getValue().getPartNumber());
		assertEquals(0, partRequestCaptor.getValue().getFileOffset());
		assertEquals(resultPreviewSize, partRequestCaptor.getValue().getPartSize());
		assertEquals(mockUploadFile, partRequestCaptor.getValue().getFile());
		
		ArgumentCaptor<CompleteMultipartUploadRequest> compRequestCaptor = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
		
		verify(mockS3Client).completeMultipartUpload(compRequestCaptor.capture());
		
		assertEquals(uploadId, compRequestCaptor.getValue().getUploadId());
		assertEquals(pfm.getBucketName(), compRequestCaptor.getValue().getBucketName());
		assertEquals(pfm.getKey(), compRequestCaptor.getValue().getKey());
		assertEquals(1, compRequestCaptor.getValue().getPartETags().size());
		
		// Make sure the preview is in the dao
		CloudProviderFileHandleInterface fromDao = (CloudProviderFileHandleInterface) stubFileMetadataDao.get(pfm.getId());
		assertEquals(pfm, fromDao);
	}
	
	@Test
	public void testExpectedS3PreviewWithMultipleParts() throws Exception{
		
		String uploadId = "uploadId";
		long contentLength = PreviewManagerImpl.MULTIPART_MAX_PART_SIZE * 2 - 1;
		
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(mockS3Object);
		when(mockS3Object.getObjectContent()).thenReturn(mockS3ObjectInputStream);
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		when(mockPreviewGenerator.generatePreview(mockS3ObjectInputStream, mockOutputStream)).thenReturn(previewContentType);
		when(mockUploadFile.length()).thenReturn(contentLength);
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(789L);
		when(mockMultipartResult.getUploadId()).thenReturn(uploadId);
		when(mockS3Client.initiateMultipartUpload(any())).thenReturn(mockMultipartResult);
		when(mockS3Client.uploadPart(any())).thenReturn(mockUploadPartResult);
		
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		assertNotNull(pfm);
		assertNotNull(pfm.getId());
		assertEquals(previewContentType.getContentType(), pfm.getContentType());
		assertEquals(testMetadata.getCreatedBy(), pfm.getCreatedBy());
		assertNotNull(pfm.getCreatedOn());
		assertEquals("preview"+previewContentType.getExtension(), pfm.getFileName());
		assertEquals(contentLength, pfm.getContentSize());
		
		ArgumentCaptor<InitiateMultipartUploadRequest> initRequestCaptor = ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
		
		verify(mockS3Client).initiateMultipartUpload(initRequestCaptor.capture());
		
		assertEquals(pfm.getBucketName(), initRequestCaptor.getValue().getBucketName());
		assertEquals(pfm.getKey(), initRequestCaptor.getValue().getKey());
		assertEquals(CannedAccessControlList.BucketOwnerFullControl, initRequestCaptor.getValue().getCannedACL());
		
		ArgumentCaptor<UploadPartRequest> partRequestCaptor = ArgumentCaptor.forClass(UploadPartRequest.class);
		
		verify(mockS3Client, times(2)).uploadPart(partRequestCaptor.capture());
		
		assertEquals(2, partRequestCaptor.getAllValues().size());
		
		UploadPartRequest part1 = partRequestCaptor.getAllValues().get(0);
		
		assertEquals(uploadId, part1.getUploadId());
		assertEquals(pfm.getBucketName(), part1.getBucketName());
		assertEquals(pfm.getKey(), part1.getKey());
		assertEquals(1, part1.getPartNumber());
		assertEquals(0, part1.getFileOffset());
		assertEquals(PreviewManagerImpl.MULTIPART_MAX_PART_SIZE, part1.getPartSize());
		assertEquals(mockUploadFile, part1.getFile());
		
		UploadPartRequest part2 = partRequestCaptor.getAllValues().get(1);
		
		assertEquals(uploadId, part2.getUploadId());
		assertEquals(pfm.getBucketName(), part2.getBucketName());
		assertEquals(pfm.getKey(), part2.getKey());
		assertEquals(2, part2.getPartNumber());
		assertEquals(PreviewManagerImpl.MULTIPART_MAX_PART_SIZE, part2.getFileOffset());
		assertEquals(PreviewManagerImpl.MULTIPART_MAX_PART_SIZE - 1, part2.getPartSize());
		assertEquals(mockUploadFile, part2.getFile());
		
		ArgumentCaptor<CompleteMultipartUploadRequest> compRequestCaptor = ArgumentCaptor.forClass(CompleteMultipartUploadRequest.class);
		
		verify(mockS3Client).completeMultipartUpload(compRequestCaptor.capture());
		
		assertEquals(uploadId, compRequestCaptor.getValue().getUploadId());
		assertEquals(pfm.getBucketName(), compRequestCaptor.getValue().getBucketName());
		assertEquals(pfm.getKey(), compRequestCaptor.getValue().getKey());
		assertEquals(2, compRequestCaptor.getValue().getPartETags().size());
		
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
		when(mockStorageLocationDao.get(any())).thenReturn(new S3StorageLocationSetting().setBaseKey("testBaseKey"));
		when(mockS3Client.initiateMultipartUpload(any())).thenReturn(mockMultipartResult);
		when(mockS3Client.uploadPart(any())).thenReturn(mockUploadPartResult);
		
		// Call under test
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		
		assertNotNull(pfm);
		
		ArgumentCaptor<InitiateMultipartUploadRequest> requestCaptor = ArgumentCaptor.forClass(InitiateMultipartUploadRequest.class);
		
		verify(mockStorageLocationDao).get(123L);
		verify(mockS3Client).initiateMultipartUpload(requestCaptor.capture());
		
		assertEquals(StorageClass.IntelligentTiering, requestCaptor.getValue().getStorageClass());
	}
	
	@Test
	public void testExpectedS3PreviewWithBasePath() throws Exception {
		
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
		when(mockStorageLocationDao.get(any())).thenReturn(new S3StorageLocationSetting().setBaseKey("testBaseKey"));
		when(mockS3Client.initiateMultipartUpload(any())).thenReturn(mockMultipartResult);
		when(mockS3Client.uploadPart(any())).thenReturn(mockUploadPartResult);
		
		// Call under test
		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testMetadata);
		
		assertNotNull(pfm);
		assertEquals(123L, pfm.getStorageLocationId());
		assertTrue(pfm.getKey().startsWith("testBaseKey/"));
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
	
	@Test
	public void testExpectedGoogleCloudPreviewWithBaseKey() throws Exception {
		
		testGoogleCloudMetadata.setStorageLocationId(123L);
		
		when(mockFileProvider.createTempFile(any(String.class), any(String.class))).thenReturn(mockUploadFile);
		when(mockFileProvider.createFileOutputStream(mockUploadFile)).thenReturn(mockOutputStream);
		when(mockGoogleCloudClient.getObject(any(String.class), any(String.class))).thenReturn(mockBlob);
		when(mockBlob.reader()).thenReturn(mockGoogleCloudReadChannel);
		when(mockPreviewGenerator.supportsContentType(testContentType, "txt")).thenReturn(true);
		when(mockPreviewGenerator.calculateNeededMemoryBytesForPreview(any(), anyLong())).thenReturn(maxPreviewSize);
		when(mockPreviewGenerator.generatePreview(any(InputStream.class), eq(mockOutputStream))).thenReturn(previewContentType);		
		when(mockUploadFile.length()).thenReturn(resultPreviewSize);
		when(mockStorageLocationDao.get(any())).thenReturn(new ExternalGoogleCloudStorageLocationSetting().setBaseKey("testBaseKey"));
		when(mockIdGenerator.generateNewId(IdType.FILE_IDS)).thenReturn(789L);		

		CloudProviderFileHandleInterface pfm = previewManager.generatePreview(testGoogleCloudMetadata);
		
		assertEquals(123L, pfm.getStorageLocationId());
		assertTrue(pfm.getKey().startsWith("testBaseKey/"));
	}

}
