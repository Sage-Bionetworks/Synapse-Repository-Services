package org.sagebionetworks.file.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.dbo.file.FileHandleDao;
import org.sagebionetworks.repo.model.dbo.persistence.DBOFileHandle;
import org.sagebionetworks.repo.model.file.FileHandleStatus;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.google.common.collect.ImmutableList;

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
	 
	private DBOFileHandle fileHandle(Long id, Timestamp createdOn) {
		DBOFileHandle file = new DBOFileHandle();
		file.setId(id);
		file.setCreatedOn(createdOn);
		file.setUpdatedOn(createdOn);
		file.setStatus(FileHandleStatus.AVAILABLE.name());
		return file;
	}
	
	@Test
	public void testRun() throws RecoverableMessageException, Exception {
		
		Timestamp createdOn = Timestamp.from(Instant.now());
		
		Long id1 = 123L;
		Long id2 = 456L;
		
		List<DBOFileHandle> fileHandles = ImmutableList.of(
				fileHandle(id1, createdOn),
				fileHandle(id2, createdOn)
		);	
		
		when(mockFileHandleDao.getDBOFileHandlesBatch(anyList(), anyInt())).thenReturn(fileHandles);
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId(id1.toString()).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.CREATE).setObjectId(id2.toString()).setObjectType(ObjectType.FILE)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verify(mockFileHandleDao).getDBOFileHandlesBatch(Arrays.asList(id1, id2), FileHandleStreamWorker.UPDATED_ON_DAYS_FILTER);
		verify(mockKinesisLogger).logBatch("fileHandleData", Arrays.asList(
				new FileHandleRecord().withId(123).withCreatedOn(createdOn.getTime()).withUpdatedOn(createdOn.getTime()).withIsPreview(false).withStatus("AVAILABLE"),
				new FileHandleRecord().withId(456).withCreatedOn(createdOn.getTime()).withUpdatedOn(createdOn.getTime()).withIsPreview(false).withStatus("AVAILABLE")
		));
		
	}
	
	@Test
	public void testRunWithNonExisting() throws RecoverableMessageException, Exception {
		
		Timestamp createdOn = Timestamp.from(Instant.now());
		
		Long id1 = 123L;
		Long id2 = 456L;
		
		List<DBOFileHandle> fileHandles = ImmutableList.of(
				fileHandle(id1, createdOn)
		);	
		
		when(mockFileHandleDao.getDBOFileHandlesBatch(anyList(), anyInt())).thenReturn(fileHandles);
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId(id1.toString()).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.CREATE).setObjectId(id2.toString()).setObjectType(ObjectType.FILE)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verify(mockFileHandleDao).getDBOFileHandlesBatch(Arrays.asList(id1, id2), FileHandleStreamWorker.UPDATED_ON_DAYS_FILTER);
		verify(mockKinesisLogger).logBatch("fileHandleData", Arrays.asList(
				new FileHandleRecord().withId(123).withCreatedOn(createdOn.getTime()).withUpdatedOn(createdOn.getTime()).withIsPreview(false).withStatus("AVAILABLE")
		));
		
	}
	
	@Test
	public void testRunWithDelete() throws RecoverableMessageException, Exception {
		
		Timestamp createdOn = Timestamp.from(Instant.now());
		
		Long id1 = 123L;
		Long id2 = 456L;
		
		List<DBOFileHandle> fileHandles = ImmutableList.of(
				fileHandle(id1, createdOn)
		);	
		
		when(mockFileHandleDao.getDBOFileHandlesBatch(anyList(), anyInt())).thenReturn(fileHandles);
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId(id1.toString()).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.DELETE).setObjectId(id2.toString()).setObjectType(ObjectType.FILE)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verify(mockFileHandleDao).getDBOFileHandlesBatch(Arrays.asList(id1), FileHandleStreamWorker.UPDATED_ON_DAYS_FILTER);
		verify(mockKinesisLogger).logBatch("fileHandleData", Arrays.asList(
				new FileHandleRecord().withId(123).withCreatedOn(createdOn.getTime()).withUpdatedOn(createdOn.getTime()).withIsPreview(false).withStatus("AVAILABLE")
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
		
		Timestamp createdOn = Timestamp.from(Instant.now());
		
		Long id1 = 123L;
		Long id2 = 456L;
		
		List<DBOFileHandle> fileHandles = ImmutableList.of(
				fileHandle(id1, createdOn)
		);	
		
		when(mockFileHandleDao.getDBOFileHandlesBatch(anyList(), anyInt())).thenReturn(fileHandles);
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId(id1.toString()).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.DELETE).setObjectId(id2.toString()).setObjectType(ObjectType.ENTITY)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verify(mockFileHandleDao).getDBOFileHandlesBatch(Arrays.asList(id1), FileHandleStreamWorker.UPDATED_ON_DAYS_FILTER);
		verify(mockKinesisLogger).logBatch("fileHandleData", Arrays.asList(
				new FileHandleRecord().withId(123).withCreatedOn(createdOn.getTime()).withUpdatedOn(createdOn.getTime()).withIsPreview(false).withStatus("AVAILABLE")
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
		
		Long id1 = 123L;
		Long id2 = 456L;
		
		List<DBOFileHandle> fileHandles = Collections.emptyList();
		
		when(mockFileHandleDao.getDBOFileHandlesBatch(anyList(), anyInt())).thenReturn(fileHandles);
		
		
		List<ChangeMessage> messages = Arrays.asList(
				new ChangeMessage().setChangeType(ChangeType.UPDATE).setObjectId(id1.toString()).setObjectType(ObjectType.FILE),
				new ChangeMessage().setChangeType(ChangeType.DELETE).setObjectId(id2.toString()).setObjectType(ObjectType.ENTITY)
		);
		
		// Call under test
		worker.run(mockCallback, messages);
		
		verify(mockFileHandleDao).getDBOFileHandlesBatch(Arrays.asList(id1), FileHandleStreamWorker.UPDATED_ON_DAYS_FILTER);
		
		verifyZeroInteractions(mockKinesisLogger);		
	}

	@Test
	public void testMapFile() {
		Timestamp createdOn = Timestamp.from(Instant.now());
		
		DBOFileHandle file = fileHandle(123L, createdOn);
		
		file.setIsPreview(true);
		file.setContentSize(123L);
		file.setStatus(FileHandleStatus.AVAILABLE.name());
				
		FileHandleRecord expected = new FileHandleRecord()
				.withId(123)
				.withCreatedOn(createdOn.getTime())
				.withUpdatedOn(createdOn.getTime())
				.withIsPreview(true)
				.withStatus(FileHandleStatus.AVAILABLE.name())
				.withContentSize(123L);
		
		// Call under test
		FileHandleRecord result = worker.mapFileHandle(file);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testMapFileWithNullPreview() {
		Timestamp createdOn = Timestamp.from(Instant.now());
		
		DBOFileHandle file = fileHandle(123L, createdOn);
		
		file.setIsPreview(null);
		file.setStatus(FileHandleStatus.AVAILABLE.name());
				
		FileHandleRecord expected = new FileHandleRecord()
				.withId(123)
				.withCreatedOn(createdOn.getTime())
				.withUpdatedOn(createdOn.getTime())
				.withIsPreview(false)
				.withStatus(FileHandleStatus.AVAILABLE.name());
		
		// Call under test
		FileHandleRecord result = worker.mapFileHandle(file);
		
		assertEquals(expected, result);
	}
	
	@Test
	public void testMapFileWithBucketAndKey() {
		Timestamp createdOn = Timestamp.from(Instant.now());
		
		DBOFileHandle file = fileHandle(123L, createdOn);
		
		file.setIsPreview(false);
		file.setContentSize(123L);
		file.setStatus(FileHandleStatus.AVAILABLE.name());
		file.setBucketName("bucket");
		file.setKey("key");
				
		FileHandleRecord expected = new FileHandleRecord()
				.withId(123)
				.withCreatedOn(createdOn.getTime())
				.withUpdatedOn(createdOn.getTime())
				.withIsPreview(false)
				.withStatus(FileHandleStatus.AVAILABLE.name())
				.withContentSize(123L)
				.withBucket("bucket")
				.withKey("key");
		
		// Call under test
		FileHandleRecord result = worker.mapFileHandle(file);
		
		assertEquals(expected, result);
	}

}
