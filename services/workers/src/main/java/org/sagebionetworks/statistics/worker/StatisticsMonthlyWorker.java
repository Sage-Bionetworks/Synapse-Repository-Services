package org.sagebionetworks.statistics.worker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyManager;
import org.sagebionetworks.workers.util.aws.message.MessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.sqs.model.Message;

/**
 * The worker process a specific month (specified in the received message) worth of data for a given object type
 * 
 * @author Marco
 *
 */
public class StatisticsMonthlyWorker implements MessageDrivenRunner {
	
	private static final Logger LOG = LogManager.getLogger(StatisticsMonthlyWorker.class);
	
	private StatisticsMonthlyManager manager;
	
	@Autowired
	public StatisticsMonthlyWorker(StatisticsMonthlyManager manager) {
		this.manager = manager;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {
		LOG.info("Message received: " + message);
		/**
		 * TODO:
		 * - Decode the message and extract the month and object type to process
		 * - Invoke the project statistics manager to compute the monthly statistics
		 * - Set the given month to AVAILABLE or FAILED
		 */
	}

}
