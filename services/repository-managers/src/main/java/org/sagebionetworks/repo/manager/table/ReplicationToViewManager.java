package org.sagebionetworks.repo.manager.table;

import org.sagebionetworks.repo.model.table.ReplicatedEvent;

public interface ReplicationToViewManager {

	/**
	 * An event fired after an object as finished replicating.
	 * 
	 * @param event
	 */
	void objectReplicated(ReplicatedEvent event);

	/**
	 * Consumes all visible view updates and triggers each view to synchronize.
	 */
	void consumeVisibleViewUpdates();

}
