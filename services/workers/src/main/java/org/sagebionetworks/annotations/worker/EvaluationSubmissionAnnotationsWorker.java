package org.sagebionetworks.annotations.worker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.SubmissionStatusAnnotationsAsyncManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.TransientDataAccessException;

/**
 * The worker that processes messages for SubmissionStatus Annotations jobs.
 *
 */
public class EvaluationSubmissionAnnotationsWorker implements ChangeMessageDrivenRunner {
	
	static private Log log = LogFactory.getLog(EvaluationSubmissionAnnotationsWorker.class);
	
	@Autowired
	private SubmissionStatusAnnotationsAsyncManager ssAsyncMgr;
	@Autowired
	private WorkerLogger workerLogger;

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage change)
			throws RecoverableMessageException, Exception {
		// We only care about Submission messages here
		if (ObjectType.EVALUATION_SUBMISSIONS == change.getObjectType()) {
			try {
				// Is this a create, update, or delete?
				if (ChangeType.CREATE == change.getChangeType()) {
					ssAsyncMgr.createEvaluationSubmissionStatuses(change.getObjectId());
				} else if (ChangeType.UPDATE == change.getChangeType()) {
					// update
					ssAsyncMgr.updateEvaluationSubmissionStatuses(change.getObjectId());
				} else if(ChangeType.DELETE == change.getChangeType()) {
					// delete
					ssAsyncMgr.deleteEvaluationSubmissionStatuses(change.getObjectId());
				} else {
					throw new IllegalArgumentException("Unknown change type: "+change.getChangeType());
				}
			} catch (TransientDataAccessException e) {
				log.info("Intermittent error in AnnotationsWorker: "+e.getMessage()+". Will retry");
				workerLogger.logWorkerFailure(this.getClass(), change, e, true);
				throw new RecoverableMessageException();
			} catch (Throwable e) {
				// Something went wrong and we did not process the message.  By default we remove the message from the queue.
				log.error("Failed to process message", e);
				workerLogger.logWorkerFailure(this.getClass(), change, e, false);
			}
		}
	}

}
