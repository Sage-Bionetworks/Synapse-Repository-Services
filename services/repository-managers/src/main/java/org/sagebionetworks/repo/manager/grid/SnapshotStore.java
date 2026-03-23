package org.sagebionetworks.repo.manager.grid;

import java.io.File;

import org.sagebionetworks.repo.model.grid.ClockTable;

public interface SnapshotStore {

	/**
	 * Uploads a snapshot file to S3 and saves it to the database with the provided metadata.
	 *
	 * @param sessionId The grid session ID
	 * @param clockTable The clock table representing the state of the snapshot
	 * @param createdByUserId The user who created the snapshot
     * @param snapshotFile The snapshot file to upload
	 */
	void saveSnapshot(String sessionId, ClockTable clockTable, Long createdByUserId, File snapshotFile);

}
