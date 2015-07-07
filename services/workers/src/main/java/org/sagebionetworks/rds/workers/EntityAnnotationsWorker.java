package org.sagebionetworks.rds.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.model.AsynchronousDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.sagebionetworks.workers.util.progress.ProgressCallback;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The worker that processes messages for RDS asynchronous jobs.
 * 
 * @author jmhill
 * 
 */
public class EntityAnnotationsWorker implements ChangeMessageDrivenRunner {

	static private Logger log = LogManager.getLogger(EntityAnnotationsWorker.class);

	@Autowired
	AsynchronousDAO asynchronousDAO;

	@Autowired
	WorkerLogger workerLogger;

	@Override
	public void run(ProgressCallback<ChangeMessage> progressCallback, ChangeMessage change)
			throws RecoverableMessageException, Exception {
		// Extract the ChangeMessage
		// We only care about entity messages here
		if (ObjectType.ENTITY == change.getObjectType()) {
			try {
				// Is this a create update or delete?
				if (ChangeType.CREATE == change.getChangeType()) {
					// create
					asynchronousDAO.createEntity(change.getObjectId());
				} else if (ChangeType.UPDATE == change.getChangeType()) {
					// update
					asynchronousDAO.updateEntity(change.getObjectId());
				} else if (ChangeType.DELETE == change.getChangeType()) {
					// delete
					asynchronousDAO.deleteEntity(change.getObjectId());
				} else {
					throw new IllegalArgumentException("Unknown change type: "
							+ change.getChangeType());
				}
			} catch (NotFoundException e) {
				log.info("NotFound: "
						+ e.getMessage()
						+ ". The message will be returend as processed and removed from the queue");
			} catch (Throwable e) {
				// Something went wrong and we did not process the message.
				log.error("Failed to process message", e);
				workerLogger.logWorkerFailure(EntityAnnotationsWorker.class, change, e, true);
				throw new RecoverableMessageException();
			}
		}
	}

}
