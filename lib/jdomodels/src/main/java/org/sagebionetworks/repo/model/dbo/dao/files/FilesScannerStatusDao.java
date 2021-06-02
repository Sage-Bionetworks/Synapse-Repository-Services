package org.sagebionetworks.repo.model.dbo.dao.files;

import java.util.Optional;

/**
 * Data access layer for handle the file handle association scanner status
 *
 */
public interface FilesScannerStatusDao {

	/**
	 * Creates a new job with a default count of 0 for started and completed jobs as well as for scanned associations
	 * 
	 * @return The newly created status for the job
	 */
	DBOFilesScannerStatus create();
	
	/**
	 * 
	 * @param id
	 * @return The status with the given id
	 */
	DBOFilesScannerStatus get(long id);
	
	/**
	 * Deletes the job status with the given id
	 * @param id The id of the status
	 */
	void delete(long id);
	
	/**
	 * Increases the number of completed (sub) jobs and updates the status with the given id
	 * 
	 * @param id
	 * @return The updated status
	 */
	DBOFilesScannerStatus increaseJobCompletedCount(long id, int scannedAssociations);
	
	/**
	 * Sets the number of jobs started for the job with the given id
	 * 
	 * @param id The id of the job
	 * @param jobsCount The jobs count
	 * @return The updated status
	 */
	DBOFilesScannerStatus setStartedJobsCount(long id, long jobsCount);
	
	/**
	 * @param id The id of the job
	 * @return True if a job with the given id exists
	 */
	boolean exist(long id);
	
	/**
	 * Checks if a job exist that has been modified within the last given number of days
	 * 
	 * @param daysNum The number of days
	 * @return True if a job that was last modified within the last given number of days
	 */
	boolean existsWithinLast(int lastModifiedDaysInterval);
	
	/**
	* @return Return the status of the latest job if any
	*/
	Optional<DBOFilesScannerStatus> getLatest();
	
	// For testing
	void truncateAll();
	
	/**
	 * Set the updatedOn for the job with the given id to the given amount of days in the past
	 * 
	 * @param id
	 * @param int daysInThePast
	 */
	void reduceUpdatedOnOfNumberOfDays(long id, int daysInThePast);
	
}
