package org.sagebionetworks.repo.model.dbo.dao.table;

import org.sagebionetworks.repo.model.entity.IdAndVersion;

public interface ViewSnapshotDao {

	/**
	 * Create a record of a view snapshot stored in S3.
	 * 
	 * @param userId
	 * @param resultingIdAndVersion
	 * @param bucket
	 * @param key
	 */
	ViewSnapshot createSnapshot(ViewSnapshot snapshot);

	/**
	 * Get the snapshot information for the given ID and version. Note: version is
	 * required.
	 * 
	 * @param idAndVersion
	 * @return
	 */
	ViewSnapshot getSnapshot(IdAndVersion idAndVersion);

	/**
	 * Truncate all snapshots.
	 */
	void truncateAll();

}
