package org.sagebionetworks.repo.service;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobUtils;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousAdminRequestBody;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Basic implementation.
 * 
 * @author John
 * 
 */
@Service
public class AsynchronousJobServicesImpl implements AsynchronousJobServices {

	private UserManager userManager;
	private AsynchJobStatusManager asynchJobStatusManager;
	
	@Autowired
	public AsynchronousJobServicesImpl(UserManager userManager, AsynchJobStatusManager asynchJobStatusManager) {
		this.userManager = userManager;
		this.asynchJobStatusManager = asynchJobStatusManager;
	}

	@Override
	public AsynchronousJobStatus startJob(Long userId, AsynchronousRequestBody body) throws NotFoundException, UnauthorizedException {
		if (userId == null) {
			throw new IllegalArgumentException("UserId cannot be null");
		}
		if (body == null) {
			throw new IllegalArgumentException("Body cannot be null");
		}
		UserInfo user = userManager.getUserInfo(userId);
		if ((body instanceof AsynchronousAdminRequestBody) && (! user.isAdmin())) {
			throw new UnauthorizedException("Only an administrator may start this job.");
		}
		return asynchJobStatusManager.startJob(user, body);
	}

	@Override
	public AsynchronousJobStatus getJobStatus(Long userId, String jobId) throws NotFoundException {
		if (userId == null) {
			throw new IllegalArgumentException("UserId cannot be null");
		}
		if (jobId == null) {
			throw new IllegalArgumentException("JobId cannot be null");
		}

		UserInfo user = userManager.getUserInfo(userId);
		AsynchronousJobStatus jobStatus = asynchJobStatusManager.getJobStatus(user, jobId);
		return jobStatus;
	}

	@Override
	@Deprecated
	public void cancelJob(Long userId, String jobId) throws NotFoundException {
		if (userId == null) {
			throw new IllegalArgumentException("UserId cannot be null");
		}
		if (jobId == null) {
			throw new IllegalArgumentException("JobId cannot be null");
		}

		UserInfo user = userManager.getUserInfo(userId);
		asynchJobStatusManager.cancelJob(user, jobId);
	}

	@Override
	public AsynchronousJobStatus getJobStatusAndThrow(Long userId, String jobId) throws Throwable {
		if (userId == null) {
			throw new IllegalArgumentException("UserId cannot be null");
		}
		if (jobId == null) {
			throw new IllegalArgumentException("JobId cannot be null");
		}

		UserInfo user = userManager.getUserInfo(userId);
		AsynchronousJobStatus jobStatus = asynchJobStatusManager.getJobStatus(user, jobId);
		AsynchJobUtils.throwExceptionIfFailed(jobStatus);
		if (jobStatus.getJobState() == AsynchJobState.PROCESSING) {
			throw new NotReadyException(jobStatus);
		}
		return jobStatus;
	}
}
