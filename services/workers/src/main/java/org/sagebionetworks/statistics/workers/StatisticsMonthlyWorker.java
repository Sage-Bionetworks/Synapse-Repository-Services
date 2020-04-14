package org.sagebionetworks.statistics.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyManager;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyProcessNotification;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
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

	private WorkerLogger workerLogger;

	@Autowired
	public StatisticsMonthlyWorker(StatisticsMonthlyManager manager, WorkerLogger workerLogger) {
		this.manager = manager;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message) throws RecoverableMessageException, Exception {

		try {
			String messageBody = message.getBody();

			if (LOG.isDebugEnabled()) {
				LOG.debug("Process notification received: " + messageBody);
			}

			StatisticsMonthlyProcessNotification notification = StatisticsMonthlyUtils.fromNotificationBody(messageBody);

			if (LOG.isDebugEnabled()) {
				LOG.debug("Proccessing months {} for object type {}...", notification.getMonth(), notification.getObjectType());
			}

			manager.processMonth(notification.getObjectType(), notification.getMonth(), progressCallback);
		} catch (Throwable e) {
			LOG.error(e.getMessage(), e);

			boolean willRetry = false;
			// Sends a fail metric for cloud watch
			workerLogger.logWorkerFailure(StatisticsMonthlyWorker.class.getName(), e, willRetry);
		}
	}

}
