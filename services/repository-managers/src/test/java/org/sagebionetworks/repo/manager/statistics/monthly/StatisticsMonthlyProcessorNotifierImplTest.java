package org.sagebionetworks.repo.manager.statistics.monthly;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.springframework.transaction.support.TransactionSynchronization;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.GetQueueUrlResult;

@ExtendWith(MockitoExtension.class)
public class StatisticsMonthlyProcessorNotifierImplTest {

	private static final String TEST_QUEUE = "SomeQueue";
	private static final String TEST_QUEUE_URL = "SomeQueueUrl";

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private AmazonSQS mockSQSClient;

	@Mock
	private TransactionSynchronizationProxy mockTransactionSync;
	
	@Captor
	private ArgumentCaptor<TransactionSynchronization> captorTransaction;

	private StatisticsMonthlyProcessorNotifier notifier;

	@BeforeEach
	public void before() {

		GetQueueUrlResult queueResult = new GetQueueUrlResult().withQueueUrl(TEST_QUEUE_URL);

		when(mockConfig.getQueueName(any())).thenReturn(TEST_QUEUE);
		when(mockSQSClient.getQueueUrl((String) any())).thenReturn(queueResult);

		notifier = new StatisticsMonthlyProcessorNotifierImpl(mockTransactionSync, mockConfig, mockSQSClient);
	}

	@Test
	public void testSendNotificationWithinTransaction() {

		when(mockTransactionSync.isActualTransactionActive()).thenReturn(true);

		StatisticsObjectType objectType = StatisticsObjectType.PROJECT;
		YearMonth month = YearMonth.of(2019, 8);

		// Call under test
		notifier.sendStartProcessingNotification(objectType, month);

		verify(mockTransactionSync).isActualTransactionActive();
		verify(mockTransactionSync).registerSynchronization(captorTransaction.capture());
		verify(mockSQSClient, never()).sendMessage(any(), any());
		
		// Trigger the after commit
		captorTransaction.getValue().afterCommit();
		
		verify(mockSQSClient).sendMessage(TEST_QUEUE_URL, StatisticsMonthlyUtils.buildNotificationBody(objectType, month));

	}

	@Test
	public void testSendNotificationWithNoTransaction() {

		when(mockTransactionSync.isActualTransactionActive()).thenReturn(false);

		StatisticsObjectType objectType = StatisticsObjectType.PROJECT;
		YearMonth month = YearMonth.of(2019, 8);

		Assertions.assertThrows(IllegalStateException.class, () -> {
			// Call under test
			notifier.sendStartProcessingNotification(objectType, month);
		});

		verify(mockTransactionSync).isActualTransactionActive();
		verify(mockTransactionSync, never()).registerSynchronization(any());
		
		verify(mockSQSClient, never()).sendMessage(any(), any());

	}

	
}
