package org.sagebionetworks.repo.manager.replication;

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
	 * Get the approximate age of the oldest message on the queue.
	 * @return
	 */
	long getApproximateNumberOfMessageOnReplicationQueue();

}
