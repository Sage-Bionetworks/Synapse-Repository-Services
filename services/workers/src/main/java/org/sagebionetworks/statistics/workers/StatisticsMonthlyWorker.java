package org.sagebionetworks.statistics.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.statistics.monthly.StatisticsMonthlyManager;
import org.sagebionetworks.repo.model.statistics.monthly.StatisticsMonthlyUtils;
import org.sagebionetworks.repo.model.statistics.monthly.notification.StatisticsMonthlyProcessNotification;
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
		String messageBody = message.getBody();
		
		LOG.debug("Process notification received: " + messageBody);
		
		StatisticsMonthlyProcessNotification notification = StatisticsMonthlyUtils.fromNotificationBody(messageBody);

		LOG.info("Proccessing months {} for object type {}...", notification.getMonth(), notification.getObjectType());
		if (manager.processMonth(notification.getObjectType(), notification.getMonth())) {			
			LOG.info("Proccessing months {} for object type {}...DONE", notification.getMonth(), notification.getObjectType());
		} else {
			LOG.info("Proccessing months {} for object type {}...DONE (Processing skipped)", notification.getMonth(), notification.getObjectType());
		}
	}

}
