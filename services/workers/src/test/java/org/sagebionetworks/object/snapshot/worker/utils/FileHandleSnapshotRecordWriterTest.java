package org.sagebionetworks.object.snapshot.worker.utils;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.asynchronous.workers.sqs.MessageUtils;
import org.sagebionetworks.audit.dao.ObjectRecordDAO;
import org.sagebionetworks.audit.utils.ObjectRecordBuilderUtils;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.audit.FileHandleSnapshot;
import org.sagebionetworks.repo.model.audit.ObjectRecord;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.ExternalObjectStoreFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.PreviewFileHandle;
import org.sagebionetworks.repo.model.file.ProxyFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.sqs.model.Message;

public class FileHandleSnapshotRecordWriterTest {

	@Mock
	private FileHandleDao mockFileHandleDao;
	@Mock
	private ObjectRecordDAO mockObjectRecordDao;
	@Mock
	private ProgressCallback mockCallback;
	private FileHandleSnapshotRecordWriter writer;
	private String id = "123";

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		writer = new FileHandleSnapshotRecordWriter();
		ReflectionTestUtils.setField(writer, "fileHandleDao", mockFileHandleDao);
		ReflectionTestUtils.setField(writer, "objectRecordDAO", mockObjectRecordDao);
	}

	@Test
	public void deleteFileMessageTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.DELETE, id, ObjectType.FILE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
		verify(mockObjectRecordDao, never()).saveBatch(anyList(), anyString());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void invalidChangeMessageTest() throws IOException {
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, id, ObjectType.PRINCIPAL, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage));
	}
	
	@Test
	public void validChangeMessageTest() throws IOException {
		FileHandle fileHandle = new S3FileHandle();
		fileHandle.setEtag("etag");
		when(mockFileHandleDao.get(id)).thenReturn(fileHandle);
		
		Message message = MessageUtils.buildMessage(ChangeType.UPDATE, id, ObjectType.FILE, "etag", System.currentTimeMillis());
		ChangeMessage changeMessage = MessageUtils.extractMessageBody(message);
		FileHandleSnapshot record = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(fileHandle);
		ObjectRecord expected = ObjectRecordBuilderUtils.buildObjectRecord(record, changeMessage.getTimestamp().getTime());
		writer.buildAndWriteRecords(mockCallback, Arrays.asList(changeMessage, changeMessage));
		verify(mockFileHandleDao, times(2)).get(id);
		verify(mockObjectRecordDao).saveBatch(eq(Arrays.asList(expected, expected)), eq(expected.getJsonClassName()));
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
		s3FH.setFileName("fileName");
		s3FH.setId("555");
		s3FH.setKey("key");
		s3FH.setStorageLocationId(900L);
		FileHandleSnapshot snapshot = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(s3FH);
		assertEquals(s3FH.getBucketName(), snapshot.getBucket());
		assertEquals(s3FH.getConcreteType(), snapshot.getConcreteType());
		assertEquals(s3FH.getContentMd5(), snapshot.getContentMd5());
		assertEquals(s3FH.getContentSize(), snapshot.getContentSize());
		assertEquals(s3FH.getCreatedBy(), snapshot.getCreatedBy());
		assertEquals(s3FH.getCreatedOn(), snapshot.getCreatedOn());
		assertEquals(s3FH.getFileName(), snapshot.getFileName());
		assertEquals(s3FH.getId(), snapshot.getId());
		assertEquals(s3FH.getKey(), snapshot.getKey());
		assertEquals(s3FH.getStorageLocationId(), snapshot.getStorageLocationId());
	}

	@Test
	public void testBuildFileHandleSnapshotWithPreviewFileHandle() {
		PreviewFileHandle previewFH = new PreviewFileHandle();
		previewFH.setBucketName("bucket");
		previewFH.setConcreteType(S3FileHandle.class.getName());
		previewFH.setContentMd5("md5");
		previewFH.setContentSize(1L);
		previewFH.setCreatedBy("998");
		previewFH.setCreatedOn(new Date());
		previewFH.setFileName("fileName");
		previewFH.setId("555");
		previewFH.setKey("key");
		previewFH.setStorageLocationId(900L);
		FileHandleSnapshot snapshot = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(previewFH);
		assertEquals(previewFH.getBucketName(), snapshot.getBucket());
		assertEquals(previewFH.getConcreteType(), snapshot.getConcreteType());
		assertEquals(previewFH.getContentMd5(), snapshot.getContentMd5());
		assertEquals(previewFH.getContentSize(), snapshot.getContentSize());
		assertEquals(previewFH.getCreatedBy(), snapshot.getCreatedBy());
		assertEquals(previewFH.getCreatedOn(), snapshot.getCreatedOn());
		assertEquals(previewFH.getFileName(), snapshot.getFileName());
		assertEquals(previewFH.getId(), snapshot.getId());
		assertEquals(previewFH.getKey(), snapshot.getKey());
		assertEquals(previewFH.getStorageLocationId(), snapshot.getStorageLocationId());
	}

	@Test
	public void testBuildFileHandleSnapshotWithExternalFileHandle() {
		ExternalFileHandle externalFH = new ExternalFileHandle();
		externalFH.setConcreteType(S3FileHandle.class.getName());
		externalFH.setContentMd5("md5");
		externalFH.setContentSize(1L);
		externalFH.setCreatedBy("998");
		externalFH.setCreatedOn(new Date());
		externalFH.setFileName("fileName");
		externalFH.setId("555");
		externalFH.setExternalURL("externalURL");
		externalFH.setStorageLocationId(900L);
		FileHandleSnapshot snapshot = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(externalFH);
		assertNull(snapshot.getBucket());
		assertEquals(externalFH.getConcreteType(), snapshot.getConcreteType());
		assertEquals(externalFH.getContentMd5(), snapshot.getContentMd5());
		assertEquals(externalFH.getContentSize(), snapshot.getContentSize());
		assertEquals(externalFH.getCreatedBy(), snapshot.getCreatedBy());
		assertEquals(externalFH.getCreatedOn(), snapshot.getCreatedOn());
		assertEquals(externalFH.getFileName(), snapshot.getFileName());
		assertEquals(externalFH.getId(), snapshot.getId());
		assertEquals(externalFH.getExternalURL(), snapshot.getKey());
		assertEquals(externalFH.getStorageLocationId(), snapshot.getStorageLocationId());
	}

	@Test
	public void testBuildFileHandleSnapshotWithProxyFileHandle() {
		ProxyFileHandle proxyFH = new ProxyFileHandle();
		proxyFH.setConcreteType(ProxyFileHandle.class.getName());
		proxyFH.setContentMd5("md5");
		proxyFH.setContentSize(1L);
		proxyFH.setCreatedBy("998");
		proxyFH.setCreatedOn(new Date());
		proxyFH.setFileName("fileName");
		proxyFH.setId("555");
		proxyFH.setFilePath("filePath");
		proxyFH.setStorageLocationId(900L);
		FileHandleSnapshot snapshot = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(proxyFH);
		assertNull(snapshot.getBucket());
		assertEquals(proxyFH.getConcreteType(), snapshot.getConcreteType());
		assertEquals(proxyFH.getContentMd5(), snapshot.getContentMd5());
		assertEquals(proxyFH.getContentSize(), snapshot.getContentSize());
		assertEquals(proxyFH.getCreatedBy(), snapshot.getCreatedBy());
		assertEquals(proxyFH.getCreatedOn(), snapshot.getCreatedOn());
		assertEquals(proxyFH.getFileName(), snapshot.getFileName());
		assertEquals(proxyFH.getId(), snapshot.getId());
		assertEquals(proxyFH.getFilePath(), snapshot.getKey());
		assertEquals(proxyFH.getStorageLocationId(), snapshot.getStorageLocationId());
	}

	@Test
	public void testBuildFileHandleSnapshotWithExternalObjectStoreFileHandle() {
		ExternalObjectStoreFileHandle externalObjectStoreFileHandle = new ExternalObjectStoreFileHandle();
		externalObjectStoreFileHandle.setConcreteType(ExternalObjectStoreFileHandle.class.getName());
		externalObjectStoreFileHandle.setContentMd5("md5");
		externalObjectStoreFileHandle.setContentSize(1L);
		externalObjectStoreFileHandle.setCreatedBy("998");
		externalObjectStoreFileHandle.setCreatedOn(new Date());
		externalObjectStoreFileHandle.setFileName("fileName");
		externalObjectStoreFileHandle.setId("555");
		externalObjectStoreFileHandle.setFileKey("key");
		externalObjectStoreFileHandle.setStorageLocationId(900L);
		FileHandleSnapshot snapshot = FileHandleSnapshotRecordWriter.buildFileHandleSnapshot(externalObjectStoreFileHandle);
		assertNull(snapshot.getBucket());
		assertEquals(externalObjectStoreFileHandle.getConcreteType(), snapshot.getConcreteType());
		assertEquals(externalObjectStoreFileHandle.getContentMd5(), snapshot.getContentMd5());
		assertEquals(externalObjectStoreFileHandle.getContentSize(), snapshot.getContentSize());
		assertEquals(externalObjectStoreFileHandle.getCreatedBy(), snapshot.getCreatedBy());
		assertEquals(externalObjectStoreFileHandle.getCreatedOn(), snapshot.getCreatedOn());
		assertEquals(externalObjectStoreFileHandle.getFileName(), snapshot.getFileName());
		assertEquals(externalObjectStoreFileHandle.getId(), snapshot.getId());
		assertEquals(externalObjectStoreFileHandle.getFileKey(), snapshot.getKey());
		assertEquals(externalObjectStoreFileHandle.getStorageLocationId(), snapshot.getStorageLocationId());
	}
}
