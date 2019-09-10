package org.sagebionetworks.repo.manager.statistics.records;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.LoggerProvider;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileEvent;
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
	Logger mockLogger;
	
	@Mock
	ProjectResolver mockProjectResolver;

	StatisticsFileEventLogRecordProvider provider;
	
	@BeforeEach
	public void before() {
		when(mockLogProvider.getLogger(StatisticsFileEventLogRecordProvider.class.getName())).thenReturn(mockLogger);
		provider = new StatisticsFileEventLogRecordProvider(mockProjectResolver, mockLogProvider);
	}
	
	@Test
	public void testGetEventClass() {
		assertEquals(StatisticsFileEvent.class, provider.getEventClass());
	}

	@Test
	public void testGetStreamNameForDownload() {
		when(mockEvent.getActionType()).thenReturn(FileEvent.FILE_DOWNLOAD);
		String expectedStreamName = StatisticsFileEventLogRecordProvider.ASSOCIATED_STREAMS
				.get(FileEvent.FILE_DOWNLOAD);
		assertEquals(expectedStreamName, provider.getStreamName(mockEvent));
	}

	@Test
	public void testGetStreamNameForUpload() {
		when(mockEvent.getActionType()).thenReturn(FileEvent.FILE_UPLOAD);
		String expectedStreamName = StatisticsFileEventLogRecordProvider.ASSOCIATED_STREAMS
				.get(FileEvent.FILE_UPLOAD);
		assertEquals(expectedStreamName, provider.getStreamName(mockEvent));
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
		Optional<StatisticsEventLogRecord> record = provider.getRecordForEvent(event);
		
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
		Optional<StatisticsEventLogRecord> record = provider.getRecordForEvent(event);

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
		Optional<StatisticsEventLogRecord> record = provider.getRecordForEvent(event);

		assertFalse(record.isPresent());
		
		verify(mockLogger, times(1)).warn(ex.getMessage(), ex);
	}

}
