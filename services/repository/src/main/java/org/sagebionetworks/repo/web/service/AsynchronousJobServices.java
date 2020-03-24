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
	 * @param userId
	 * @param body
	 * @return
	 * @throws NotFoundException 
	 */
	AsynchronousJobStatus startJob(Long userId, AsynchronousRequestBody body) throws NotFoundException, UnauthorizedException;
	
	/**
	 * Get the status for an existing job.
	 * 
	 * @param userId
	 * @param jobId
	 * @return
	 * @throws NotFoundException
	 * @throws NotReadyException
	 * @throws AsynchJobFailedException
	 */
	AsynchronousJobStatus getJobStatus(Long userId, String jobId) throws NotFoundException;

	/**
	 * Stop an existing job.
	 * 
	 * @param userId
	 * @param jobId
	 * @return
	 * @throws NotFoundException
	 * @throws NotReadyException
	 * @throws AsynchJobFailedException
	 */
	void cancelJob(Long userId, String jobId) throws NotFoundException;

	/**
	 * Get the status for an existing job and throw exceptions on error and not done.
	 * 
	 * @param userId
	 * @param jobId
	 * @return
	 * @throws Throwable 
	 */
	AsynchronousJobStatus getJobStatusAndThrow(Long userId, String jobId) throws Throwable;
}
