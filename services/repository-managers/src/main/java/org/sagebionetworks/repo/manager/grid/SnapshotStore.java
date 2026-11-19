package org.sagebionetworks.repo.manager.grid;

import java.io.File;
import java.time.Instant;

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

	/**
	 * Deletes the rows and files of a session's snapshots that a snapshot created at or before the given time
	 * supersedes. The latest snapshot is never deleted. Deletes at most a batch per call; older snapshots beyond it
	 * are left for the next call.
	 *
	 * @param sessionId    The grid session ID
	 * @param supersededBy Only snapshots superseded by a snapshot created at or before this time are deleted
	 * @return The number of snapshots deleted
	 */
	int deleteSupersededSnapshots(String sessionId, Instant supersededBy);

}
