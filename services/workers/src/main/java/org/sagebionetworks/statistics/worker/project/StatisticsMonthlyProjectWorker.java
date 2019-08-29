package org.sagebionetworks.statistics.worker.project;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;

import com.amazonaws.services.sqs.model.Message;

/**
 * The worker process a specific month (specified in the received message) worth of data for projects
 * 
 * @author Marco
 *
 */
public class StatisticsMonthlyProjectWorker implements MessageDrivenRunner {
	

	private static final Logger LOG = LogManager.getLogger(StatisticsMonthlyProjectWorker.class);

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		LOG.info("Message received: " + message);
		/**
		 * TODO:
		 * - Decode the message and extract the month to process
		 * - Set the given month in PROCESSING status
		 * - Invoke the project statistics manager to compute the monthly statistics
		 * - Set the given month to AVAILABLE or FAILED
		 */
	}

}
