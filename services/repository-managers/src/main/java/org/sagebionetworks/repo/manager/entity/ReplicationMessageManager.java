package org.sagebionetworks.repo.manager.entity;

import java.util.List;

import org.sagebionetworks.repo.model.message.ChangeMessage;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

/**
 * Abstraction for the management of entity replication messages.
 *
 */
public interface ReplicationMessageManager {

	/**
	 * Publish the given ChangeMessagse to the entity replication queue. The
	 * worker listening to this queue will add, update, or delete entity
	 * replication as needed for each message.
	 * 
	 * @param toPush
	 * @throws JSONObjectAdapterException
	 */
	void pushChangeMessagesToReplicationQueue(List<ChangeMessage> toPush);

	/**
	 * Push the given container IDs to the Replication Delta queue. The worker
	 * listing to this queue will reconcile any differences between the truth
	 * and replication data for all of the given container IDs. .
	 * 
	 * @param scopeIds
	 */
	void pushContainerIdsToReconciliationQueue(List<Long> scopeIds);
	
	/**
	 * Get the approximate age of the oldest message on the queue.
	 * @return
	 */
	long getApproximateNumberOfMessageOnReplicationQueue();

}
