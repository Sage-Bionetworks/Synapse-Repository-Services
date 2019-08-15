package org.sagebionetworks.repo.manager.statistics;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sagebionetworks.kinesis.AwsKinesisFirehoseLogger;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileActionType;
import org.sagebionetworks.repo.manager.statistics.events.StatisticsFileEvent;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsEventLogRecord;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsFileEventLogRecordProvider;
import org.sagebionetworks.repo.manager.statistics.records.StatisticsLogRecordProviderFactory;
import org.sagebionetworks.repo.model.file.FileHandleAssociateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StatisticsEventsCollectorAutowireTest {

	@Autowired
	private StatisticsLogRecordProviderFactory logRecordProviderFactory;

	@Autowired
	private StatisticsFileEventLogRecordProvider fileEventLogRecordProvider;

	@Mock
	private AwsKinesisFirehoseLogger firehoseLogger;

	private StatisticsEventsCollector statsEventsCollector;

	@BeforeEach
	public void before() throws Exception {
		MockitoAnnotations.initMocks(this);
		// We mock the firehose logger
		statsEventsCollector = new StatisticsEventsCollectorImpl(firehoseLogger, logRecordProviderFactory);
	}

	@Test
	public void testCollectEventWithNullEvent() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			statsEventsCollector.collectEvent(null);
		});
	}

	@Test
	public void testCollectFileDownloadEvent() {
		StatisticsFileEvent event = new StatisticsFileEvent(StatisticsFileActionType.FILE_DOWNLOAD, 123L, "123", "123",
				FileHandleAssociateType.FileEntity);

		StatisticsEventLogRecord expectedRecord = fileEventLogRecordProvider.getRecordForEvent(event);
		String expectedStream = StatisticsFileEventLogRecordProvider.ASSOCIATED_STREAMS
				.get(StatisticsFileActionType.FILE_DOWNLOAD);

		// Call under test
		statsEventsCollector.collectEvent(event);

		verify(firehoseLogger, times(1)).logBatch(eq(expectedStream), eq(Collections.singletonList(expectedRecord)));

	}

	@Test
	public void testCollectFileUploadEvent() {
		StatisticsFileEvent event = new StatisticsFileEvent(StatisticsFileActionType.FILE_UPLOAD, 123L, "123", "123",
				FileHandleAssociateType.FileEntity);

		StatisticsEventLogRecord expectedRecord = fileEventLogRecordProvider.getRecordForEvent(event);
		String expectedStream = StatisticsFileEventLogRecordProvider.ASSOCIATED_STREAMS
				.get(StatisticsFileActionType.FILE_UPLOAD);

		// Call under test
		statsEventsCollector.collectEvent(event);

		verify(firehoseLogger, times(1)).logBatch(eq(expectedStream), eq(Collections.singletonList(expectedRecord)));

	}

	@Test
	public void testCollectEventWithoutSending() {
		StatisticsFileEvent event = new StatisticsFileEvent(StatisticsFileActionType.FILE_UPLOAD, 123L, "123", "123",
				FileHandleAssociateType.TeamAttachment);

		// Call under test
		statsEventsCollector.collectEvent(event);

		verify(firehoseLogger, never()).logBatch(any(), any());
	}
}
