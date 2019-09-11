package org.sagebionetworks.statistics.workers;

import java.time.YearMonth;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyManager;
import org.sagebionetworks.repo.model.statistics.StatisticsObjectType;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This worker runs periodically to watch the status of the monthly statistics in order to compute them if needed.
 * 
 * @author Marco
 */
public class StatisticsMonthlyStatusWatcherWorker implements ProgressingRunner {

	private static final long PROCESSING_TIMEOUT = 10 * 60 * 1000; // 10 minutes of idle time before the processing is restarted
	private static final Logger LOG = LogManager.getLogger(StatisticsMonthlyStatusWatcherWorker.class);

	private StatisticsMonthlyManager statisticsManager;
	private WorkerLogger workerLogger;

	@Autowired
	public StatisticsMonthlyStatusWatcherWorker(StatisticsMonthlyManager statisticsManager, WorkerLogger workerLogger) {
		this.statisticsManager = statisticsManager;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {

		LOG.debug("Checking monthly statistics status...");

		for (StatisticsObjectType objectType : StatisticsObjectType.values()) {
			try {
				List<YearMonth> unprocessedMonths = statisticsManager.getUnprocessedMonths(objectType);

				if (unprocessedMonths.isEmpty()) {
					continue;
				}

				LOG.info("Found {} unprocessed months for object type {}", unprocessedMonths.size(), objectType);

				submitProcessing(objectType, unprocessedMonths);
			} catch (Throwable e) {
				LOG.error("Could not process object " + objectType + ": " + e.getMessage(), e);
				boolean willRetry = false;
				// Sends a fail metric for cloud watch
				workerLogger.logWorkerFailure(StatisticsMonthlyStatusWatcherWorker.class.getName(), e, willRetry);
			}
		}

		LOG.debug("Checking monthly statistics status...DONE");

	}

	private void submitProcessing(StatisticsObjectType objectType, List<YearMonth> months) {
		months.forEach(month -> {

			boolean processingStarted = statisticsManager.startProcessingMonth(objectType, month, PROCESSING_TIMEOUT);

			if (processingStarted) {
				LOG.info("Processing request sent for object type {} and month {}", objectType, month);
			} else {
				LOG.info("Skipping processing request for object type {} and month {}", objectType, month);
			}
		});

	}

}
