package org.sagebionetworks.repo.model.dbo.dao.table;

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

}
