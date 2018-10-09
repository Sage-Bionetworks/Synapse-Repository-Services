package org.sagebionetworks.repo.model.dao.asynch;

import java.util.List;

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
	 * @param requestHash For jobs that are cacheable a hash of the job body + object etag will be included.
	 * This hash can then be used to find existing jobs with the same hash.
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
	 * Set a job to canceling.
	 * 
	 * @param jobId
	 * @param error
	 * @return
	 */
	public void setJobCanceling(String jobId);

	/**
	 * Set a job to complete
	 * 
	 * @param body The final body of the job.
	 * @param requestHash Set the request hash for cacheable requests.  This hash is used to find cached jobs for the same
	 * request from the same user.  See: {@link #findCompletedJobStatus(String, Long)}.
	 * 
	 * @return runtime MS.
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public long setComplete(String jobId, AsynchronousResponseBody body, String requestHash) throws DatastoreException, NotFoundException;

	
	/**
	 * Clear all job status data from the database.
	 */
	public void truncateAllAsynchTableJobStatus();

	/**
	 * Find a job status for the request hash and user id and with a jobState=COMPLETE. If no such job exists, then null will be returned.
	 * 
	 * @param requestHash
	 * @param objectEtag
	 * @return A list of all AsynchronousJobStatus with the given request hash and userId.  Will return an empty list if there are no matches.
	 * 
	 * Note: This method will never return more than five results.
	 */
	public List<AsynchronousJobStatus> findCompletedJobStatus(String requestHash, Long userId);
	

}
