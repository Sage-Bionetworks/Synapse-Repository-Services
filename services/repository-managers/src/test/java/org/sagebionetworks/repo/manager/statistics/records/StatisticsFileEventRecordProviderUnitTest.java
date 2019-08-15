package org.sagebionetworks.repo.manager.statistics.records;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileActionType;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileEvent;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StatisticsFileEventRecordProviderUnitTest {

	@InjectMocks
	StatisticsFileEventLogRecordProvider provider;

	@Mock
	StatisticsFileEvent mockEvent;

	@Test
	public void testGetEventClass() {
		assertEquals(StatisticsFileEvent.class, provider.getEventClass());
	}

	@Test
	public void testGetStreamNameForDownload() {
		when(mockEvent.getActionType()).thenReturn(StatisticsFileActionType.FILE_DOWNLOAD);
		String expectedStreamName = StatisticsFileEventLogRecordProvider.ASSOCIATED_STREAMS
				.get(StatisticsFileActionType.FILE_DOWNLOAD);
		assertEquals(expectedStreamName, provider.getStreamName(mockEvent));
	}

	@Test
	public void testGetStreamNameForUpload() {
		when(mockEvent.getActionType()).thenReturn(StatisticsFileActionType.FILE_UPLOAD);
		String expectedStreamName = StatisticsFileEventLogRecordProvider.ASSOCIATED_STREAMS
				.get(StatisticsFileActionType.FILE_UPLOAD);
		assertEquals(expectedStreamName, provider.getStreamName(mockEvent));
	}


	@Test
	public void testSendToStream() {
		for (FileHandleAssociateType associate : FileHandleAssociateType.values()) {
			when(mockEvent.getAssociationType()).thenReturn(associate);
			if (FileHandleAssociateType.FileEntity.equals(associate) || FileHandleAssociateType.TableEntity.equals(associate)) {
				assertTrue(provider.sendToStream(mockEvent));
			} else {
				assertFalse(provider.sendToStream(mockEvent));
			}
		}
	}

	@Test
	public void testGetRecordForEvent() {
		Long userId = 123L;
		String fileHandleId = "123";
		String associateId = "123";
		FileHandleAssociateType associateType = FileHandleAssociateType.FileEntity;

		StatisticsFileEvent event = new StatisticsFileEvent(StatisticsFileActionType.FILE_DOWNLOAD, userId,
				fileHandleId, associateId, associateType);

		StatisticsEventLogRecord expectedRecord = new StatisticsFileEventLogRecord()
				.withAssociation(associateType, associateId).withTimestamp(event.getTimestamp()).withUserId(userId)
				.withFileHandleId(fileHandleId);

		assertEquals(expectedRecord, provider.getRecordForEvent(event));
	}

}
