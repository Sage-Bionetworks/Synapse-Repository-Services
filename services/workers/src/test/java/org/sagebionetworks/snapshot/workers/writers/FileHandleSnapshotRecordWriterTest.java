package org.sagebionetworks.snapshot.workers.writers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.FileHandleSnapshot;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.repo.model.file.GoogleCloudFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.snapshot.workers.KinesisObjectSnapshotRecord;
import org.sagebionetworks.util.progress.ProgressCallback;

import com.amazonaws.services.sqs.model.Message;

@ExtendWith(MockitoExtension.class)
public class FileHandleSnapshotRecordWriterTest {

	@Mock
	private FileHandleDao mockFileHandleDao;
	@Mock
	private ProgressCallback mockCallback;
	@Mock
	private AwsKinesisFirehoseLogger mockKinesisLogger;

	@InjectMocks
	private FileHandleSnapshotRecordWriter writer;

	private String id = "123";
	
	@Captor
	private ArgumentCaptor<List<KinesisObjectSnapshotRecord<?>>> recordCaptor;

	@Test
	public void deleteFileMessageTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, id, ObjectType.FILE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		
		KinesisObjectSnapshotRecord<?> expectedRecord = KinesisObjectSnapshotRecord.map(changeMessage, new FileHandleSnapshot().setId(id));
		
		verify(mockKinesisLogger).logBatch(eq("fileSnapshots"), recordCaptor.capture());
		
		expectedRecord.withSnapshotTimestamp(recordCaptor.getValue().get(0).getSnapshotTimestamp());
		
