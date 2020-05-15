package org.sagebionetworks.replication.workers;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.BatchChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.database.semaphore.LockReleaseFailedException;
import org.sagebionetworks.repo.manager.replication.ReplicationManager;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.workers.util.aws.message.RecoverableMessageException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DeadlockLoserDataAccessException;

import com.amazonaws.AmazonServiceException;

/**
 * This worker listens to entity change events and replicates the changes to the
 * index database. The replicated data supports both entity views and entity queries.
 * 
 * @author John
 *
 */
public class ObjectReplicationWorker implements BatchChangeMessageDrivenRunner {
	
	static private Logger log = LogManager.getLogger(ObjectReplicationWorker.class);
	
	@Autowired
	ReplicationManager replicationManager;
	
	@Autowired
	WorkerLogger workerLogger;

	@Override
	public void run(ProgressCallback progressCallback,
			List<ChangeMessage> messages) throws RecoverableMessageException,
			Exception {
		try {
			replicationManager.replicate(messages);
		} catch (RecoverableMessageException
				| LockReleaseFailedException
				| CannotAcquireLockException
				| DeadlockLoserDataAccessException
				| AmazonServiceException e) {
			handleRecoverableException(e);
		} catch (Exception e) {
			boolean willRetry = false;
			workerLogger.logWorkerFailure(
					ObjectReplicationWorker.class.getName(), e, willRetry);
			log.error("Failed while replicating:", e);
		}
	}
	
	/**
	 * Handle a Recoverable exception.
	 * @param exception
	 * @throws RecoverableMessageException
	 */
	private void handleRecoverableException(Exception exception) throws RecoverableMessageException{
		log.error("Failed while replicating. Will retry. Message: "+exception.getMessage());
		throw new RecoverableMessageException(exception);
	}
}
