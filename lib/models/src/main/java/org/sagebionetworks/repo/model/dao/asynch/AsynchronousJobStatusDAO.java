package org.sagebionetworks.repo.model.dao.asynch;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Basic abstraction for the status of Asynchronous jobs.
 * @author jmhill
 *
 */
public interface AsynchronousJobStatusDAO {
	
	/**
	 * Start a new 
	 * @param startedByUserId The ID of the user that is starting the job.
	 * @param body
	 * @return
	 */
	public AsynchronousJobStatus startJob(Long startedByUserId, AsynchronousRequestBody body);
	
	/**
	 * Get the status of a job from its jobId.
	 * 
	 * @param jobId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public AsynchronousJobStatus getJobStatus(String jobId) throws DatastoreException, NotFoundException;
	
	/**
	 * Update the progress of a job.
	 * @param jobId
	 * @param progressCurrent
	 * @param progressTotal
	 * @return The new etag.
	 */
	public void updateJobProgress(String jobId, Long progressCurrent, Long progressTotal, String progressMessage);
	
	/**
	 * Set a job to failed.
	 * 
	 * @param jobId
	 * @param error
	 * @return
	 */
	public String setJobFailed(String jobId, Throwable error);
	
	/**
	 * Set a job to complete
	 * 
	 * @param body The final body of the job.
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public String setComplete(String jobId, AsynchronousResponseBody body) throws DatastoreException, NotFoundException;
	
	/**
	 * Clear all job status data from the database.
	 */
	public void truncateAllAsynchTableJobStatus();
	

}
