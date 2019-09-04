package org.sagebionetworks.statistics.workers;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyManager;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQS;

/**
 * This worker runs periodically to watch the status of the monthly statistics in order to compute them if needed.
 * 
 * @author Marco
 */
public class StatisticsMonthlyStatusWatcherWorker implements ProgressingRunner {

	private static final Logger LOG = LogManager.getLogger(StatisticsMonthlyStatusWatcherWorker.class);
	static final String NOTIFICATION_QUEUE = "STATISTICS_MONTHLY";

	private StatisticsMonthlyManager statisticsManager;
	private AmazonSQS awsSQSClient;
	private String queueUrl;

	@Autowired
	public StatisticsMonthlyStatusWatcherWorker(StatisticsMonthlyManager statisticsManager, AmazonSQS awsSQSClient, StackConfiguration configuration) {
		this.statisticsManager = statisticsManager;
		this.awsSQSClient = awsSQSClient;
		this.queueUrl = awsSQSClient.getQueueUrl(configuration.getQueueName(NOTIFICATION_QUEUE)).getQueueUrl();
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		LOG.debug("Checking monthly statistics status...");

		for (StatisticsObjectType objectType : StatisticsObjectType.values()) {
			List<YearMonth> unprocessedMonths = statisticsManager.getUnprocessedMonths(objectType);
			if (!unprocessedMonths.isEmpty()) {
				LOG.info("Found {} unprocessed months for object type {}", unprocessedMonths.size(), objectType);
				sendProcessNotification(objectType, unprocessedMonths);
			}
		}

		LOG.debug("Checking monthly statistics status...DONE");
	}

	private void sendProcessNotification(StatisticsObjectType objectType, List<YearMonth> months) {
		if (months.isEmpty()) {
			return;
		}

		List<String> notifications = months.stream()
				.map(month -> StatisticsMonthlyUtils.buildNotificationBody(objectType, month))
				.collect(Collectors.toList());

		notifications.forEach(notification -> 
			awsSQSClient.sendMessage(queueUrl, notification)
		);

	}

}
