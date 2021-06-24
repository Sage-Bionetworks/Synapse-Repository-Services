package org.sagebionetworks.repo.manager.asynch;

import java.io.IOException;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for an AsynchronousJobStatus CRUD.
 * 
 * @author John
 *
 */
public interface AsynchJobStatusManager {
	
	/**
	 * Start a new job.
	 * 
	 * @param user
	 * @param body
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public AsynchronousJobStatus startJob(UserInfo user, AsynchronousRequestBody body) throws DatastoreException, NotFoundException;
	
	/**
	 * Get the current status of a job.
	 * 
	 * @param user
	 * @param jobId
	 * @return
	 * @throws NotFoundException 
	 * @throws DatastoreException 
	 */
	public AsynchronousJobStatus getJobStatus(UserInfo user, String jobId) throws DatastoreException, NotFoundException;
	
	/**
	 * Lookup an job status using only the ID.
	 * This method is for server-side calls only.  For user requests use {@link #getJobStatus(UserInfo, String)}
	 * to ensure a security check is used.
	 * @param jobId
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public AsynchronousJobStatus lookupJobStatus(String jobId) throws DatastoreException, NotFoundException;

	/**
	 * Stop a job.
	 * 
	 * @param user
	 * @param jobId
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	public void cancelJob(UserInfo user, String jobId) throws DatastoreException, NotFoundException;
	
	/**
	 * Update the progress of a job.
	 * This method should only be called by a worker.
	 * 
	 * @param jobId
	 * @param progressCurrent
	 * @param progressTotal
	 * @param progressMessage
	 * @return
	 */
	public void updateJobProgress(String jobId, Long progressCurrent, Long progressTotal, String progressMessage);
	
	
	/**
	 * Set a job to failed.
	 * This method should only be called by a worker.
	 * 
	 * @param jobId
	 * @param error
	 * @return
	 */
	public String setJobFailed(String jobId, Throwable error);
	
	/**
	 * Set a job to canceled. This method should only be called by a worker.
	 * 
	 * @param jobId
	 * @param error
	 * @return
	 */
	public void setJobCanceling(String jobId);

	/**
	 * Set a job to complete. This method should only be called by a worker.
	 * 
	 * @param body The final body of the job.
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws IOException 
	 */
	public void setComplete(String jobId, AsynchronousResponseBody body) throws DatastoreException, NotFoundException, IOException;

	public void emptyAllQueues();
}
