package org.sagebionetworks.repo.model.dao.asynch;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Basic abstraction for the status of Asynchronous jobs.
 * @author jmhill
 *
 */
public interface AsynchronousJobStatusDAO {
	
	/**
	 * Create a new status for a new job
	 * @param status
	 * @return
	 */
	public <T extends AsynchronousJobStatus> T startJob(T status);
	
	/**
	 * Get the status of a job from its jobId.
	 * 
	 * @param jobId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public <T extends AsynchronousJobStatus> T getJobStatus(String jobId, Class<? extends T> clazz) throws DatastoreException, NotFoundException;
	
	/**
	 * Update the progress of a job.
	 * @param jobId
	 * @param progressCurrent
	 * @param progressTotal
	 * @return The new etag.
	 */
	public String updateJobProgress(String jobId, Long progressCurrent, Long progressTotal, String progressMessage);
	
	/**
	 * Set a job to failed.
	 * 
	 * @param jobId
	 * @param error
	 * @return
	 */
	public String setJobFailed(String jobId, Throwable error);
	
	/**
	 * Clear all job status data from the database.
	 */
	public void truncateAllAsynchTableJobStatus();

}
