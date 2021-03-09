package org.sagebionetworks.repo.model.dbo.dao.files;

import java.time.Instant;

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
	 * Checks if a job exist that has been modified within the last given number of days
	 * 
	 * @param daysNum The number of days
	 * @return True if a job that was last modified within the last given number of days
	 */
	boolean exists(int lastModifiedDaysInterval);
	
	// For testing
	void truncateAll();
	
	/**
	 * Set the updatedOn for the job with the given id to the given amount of days in the past
	 * 
	 * @param id
	 * @param int daysInThePast
	 */
	void setUpdatedOn(long id, int daysInThePast);
	
}
