package org.sagebionetworks.repo.manager.asynch;

import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.AuthorizationManagerUtil;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.StackStatusDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousResponseBody;
import org.sagebionetworks.repo.model.dao.asynch.AsynchronousJobStatusDAO;
import org.sagebionetworks.repo.model.status.StatusEnum;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class AsynchJobStatusManagerImpl implements AsynchJobStatusManager {
	
	private static final String JOB_ABORTED_MESSAGE = "Job aborted because the stack was not in: "+StatusEnum.READ_WRITE;

	@Autowired
	AsynchronousJobStatusDAO asynchJobStatusDao;
	@Autowired
	AuthorizationManager authorizationManager;
	@Autowired
	StackStatusDao stackStatusDao;
	@Autowired
	AsynchJobQueuePublisher asynchJobQueuePublisher;
	
	@Override
	public AsynchronousJobStatus getJobStatus(UserInfo userInfo, String jobId) throws DatastoreException, NotFoundException {
		if(userInfo == null) throw new IllegalArgumentException("UserInfo cannot be null");
		// Get the status
		AsynchronousJobStatus status = asynchJobStatusDao.getJobStatus(jobId);
		// Only the user that started a job can read it
		if(!authorizationManager.isUserCreatorOrAdmin(userInfo, status.getStartedByUserId().toString())){
			throw new UnauthorizedException("Only the user that created a job can access the job's status.");
		}
		// If a job is running and the stack is not in READ-WRITE mode then the job is failed.
		if(AsynchJobState.PROCESSING.equals(status.getJobState())){
			// Since the job is processing check the state of the stack.
			checkStackReadWrite();
		}
		return status;
	}

	@Override
	public AsynchronousJobStatus startJob(UserInfo user, AsynchronousRequestBody body) throws DatastoreException, NotFoundException {
		if(user == null) throw new IllegalArgumentException("UserInfo cannot be null");
		if(body == null) throw new IllegalArgumentException("Body cannot be null");
		// Dao does the rest.
		AsynchronousJobStatus status = asynchJobStatusDao.startJob(user.getId(), body);
		// publish a message to get the work started
		asynchJobQueuePublisher.publishMessage(status);
		return status;
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateJobProgress(String jobId, Long progressCurrent, Long progressTotal, String progressMessage) {
		// Progress can only be updated if the stack is in read-write mode.
		checkStackReadWrite();
		asynchJobStatusDao.updateJobProgress(jobId, progressCurrent, progressTotal, progressMessage);
	}

	/**
	 * If the stack is not in read-write mode an IllegalStateException will be thrown.
	 */
	private void checkStackReadWrite() {
		if(!StatusEnum.READ_WRITE.equals(stackStatusDao.getCurrentStatus())){
			throw new IllegalStateException(JOB_ABORTED_MESSAGE);
		}
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String setJobFailed(String jobId, Throwable error) {
		// We allow a job to fail even if the stack is not in read-write mode.
		return asynchJobStatusDao.setJobFailed(jobId, error);
	}


	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public String setComplete(String jobId, AsynchronousResponseBody body)
			throws DatastoreException, NotFoundException {
		// Job can only be completed if the stack is in read-write mode.
		checkStackReadWrite();
		return asynchJobStatusDao.setComplete(jobId, body);
	}

	@Override
	public void emptyAllQueues() {
		asynchJobQueuePublisher.emptyAllQueues();
	}
	
	

}
