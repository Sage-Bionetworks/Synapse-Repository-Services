package org.sagebionetworks.repo.model.dbo.dao.table;

import org.sagebionetworks.repo.model.entity.IdAndVersion;

public interface ViewSnapshotDao {

	
	/**
	 * Create a record of a view snapshot stored in S3.
	 * @param snapshot
	 * @return
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

	/**
	 * Get the snapshot ID for the given view/version.
	 * 
	 * @param idAndVersion
	 * @return
	 */
	long getSnapshotId(IdAndVersion idAndVersion);

}
