package org.sagebionetworks.discussion.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.repo.manager.discussion.DiscussionSearchIndexManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.util.progress.ProgressCallback;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * The worker listens for changes on threads and replies to update the disucssion search index
 */
public class DiscussionSearchIndexWorker implements ChangeMessageDrivenRunner {
	
	private final static Logger LOG = LogManager.getLogger(DiscussionSearchIndexWorker.class);
	
	private DiscussionSearchIndexManager manager;
	private WorkerLogger workerLogger;

	@Autowired
	public DiscussionSearchIndexWorker(DiscussionSearchIndexManager manager, WorkerLogger workerLogger) {
		this.manager = manager;
		this.workerLogger = workerLogger;
	}

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message) throws RecoverableMessageException, Exception {
		try {			
			ObjectType objectType = message.getObjectType();
			Long objectId = Long.valueOf(message.getObjectId());

			switch (objectType) {
			case THREAD:
				manager.processThreadChange(objectId);
				break;
			case REPLY:
				manager.processReplyChange(objectId);
				break;
			default:
				break;
			}
		} catch (RecoverableMessageException ex) {
			workerLogger.logWorkerFailure(DiscussionSearchIndexWorker.class, message, ex, true);
			throw ex;
		} catch (Throwable ex) {
			LOG.error("Worker failed: " + ex.getMessage(), ex);
			workerLogger.logWorkerFailure(DiscussionSearchIndexWorker.class, message, ex, false);
		}

	}

}
