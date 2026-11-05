package org.sagebionetworks.repo.manager.grid.internal.replica;

import org.sagebionetworks.repo.model.grid.GridConnectionInfo;

public interface GridSnapshotExportPublisher {

	/**
	 * Queue a request for the internal replica of the given connection to export a snapshot. Requests for the same
	 * session run one at a time, separately from the replica's other messages, so an export neither waits for nor
	 * delays the patches the replica applies.
	 *
	 * @param connection
	 */
	void publishExportRequest(GridConnectionInfo connection);

}
