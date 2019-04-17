package org.sagebionetworks.repo.manager.entity;

import java.util.List;
import java.util.concurrent.Future;

/**
 * An Asynchronous abstraction for the {@linkplain ReplicationMessageManager}.
 *
 */
public interface ReplicationMessageManagerAsynch {

	/**
	 * Asynchronously push the provided container IDs to the replication
	 * reconciliation queue.
	 * {@linkplain ReplicationMessageManager#pushContainerIdsToReconciliationQueue(List)}
	 * This method can take time and the caller often does not need to wait for the
	 * results. See PLFM-4836.
	 * 
	 * @param scopeIds
	 * @return
	 */
	Future<Void> pushContainerIdsToReconciliationQueue(List<Long> scopeIds);

}
