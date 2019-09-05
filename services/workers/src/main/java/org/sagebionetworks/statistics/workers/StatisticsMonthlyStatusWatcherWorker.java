package org.sagebionetworks.statistics.workers;

import java.time.YearMonth;
import java.util.List;

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
	static final long PROCESSING_TIMEOUT = 10 * 60 * 1000; // 10 minutes of idle time before the processing is restarted
	static final String NOTIFICATION_QUEUE = "STATISTICS_MONTHLY";

	private StatisticsMonthlyManager statisticsManager;
	private AmazonSQS awsSQSClient;
	private String queueUrl;

	@Autowired
	public StatisticsMonthlyStatusWatcherWorker(StatisticsMonthlyManager statisticsManager, AmazonSQS awsSQSClient,
			StackConfiguration configuration) {
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
				submitProcessing(objectType, unprocessedMonths);
			}
		}

		LOG.debug("Checking monthly statistics status...DONE");
	}

	private void submitProcessing(StatisticsObjectType objectType, List<YearMonth> months) {
		months.forEach(month -> {
			if (statisticsManager.startProcessingMonth(objectType, month, PROCESSING_TIMEOUT)) {
				LOG.info("Sending processing notification for object type {} and month {}", objectType, month);
				sendNotification(objectType, month);
			} else {
				LOG.info("Skipping processing notification for object type {} and month {}", objectType, month);
			}
		});

	}

	private void sendNotification(StatisticsObjectType objectType, YearMonth month) {
		awsSQSClient.sendMessage(queueUrl, StatisticsMonthlyUtils.buildNotificationBody(objectType, month));
	}

}
