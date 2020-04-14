package org.sagebionetworks.repo.manager.statistics.project;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.repo.model.statistics.FileEvent;

@ExtendWith(MockitoExtension.class)
public class StatisticsMonthlyProjectFileDownloadsProcessorTest {

	@Mock
	private StatisticsMonthlyProjectManager mockManager;

	@InjectMocks
	private StatisticsMonthlyProjectFileDownloadsProcessor processor;

	@Test
	public void testProcessMonth() {

		FileEvent eventType = FileEvent.FILE_DOWNLOAD;
		YearMonth month = YearMonth.of(2019, 8);

		doNothing().when(mockManager).computeFileEventsStatistics(eventType, month);

		// Call under test
		processor.processMonth(month);

		verify(mockManager).computeFileEventsStatistics(eventType, month);
	}

}
