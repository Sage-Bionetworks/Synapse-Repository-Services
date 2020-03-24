package org.sagebionetworks.repo.web.service;

import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Abstraction for a AsynchronousJobServices
 * 
 * @author John
 *
 */
public interface AsynchronousJobServices {

	/**
	 * Launch a new job.
	 * 
	 * @param accessToken
	 * @param body
	 * @return
	 * @throws NotFoundException 
	 */
	AsynchronousJobStatus startJob(String accessToken, AsynchronousRequestBody body) throws NotFoundException, UnauthorizedException;
	
	/**
	 * Get the status for an existing job.
	 * 
	 * @param accessToken
	 * @param jobId
	 * @return
	 * @throws NotFoundException
	 * @throws NotReadyException
	 * @throws AsynchJobFailedException
	 */
	AsynchronousJobStatus getJobStatus(String accessToken, String jobId) throws NotFoundException;

	/**
	 * Stop an existing job.
	 * 
	 * @param accessToken
	 * @param jobId
	 * @return
	 * @throws NotFoundException
	 * @throws NotReadyException
	 * @throws AsynchJobFailedException
	 */
	void cancelJob(String accessToken, String jobId) throws NotFoundException;

	/**
	 * Get the status for an existing job and throw exceptions on error and not done.
	 * 
	 * @param accessToken
	 * @param jobId
	 * @return
	 * @throws Throwable 
	 */
	AsynchronousJobStatus getJobStatusAndThrow(String accessToken, String jobId) throws Throwable;
}
