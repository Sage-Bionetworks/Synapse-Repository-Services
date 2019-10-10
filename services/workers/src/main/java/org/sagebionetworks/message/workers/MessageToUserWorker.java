package org.sagebionetworks.message.workers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.MessageManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.ses.QuarantinedEmailException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The worker that processes messages sending messages to users
 * 
 */
public class MessageToUserWorker implements ChangeMessageDrivenRunner {

	static private Logger log = LogManager.getLogger(MessageToUserWorker.class);

	@Autowired
	private MessageManager messageManager;

	@Autowired
	private WorkerLogger workerLogger;

	@Override
	public void run(final ProgressCallback progressCallback, final ChangeMessage change)
			throws RecoverableMessageException, Exception {
		// We only care about MESSAGE messages here
		if (ObjectType.MESSAGE == change.getObjectType()) {
			try {
				List<String> errors;
				switch (change.getChangeType()) {
				case CREATE:
					errors = messageManager.processMessage(change.getObjectId(), progressCallback);
					break;
				default:
					throw new IllegalArgumentException("Unknown change type: "
							+ change.getChangeType());
				}

				if (errors.size() > 0) {
					messageManager.sendDeliveryFailureEmail(
							change.getObjectId(), errors);
				}

			} catch (QuarantinedEmailException e) {
				log.error("The message will be returned as processed and removed from the queue: " + e.getMessage(), e);
				workerLogger.logWorkerFailure(this.getClass(), change, e, false);
			} catch (NotFoundException e) {
				log.info("NotFound: "
						+ e.getMessage()
						+ ". The message will be returned as processed and removed from the queue");
				workerLogger.logWorkerFailure(this.getClass(), change, e, false);
			} catch (Throwable e) {
				// Something went wrong and we did not process the message
				log.error("Failed to process message", e);
				workerLogger.logWorkerFailure(this.getClass(), change, e, true);
				// This is the wrong thing to do with a throwable.
				throw new RecoverableMessageException();
			}
		}
	}

}
