package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dao.FileHandleDao;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.FileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.google.common.collect.ImmutableMap;

@ExtendWith(MockitoExtension.class)
public class FileHandleStreamWorkerTest {
	
	@Mock
	private FileHandleDao mockFileHandleDao;
	
	@Mock
	private AwsKinesisFirehoseLogger mockKinesisLogger;
	
	@InjectMocks
	private FileHandleStreamWorker worker;
	
	@Mock
	private ProgressCallback mockCallback;
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		
		Date createdOn = new Date();
		
		String id1 = "123";
		String id2 = "456";
		
		Map<String, FileHandle> fileHandles = ImmutableMap.of(
				id1, new S3FileHandle().setId(id1).setCreatedOn(createdOn),
				id2, new S3FileHandle().setId(id2).setCreatedOn(createdOn)
		);
				
		
		when(mockFileHandleDao.getAllFileHandlesBatch(anyIterable())).thenReturn(fileHandles);
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId(id1).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.CREATE).setObjectId(id2).setObjectType(ObjectType.FILE)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verify(mockFileHandleDao).getAllFileHandlesBatch(Arrays.asList(id1, id2));
		verify(mockKinesisLogger).logBatch("fileHandleData", Arrays.asList(
				new FileHandleRecord().withId(123).withCreatedOn(createdOn.getTime()).withIsPreview(false).withStatus("AVAILABLE"),
				new FileHandleRecord().withId(456).withCreatedOn(createdOn.getTime()).withIsPreview(false).withStatus("AVAILABLE")
		));
		
	}
	
	@Test
	public void testRunWithNonExisting() throws RecoverableMessageException, Exception {
		
		Date createdOn = new Date();
		
		String id1 = "123";
		String id2 = "456";
		
		Map<String, FileHandle> fileHandles = ImmutableMap.of(
				id1, new S3FileHandle().setId(id1).setCreatedOn(createdOn)
		);
				
		
		when(mockFileHandleDao.getAllFileHandlesBatch(anyIterable())).thenReturn(fileHandles);
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId(id1).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.CREATE).setObjectId(id2).setObjectType(ObjectType.FILE)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verify(mockFileHandleDao).getAllFileHandlesBatch(Arrays.asList(id1, id2));
		verify(mockKinesisLogger).logBatch("fileHandleData", Arrays.asList(
				new FileHandleRecord().withId(123).withCreatedOn(createdOn.getTime()).withIsPreview(false).withStatus("AVAILABLE")
		));
		
	}
	
	@Test
	public void testRunWithDelete() throws RecoverableMessageException, Exception {
		
		Date createdOn = new Date();
		
		String id1 = "123";
		String id2 = "456";
		
		Map<String, FileHandle> fileHandles = ImmutableMap.of(
				id1, new S3FileHandle().setId(id1).setCreatedOn(createdOn)
		);
				
		
		when(mockFileHandleDao.getAllFileHandlesBatch(anyIterable())).thenReturn(fileHandles);
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId(id1).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.DELETE).setObjectId(id2).setObjectType(ObjectType.FILE)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verify(mockFileHandleDao).getAllFileHandlesBatch(Arrays.asList(id1));
		verify(mockKinesisLogger).logBatch("fileHandleData", Arrays.asList(
				new FileHandleRecord().withId(123).withCreatedOn(createdOn.getTime()).withIsPreview(false).withStatus("AVAILABLE")
		));
		
	}
	
	@Test
	public void testRunWithNoAcceptableChanges() throws RecoverableMessageException, Exception {
		
		String id1 = "123";
		String id2 = "456";
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.DELETE).setObjectId(id1).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.DELETE).setObjectId(id2).setObjectType(ObjectType.FILE)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verifyZeroInteractions(mockFileHandleDao);
		verifyZeroInteractions(mockKinesisLogger);
		
	}
	
	@Test
	public void testRunWithWithNotFile() throws RecoverableMessageException, Exception {
		
		Date createdOn = new Date();
		
		String id1 = "123";
		String id2 = "456";
		
		Map<String, FileHandle> fileHandles = ImmutableMap.of(
				id1, new S3FileHandle().setId(id1).setCreatedOn(createdOn)
		);
				
		
		when(mockFileHandleDao.getAllFileHandlesBatch(anyIterable())).thenReturn(fileHandles);
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId(id1).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.DELETE).setObjectId(id2).setObjectType(ObjectType.ENTITY)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verify(mockFileHandleDao).getAllFileHandlesBatch(Arrays.asList(id1));
		verify(mockKinesisLogger).logBatch("fileHandleData", Arrays.asList(
				new FileHandleRecord().withId(123).withCreatedOn(createdOn.getTime()).withIsPreview(false).withStatus("AVAILABLE")
		));
		
	}
	
	@Test
	public void testRunWithWithNoChanges() throws RecoverableMessageException, Exception {
		
		List<ChangeMessage> messages = Collections.emptyList();
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verifyZeroInteractions(mockFileHandleDao);
		verifyZeroInteractions(mockKinesisLogger);
	}
	
	@Test
	public void testRunWithWithNoFiles() throws RecoverableMessageException, Exception {
		
		String id1 = "123";
		String id2 = "456";
		
		Map<String, FileHandle> fileHandles = Collections.emptyMap();
				
		
		when(mockFileHandleDao.getAllFileHandlesBatch(anyIterable())).thenReturn(fileHandles);
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId(id1).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.DELETE).setObjectId(id2).setObjectType(ObjectType.ENTITY)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verify(mockFileHandleDao).getAllFileHandlesBatch(Arrays.asList(id1));
		
		verifyZeroInteractions(mockKinesisLogger);		
	}

	@Test
	public void testMapFile() {
		Date createdOn = new Date();
		
		FileHandle file = new S3FileHandle()
				.setId("123")
				.setIsPreview(true)
				.setCreatedOn(createdOn)
				.setContentSize(123L);
				
		FileHandleRecord expected = new FileHandleRecord()
				.withId(123)
				.withCreatedOn(createdOn.getTime())
				.withIsPreview(true)
				.withStatus("AVAILABLE")
				.withContentSize(123L);
		
		// Call under test
		FileHandleRecord result = worker.mapFileHandle(file);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testMapFileWithNullPreview() {
		Date createdOn = new Date();
		
		FileHandle file = new S3FileHandle()
				.setId("123")
				.setIsPreview(null)
				.setCreatedOn(createdOn);
				
		FileHandleRecord expected = new FileHandleRecord()
				.withId(123)
				.withCreatedOn(createdOn.getTime())
				.withIsPreview(false)
				.withStatus("AVAILABLE");
		
		// Call under test
		FileHandleRecord result = worker.mapFileHandle(file);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testMapFileWithBucketAndKey() {
		Date createdOn = new Date();
		
		FileHandle file = new S3FileHandle()
				.setId("123")
				.setIsPreview(false)
				.setCreatedOn(createdOn)
				.setContentSize(123L)
				.setBucketName("bucket")
				.setKey("key");
				
		FileHandleRecord expected = new FileHandleRecord()
				.withId(123)
				.withCreatedOn(createdOn.getTime())
				.withIsPreview(false)
				.withStatus("AVAILABLE")
				.withContentSize(123L)
				.withBucket("bucket")
				.withKey("key");
		
		// Call under test
		FileHandleRecord result = worker.mapFileHandle(file);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testMapFileWithNotCloud() {
		Date createdOn = new Date();
		
		FileHandle file = new ExternalFileHandle()
				.setId("123")
				.setCreatedOn(createdOn);
				
		FileHandleRecord expected = new FileHandleRecord()
				.withId(123)
				.withCreatedOn(createdOn.getTime())
				.withIsPreview(false)
				.withStatus("AVAILABLE");
		
		// Call under test
		FileHandleRecord result = worker.mapFileHandle(file);
		
		assertEquals(expected, result);
	}

}
