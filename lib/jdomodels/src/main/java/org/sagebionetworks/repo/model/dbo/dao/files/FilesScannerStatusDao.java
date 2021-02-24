package org.sagebionetworks.repo.model.dbo.dao.files;

import java.util.Optional;

import org.sagebionetworks.repo.model.files.FilesScannerState;
import org.sagebionetworks.repo.model.files.FilesScannerStatus;

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
	FilesScannerStatus create(long jobsCount);
	
	/**
	 * 
	 * @param id
	 * @return The status with the given id
	 */
	FilesScannerStatus get(long id);
		
	/**
	 * @return Return the status of the latest job if any
	 */
	Optional<FilesScannerStatus> getLatest();
	
	/**
	 * Updates the state of the job with the given id
	 * 
	 * @param state The new state
	 * @return The updates status
	 */
	FilesScannerStatus setState(long id, FilesScannerState state);
	
	// For testing
	void truncateAll();
	
}
