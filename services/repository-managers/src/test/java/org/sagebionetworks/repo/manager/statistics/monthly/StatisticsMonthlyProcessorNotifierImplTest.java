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

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@ExtendWith(MockitoExtension.class)
public class StatisticsMonthlyProcessorNotifierImplTest {

	private static final String TEST_QUEUE = "SomeQueue";
	private static final String TEST_QUEUE_URL = "SomeQueueUrl";

	@Mock
	private StackConfiguration mockConfig;

	@Mock
	private SqsClient mockSQSClient;

	@Mock
	private TransactionSynchronizationProxy mockTransactionSync;
	
	@Captor
	private ArgumentCaptor<TransactionSynchronization> captorTransaction;

	private StatisticsMonthlyProcessorNotifier notifier;

	@BeforeEach
	public void before() {

		GetQueueUrlResponse queueResult = GetQueueUrlResponse.builder().queueUrl(TEST_QUEUE_URL).build();

		when(mockConfig.getQueueName(any())).thenReturn(TEST_QUEUE);
		when(mockSQSClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(TEST_QUEUE).build())).thenReturn(queueResult);

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
		verify(mockSQSClient, never()).sendMessage(any(SendMessageRequest.class));
		
		// Trigger the after commit
		captorTransaction.getValue().afterCommit();
		
		verify(mockSQSClient).sendMessage(SendMessageRequest.builder()
				.queueUrl(TEST_QUEUE_URL)
				.messageBody(StatisticsMonthlyUtils.buildNotificationBody(objectType, month))
				.build());

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
		
		verify(mockSQSClient, never()).sendMessage(any(SendMessageRequest.class));

	}

	
}
