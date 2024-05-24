package org.sagebionetworks.ses.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.ses.SESNotificationManager;
import org.sagebionetworks.repo.model.ses.SESJsonNotification;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.worker.TypedMessageDrivenRunner;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.model.Message;

@Service
public class SESNotificationWorker implements TypedMessageDrivenRunner<SESJsonNotification> {

	private static final Logger LOG = LogManager.getLogger(SESNotificationWorker.class);

	private SESNotificationManager notificationManager;
	private WorkerLogger workerLogger;

	@Autowired
	public SESNotificationWorker(SESNotificationManager notificationManager, WorkerLogger workerLogger) {
		this.notificationManager = notificationManager;
		this.workerLogger = workerLogger;
	}
	
	@Override
	public Class<SESJsonNotification> getObjectClass() {
		return SESJsonNotification.class;
	}

	@Override
	public void run(ProgressCallback progressCallback, Message message, SESJsonNotification notification) throws RecoverableMessageException, Exception {
		try {
			notificationManager.processMessage(notification, message.getBody());
		} catch (Throwable e) {

			LOG.error("Cannot process message " + message.getBody() + ": " + e.getMessage(), e);

			boolean willRetry = false;

			// Sends a fail metric for cloud watch
			workerLogger.logWorkerFailure(SESNotificationWorker.class.getName(), e, willRetry);
		}

	}

}
