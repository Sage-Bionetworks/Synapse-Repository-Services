package org.sagebionetworks.replication.workers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.asynchronous.workers.changes.ChangeMessageDrivenRunner;
import org.sagebionetworks.cloudwatch.WorkerLogger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.repo.manager.replication.ReplicationManager;
import org.sagebionetworks.repo.manager.replication.ReplicationMessageManager;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.entity.IdAndVersion;
import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Object replication data is normally kept in-synch with the truth by the
 * {@link ObjectReplicationWorker} by listening to object change events.
 * However, message delivery is not guaranteed so a secondary process is needed
 * to ensure the entity replication data is kept up-to-date with the truth.
 * </p>
 * <p>
 * This worker reconciles discrepancies between the truth and the replicated data for
 * a given list of container IDs. This worker is driven by query events. Each time a query
 * is executed against the object replication data, an event is generated that
 * includes the container IDs involved in the query. For example, when a query
 * is executed against a table view, an event is generated that includes that
 * IDs of the view's fully expanded scope. This worker 'listens' to these events
 * and performs delta checking for each container ID that has not been checked
 * in the past 1000 minutes. When deltas are detected, change events are generated
 * to trigger the {@link ObjectReplicationWorker} to create, update, or deleted
 * object replicated data as needed.
 * </p>
 */
public class ObjectReplicationReconciliationWorker implements ChangeMessageDrivenRunner {

	static final int MAX_MESSAGE_TO_RUN_RECONCILIATION = 100;

	static private Logger log = LogManager
			.getLogger(ObjectReplicationReconciliationWorker.class);

	@Autowired
	private WorkerLogger workerLogger;
	
	@Autowired
	private ReplicationMessageManager replicationMessageManager;
	
	@Autowired
	private ReplicationManager replicationManager;

	@Override
	public void run(ProgressCallback progressCallback, ChangeMessage message) {
		try {
			if(!ObjectType.ENTITY_VIEW.equals(message.getObjectType())){
				// ignore non-view messages
				return;
			}
			/*
			 * Entity replication reconciliation is expensive. It serves as a fail-safe for
			 * lost messages and is not a replacement for the normal replication process.
			 * Therefore, reconciliation should only be run during quite periods. See:
			 * PLFM-5101 and PLFM-5051. The approximate number of message on the replication
			 * queue is used to determine if this is a quite period.
			 */
			long messagesOnQueue = this.replicationMessageManager.getApproximateNumberOfMessageOnReplicationQueue();
			if (messagesOnQueue > MAX_MESSAGE_TO_RUN_RECONCILIATION) {
				// do nothing during busy periods.
				log.info("Ignoring reconciliation request since the replication queue has: " + messagesOnQueue
						+ " messages");
				return;
			}
			
			// Get all of the containers for the given view.
			IdAndVersion idAndVersion = IdAndVersion.parse(message.getObjectId());
			
			replicationManager.reconcile(idAndVersion);

		} catch (Throwable cause) {
			log.error("Failed:", cause);
			boolean willRetry = false;
			workerLogger.logWorkerFailure(
					ObjectReplicationReconciliationWorker.class.getName(), cause,
					willRetry);
		}
	}

}
