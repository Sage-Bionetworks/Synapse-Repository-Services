package org.sagebionetworks.repo.manager.grid.internal.replica;

public interface GridSnapshotExportPublisher {

	/**
	 * Queue a request for the internal replica of the given session to export a snapshot. The request goes to its
	 * own queue, so an export neither waits for nor delays the replica's other messages, and requests for the same
	 * session are delivered one at a time.
	 *
	 * @param sessionId
	 */
	void publishExportRequest(String sessionId);

}
