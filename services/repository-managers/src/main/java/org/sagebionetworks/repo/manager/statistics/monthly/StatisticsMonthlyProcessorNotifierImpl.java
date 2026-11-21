package org.sagebionetworks.repo.manager.statistics.monthly;

import java.time.YearMonth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.message.TransactionSynchronizationProxy;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;

import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Service
public class StatisticsMonthlyProcessorNotifierImpl implements StatisticsMonthlyProcessorNotifier {

	private static final Logger LOG = LogManager.getLogger(StatisticsMonthlyProcessorNotifierImpl.class);
	private static final String NOTIFICATION_QUEUE = "STATISTICS_MONTHLY";

	private TransactionSynchronizationProxy transactionSynchronization;
	private SqsClient awsSQSClient;
	private String queueUrl;

	@Autowired
	public StatisticsMonthlyProcessorNotifierImpl(TransactionSynchronizationProxy transactionSynchronization,
			StackConfiguration stackConfig, SqsClient awsSQSClient) {
		this.transactionSynchronization = transactionSynchronization;
		this.awsSQSClient = awsSQSClient;
		this.queueUrl = awsSQSClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(stackConfig.getQueueName(NOTIFICATION_QUEUE)).build()).queueUrl();

	}

	@Override
	public void sendStartProcessingNotification(StatisticsObjectType objectType, YearMonth month) {
		ValidateArgument.required(objectType, "objectType");
		ValidateArgument.required(month, "month");

		if (transactionSynchronization.isActualTransactionActive()) {
			transactionSynchronization.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Sending processing notification for object type {} and month {}", objectType, month);
					}
					awsSQSClient.sendMessage(SendMessageRequest.builder()
							.queueUrl(queueUrl)
							.messageBody(StatisticsMonthlyUtils.buildNotificationBody(objectType, month))
							.build());
				}
			});
		} else {
			throw new IllegalStateException("This method should be invoked withing a transaction context");
		}
	}
}