		assertEquals(recordCaptor.getValue(), List.of(expectedRecord));
		
	}

	@Test
	public void invalidChangeMessageTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, id, ObjectType.PRINCIPAL, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		assertThrows(IllegalArgumentException.class, () -> writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage)));
		verifyZeroInteractions(mockKinesisLogger);
	}

	@Test
	public void validChangeMessageTest() throws IOException {
		FileHandle fileHandle = new S3FileHandle();
		fileHandle.setEtag("etag");
		when(mockFileHandleDao.get(id)).thenReturn(fileHandle);

		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, id, ObjectType.FILE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		FileHandleSnapshot record = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(fileHandle);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage, changeMessage));
		verify(mockFileHandleDao, times(2)).get(id);
		
		List<KinesisObjectSnapshotRecord<?>> expectedRecords = List.of(
			KinesisObjectSnapshotRecord.map(changeMessage, record),
			KinesisObjectSnapshotRecord.map(changeMessage, record)
		);
		
		verify(mockKinesisLogger).logBatch(eq("fileSnapshots"), recordCaptor.capture());
		
		for (int i=0; i<expectedRecords.size(); i++) {
			expectedRecords.get(i).withSnapshotTimestamp(recordCaptor.getValue().get(i).getSnapshotTimestamp());
		}
		
		assertEquals(recordCaptor.getValue(), expectedRecords);
	}

	@Test
	public void testBuildFileHandleSnapshotWithS3FileHandle() {
		S3FileHandle s3FH = new S3FileHandle();
		s3FH.setBucketName("bucket");
		s3FH.setConcreteType(S3FileHandle.class.getName());
		s3FH.setContentMd5("md5");
		s3FH.setContentSize(1L);
		s3FH.setCreatedBy("998");
		s3FH.setCreatedOn(new Date());
		s3FH.setModifiedOn(new Date());
		s3FH.setFileName("fileName");
		s3FH.setId("555");
		s3FH.setKey("key");
		s3FH.setStorageLocationId(900L);
		s3FH.setIsPreview(false);
		s3FH.setPreviewId("456");
		s3FH.setContentType("type");
		s3FH.setStatus(FileHandleStatus.ARCHIVED);
		
		FileHandleSnapshot expected = new FileHandleSnapshot()
			.setId("555")
			.setBucket("bucket")
			.setKey("key")
			.setContentMd5("md5")
			.setConcreteType(S3FileHandle.class.getName())
			.setContentSize(1L)
			.setCreatedBy("998")
			.setCreatedOn(s3FH.getCreatedOn())
			.setModifiedOn(s3FH.getModifiedOn())
			.setFileName("fileName")
			.setStorageLocationId(900L)
			.setIsPreview(false)
			.setPreviewId("456")
			.setContentType("type")
			.setStatus(FileHandleStatus.ARCHIVED);
		
		FileHandleSnapshot snapshot = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(s3FH);

		assertEquals(expected, snapshot);
		
	}

	@Test
	public void testBuildFileHandleSnapshotWithGoogleCloudFileHandle() {
		GoogleCloudFileHandle googleCloudFileHandle = new GoogleCloudFileHandle();
		googleCloudFileHandle.setBucketName("bucket");
		googleCloudFileHandle.setConcreteType(GoogleCloudFileHandle.class.getName());
		googleCloudFileHandle.setContentMd5("md5");
		googleCloudFileHandle.setContentSize(1L);
		googleCloudFileHandle.setCreatedBy("998");
		googleCloudFileHandle.setCreatedOn(new Date());
		googleCloudFileHandle.setModifiedOn(new Date());
		googleCloudFileHandle.setFileName("fileName");
		googleCloudFileHandle.setId("555");
		googleCloudFileHandle.setKey("key");
		googleCloudFileHandle.setStorageLocationId(900L);
		googleCloudFileHandle.setIsPreview(false);
		googleCloudFileHandle.setPreviewId("456");
		googleCloudFileHandle.setContentType("type");
		googleCloudFileHandle.setStatus(FileHandleStatus.ARCHIVED);
		
		FileHandleSnapshot expected = new FileHandleSnapshot()
			.setId("555")
			.setBucket("bucket")
			.setKey("key")
			.setContentMd5("md5")
			.setConcreteType(GoogleCloudFileHandle.class.getName())
			.setContentSize(1L)
			.setCreatedBy("998")
			.setCreatedOn(googleCloudFileHandle.getCreatedOn())
			.setModifiedOn(googleCloudFileHandle.getModifiedOn())
			.setFileName("fileName")
			.setStorageLocationId(900L)
			.setIsPreview(false)
			.setPreviewId("456")
			.setContentType("type")
			.setStatus(FileHandleStatus.ARCHIVED);
		
		FileHandleSnapshot snapshot = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(googleCloudFileHandle);

		assertEquals(expected, snapshot);
	}

	@Test
	public void testBuildFileHandleSnapshotWithExternalFileHandle() {
		ExternalFileHandle externalFH = new ExternalFileHandle();
		externalFH.setConcreteType(ExternalFileHandle.class.getName());
		externalFH.setContentMd5("md5");
		externalFH.setContentSize(1L);
		externalFH.setCreatedBy("998");
		externalFH.setCreatedOn(new Date());
		externalFH.setModifiedOn(new Date());
		externalFH.setFileName("fileName");
		externalFH.setId("555");
		externalFH.setExternalURL("externalURL");
		externalFH.setStorageLocationId(900L);
		externalFH.setContentType("type");
		externalFH.setStatus(FileHandleStatus.ARCHIVED);
		
		FileHandleSnapshot expected = new FileHandleSnapshot()
			.setId("555")
			.setBucket(null)
			.setKey("externalURL")
			.setContentMd5("md5")
			.setConcreteType(ExternalFileHandle.class.getName())
			.setContentSize(1L)
			.setCreatedBy("998")
			.setCreatedOn(externalFH.getCreatedOn())
			.setModifiedOn(externalFH.getModifiedOn())
			.setFileName("fileName")
			.setStorageLocationId(900L)
			.setIsPreview(false)
			.setPreviewId(null)
			.setContentType("type")
			.setStatus(FileHandleStatus.ARCHIVED);
		
		FileHandleSnapshot snapshot = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(externalFH);

		assertEquals(expected, snapshot);
	}

	@Test
	public void testBuildFileHandleSnapshotWithProxyFileHandle() {
		ProxyFileHandle proxyFH = new ProxyFileHandle();
		proxyFH.setConcreteType(ProxyFileHandle.class.getName());
		proxyFH.setContentMd5("md5");
		proxyFH.setContentSize(1L);
		proxyFH.setCreatedBy("998");
		proxyFH.setCreatedOn(new Date());
		proxyFH.setModifiedOn(new Date());
		proxyFH.setFileName("fileName");
		proxyFH.setId("555");
		proxyFH.setFilePath("filePath");
		proxyFH.setStorageLocationId(900L);
		proxyFH.setContentType("type");
		proxyFH.setStatus(FileHandleStatus.ARCHIVED);
		
		FileHandleSnapshot expected = new FileHandleSnapshot()
			.setId("555")
			.setBucket(null)
			.setKey("filePath")
			.setContentMd5("md5")
			.setConcreteType(ProxyFileHandle.class.getName())
			.setContentSize(1L)
			.setCreatedBy("998")
			.setCreatedOn(proxyFH.getCreatedOn())
			.setModifiedOn(proxyFH.getModifiedOn())
			.setFileName("fileName")
			.setStorageLocationId(900L)
			.setIsPreview(false)
			.setPreviewId(null)
			.setContentType("type")
			.setStatus(FileHandleStatus.ARCHIVED);
		
		FileHandleSnapshot snapshot = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(proxyFH);

		assertEquals(expected, snapshot);
	}

	@Test
	public void testBuildFileHandleSnapshotWithExternalObjectStoreFileHandle() {
		ExternalObjectStoreFileHandle externalObjectStoreFileHandle = new ExternalObjectStoreFileHandle();
		externalObjectStoreFileHandle.setConcreteType(ExternalObjectStoreFileHandle.class.getName());
		externalObjectStoreFileHandle.setContentMd5("md5");
		externalObjectStoreFileHandle.setContentSize(1L);
		externalObjectStoreFileHandle.setCreatedBy("998");
		externalObjectStoreFileHandle.setCreatedOn(new Date());
		externalObjectStoreFileHandle.setModifiedOn(new Date());
		externalObjectStoreFileHandle.setFileName("fileName");
		externalObjectStoreFileHandle.setId("555");
		externalObjectStoreFileHandle.setFileKey("key");
		externalObjectStoreFileHandle.setStorageLocationId(900L);
		externalObjectStoreFileHandle.setContentType("type");
		externalObjectStoreFileHandle.setStatus(FileHandleStatus.ARCHIVED);
		
		FileHandleSnapshot expected = new FileHandleSnapshot()
			.setId("555")
			.setBucket(null)
			.setKey("key")
			.setContentMd5("md5")
			.setConcreteType(ExternalObjectStoreFileHandle.class.getName())
			.setContentSize(1L)
			.setCreatedBy("998")
			.setCreatedOn(externalObjectStoreFileHandle.getCreatedOn())
			.setModifiedOn(externalObjectStoreFileHandle.getModifiedOn())
			.setFileName("fileName")
			.setStorageLocationId(900L)
			.setIsPreview(false)
			.setPreviewId(null)
			.setContentType("type")
			.setStatus(FileHandleStatus.ARCHIVED);
		
		FileHandleSnapshot snapshot = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(externalObjectStoreFileHandle);

		assertEquals(expected, snapshot);
	}
}
