package org.sagebionetworks.repo.model.dao.table;

import org.sagebionetworks.repo.model.table.AsynchTableJobStatus;

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
	public AsynchTableJobStatus crateJobStatus(AsynchTableJobStatus status);
	
	/**
	 * Get the status of a job from its jobId.
	 * 
	 * @param jobId
	 * @return
	 */
	public AsynchTableJobStatus getJobStatus(String jobId);
	
	/**
	 * Clear all job status data from the database.
	 */
	public void truncateAllAsynchTableJobStatus();

}
