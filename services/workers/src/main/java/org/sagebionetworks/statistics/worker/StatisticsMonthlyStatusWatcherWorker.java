package org.sagebionetworks.statistics.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressingRunner;
import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyManager;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.AmazonSQS;

/**
 * This worker runs periodically to watch the status of the monthly statistics in order to compute
 * them if needed.
 * 
 * @author Marco
 */
public class StatisticsMonthlyStatusWatcherWorker implements ProgressingRunner {
	
	private static final Logger LOG = LogManager.getLogger(StatisticsMonthlyStatusWatcherWorker.class);
	
	private StatisticsMonthlyManager statisticsManager;
	private AmazonSQS awsSQSClient;
	
	@Autowired
	public StatisticsMonthlyStatusWatcherWorker(StatisticsMonthlyManager statisticsManager, AmazonSQS awsSQSClient) {
		this.statisticsManager = statisticsManager;
		this.awsSQSClient = awsSQSClient;
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		LOG.info("Checking monthly statistics status...");
		/**
		 *  TODO:
		 *  - Call the manager to get the unprocessed months for a each object type (use a provider)
		 *  - Send a message for each unprocessed months on the queue for the object type
		 */		
		LOG.info("Checking monthly statistics status...DONE");
	}

}
