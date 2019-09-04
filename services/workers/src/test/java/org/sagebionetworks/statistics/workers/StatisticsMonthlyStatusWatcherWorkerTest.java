package org.sagebionetworks.statistics.workers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyManager;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

@ExtendWith(MockitoExtension.class)
public class StatisticsMonthlyStatusWatcherWorkerTest {

	private static final String QUEUE_NAME = "someQueueName";
	private static final String QUEUE_URL = "someQueueUrl";

	@Mock
	private StackConfiguration mockConfig;
	
	@Mock
	private StatisticsMonthlyManager mockStatisticsManager;

	@Mock
	private AmazonSQS mockSqsClient;

	private StatisticsMonthlyStatusWatcherWorker worker;

	@BeforeEach
	public void before() {
		when(mockConfig.getQueueName(any())).thenReturn(QUEUE_NAME);
		when(mockSqsClient.getQueueUrl(QUEUE_NAME)).thenReturn(new GetQueueUrlResult().withQueueUrl(QUEUE_URL));
		worker = new StatisticsMonthlyStatusWatcherWorker(mockStatisticsManager, mockSqsClient, mockConfig);
	}

	@Test
	public void testRunWithNoProcessing() throws Exception {
		List<YearMonth> unprocessedMonths = Collections.emptyList();
		when(mockStatisticsManager.getUnprocessedMonths(any(StatisticsObjectType.class))).thenReturn(unprocessedMonths);

		// Call under test
		worker.run(null);

		for (StatisticsObjectType type : StatisticsObjectType.values()) {
			verify(mockStatisticsManager, times(1)).getUnprocessedMonths(type);
		}

		verify(mockSqsClient, never()).sendMessage(any(), any());
	}

	@Test
	public void testRunWithWithProcessing() throws Exception {
		YearMonth month = YearMonth.of(2019, 8);
		
		List<YearMonth> unprocessedMonths = Collections.singletonList(month);

		when(mockStatisticsManager.getUnprocessedMonths(any(StatisticsObjectType.class))).thenReturn(unprocessedMonths);

		// Call under test
		worker.run(null);

		for (StatisticsObjectType type : StatisticsObjectType.values()) {
			verify(mockStatisticsManager, times(1)).getUnprocessedMonths(type);
		}

		verify(mockSqsClient, times(StatisticsObjectType.values().length)).sendMessage(eq(QUEUE_URL), any());
	}

}
