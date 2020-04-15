package org.sagebionetworks.repo.manager.statistics;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.kinesis.AwsKinesisLogRecord;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.sagebionetworks.repo.model.statistics.FileEvent;
import org.sagebionetworks.repo.web.NotFoundException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StatisticsFileEventRecordProviderUnitTest {

	@Mock
	StatisticsFileEvent mockEvent;
	
	@Mock
	LoggerProvider mockLogProvider;
	
	@Mock
	ProjectResolver mockProjectResolver;

	@InjectMocks
	StatisticsFileEventLogRecordProvider provider;
	
	@Test
	public void testGetEventClass() {
		assertEquals(StatisticsFileEvent.class, provider.getEventClass());
	}

	@Test
	public void testGetStreamNameForDownload() {
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		when(mockEvent.getActionType()).thenReturn(eventType);
		assertEquals(eventType.getFirehoseStreamName(), provider.getStreamName(mockEvent));
	}

	@Test
	public void testGetStreamNameForUpload() {
		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		when(mockEvent.getActionType()).thenReturn(eventType);
		assertEquals(eventType.getFirehoseStreamName(), provider.getStreamName(mockEvent));
	}

	@Test
	public void testGetRecordForEvent() {
		Long userId = 123L;
		Long projectId = 456L;
		String fileHandleId = "123";
		String associateId = "123";
		FileHandleAssociateType associateType = FileHandleAssociateType.FileEntity;

		StatisticsFileEvent event = new StatisticsFileEvent(FileEvent.FILE_DOWNLOAD, userId,
				fileHandleId, associateId, associateType);

		StatisticsEventLogRecord expectedRecord = new StatisticsFileEventLogRecord()
				.withAssociation(associateType, associateId)
				.withProjectId(projectId)
				.withTimestamp(event.getTimestamp())
				.withUserId(userId)
				.withFileHandleId(fileHandleId);
		
		when(mockProjectResolver.resolveProject(associateType, associateId)).thenReturn(projectId);

		// Call under test
		Optional<AwsKinesisLogRecord> record = provider.getRecordForEvent(event);
		
		assertEquals(expectedRecord, record.get());
	}
	
	@Test
	public void testGetRecordForUnsupportedEvent() {
		Long userId = 123L;
		String fileHandleId = "123";
		String associateId = "123";
		FileHandleAssociateType associateType = FileHandleAssociateType.TeamAttachment;

		StatisticsFileEvent event = new StatisticsFileEvent(FileEvent.FILE_DOWNLOAD, userId,
				fileHandleId, associateId, associateType);

		// Call under test
		Optional<AwsKinesisLogRecord> record = provider.getRecordForEvent(event);

		assertFalse(record.isPresent());
	}
	
	@Test
	public void testGetRecordForNonExistingProject() {
		Long userId = 123L;
		String fileHandleId = "123";
		String associateId = "123";
		FileHandleAssociateType associateType = FileHandleAssociateType.FileEntity;

		StatisticsFileEvent event = new StatisticsFileEvent(FileEvent.FILE_DOWNLOAD, userId,
				fileHandleId, associateId, associateType);
		
		NotFoundException ex = new NotFoundException("Not Found");
		
		when(mockProjectResolver.resolveProject(associateType, associateId)).thenThrow(ex);

		// Call under test
		Optional<AwsKinesisLogRecord> record = provider.getRecordForEvent(event);

		assertFalse(record.isPresent());
	}

}
