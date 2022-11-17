package org.sagebionetworks.repo.model.dbo.dao.table;

import java.util.Optional;

import org.sagebionetworks.repo.model.entity.IdAndVersion;

public interface TableSnapshotDao {

	
	/**
	 * Create a record of a table snapshot stored in S3.
	 * @param snapshot
	 * @return
	 */
	TableSnapshot createSnapshot(TableSnapshot snapshot);

	/**
	 * Get the snapshot information for the given ID and version. Note: version is
	 * required.
	 * 
	 * @param idAndVersion
	 * @return
	 */
	Optional<TableSnapshot> getSnapshot(IdAndVersion idAndVersion);

	/**
	 * Truncate all snapshots.
	 */
	void truncateAll();

	/**
	 * Get the snapshot ID for the given table/version.
	 * 
	 * @param idAndVersion
	 * @return
	 */
	long getSnapshotId(IdAndVersion idAndVersion);

}
