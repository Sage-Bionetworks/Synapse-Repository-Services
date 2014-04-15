package org.sagebionetworks.repo.model.dao.table;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.table.AsynchTableJobStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Basic abstraction for the status of Asynchronous table jobs.
 * @author jmhill
 *
 */
public interface AsynchTableJobStatusDAO {
	
	/**
	 * 
	 * @param status
	 * @return
	 */
	public AsynchTableJobStatus starteNewUploadJobStatus(Long userId, Long fileHandleId, String tableId);
	
	/**
	 * Get the status of a job from its jobId.
	 * 
	 * @param jobId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public AsynchTableJobStatus getJobStatus(String jobId) throws DatastoreException, NotFoundException;
	
	/**
	 * Update the progress of a job.
	 * @param jobId
	 * @param progressCurrent
	 * @param progressTotal
	 * @return The new etag.
	 */
	public String updateProgress(String jobId, Long progressCurrent, Long progressTotal, String progressMessage);
	
	/**
	 * Clear all job status data from the database.
	 */
	public void truncateAllAsynchTableJobStatus();

}
