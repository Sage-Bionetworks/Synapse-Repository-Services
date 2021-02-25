package org.sagebionetworks.repo.model.dbo.dao.files;

import java.util.Optional;

/**
 * Data access layer for handle the file handle association scanner status
 *
 */
public interface FilesScannerStatusDao {

	/**
	 * Creates a new job with the given number of jobs count
	 * 
	 * @param jobsCount
	 * @return The newly created status for the job
	 */
	DBOFilesScannerStatus create(long jobsCount);
	
	/**
	 * 
	 * @param id
	 * @return The status with the given id
	 */
	DBOFilesScannerStatus get(long id);
	
	/**
	 * Increases the number of completed (sub) jobs and updates the status with the given id
	 * 
	 * @param id
	 * @return The updated status
	 */
	DBOFilesScannerStatus increaseJobCompletedCount(long id);
		
	/**
	 * @return Return the status of the latest job if any
	 */
	Optional<DBOFilesScannerStatus> getLatest();
	
	
	// For testing
	void truncateAll();
	
}
