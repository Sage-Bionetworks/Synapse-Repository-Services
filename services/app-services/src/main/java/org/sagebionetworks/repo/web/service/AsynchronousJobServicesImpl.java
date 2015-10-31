package org.sagebionetworks.repo.web.service;

import java.lang.reflect.InvocationTargetException;

import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.manager.asynch.AsynchJobStatusManager;
import org.sagebionetworks.repo.model.AsynchJobFailedException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NotReadyException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.asynch.AsynchJobState;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousRequestBody;
import org.sagebionetworks.repo.model.table.QueryResultBundle;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation.
 * 
 * @author John
 * 
 */
public class AsynchronousJobServicesImpl implements AsynchronousJobServices {

	@Autowired
	private UserManager userManager;
	@Autowired
	private AsynchJobStatusManager asynchJobStatusManager;

	@Override
	public AsynchronousJobStatus startJob(Long userId, AsynchronousRequestBody body) throws NotFoundException {
		if (userId == null) {
			throw new IllegalArgumentException("UserId cannot be null");
		}
		if (body == null) {
			throw new IllegalArgumentException("Body cannot be null");
		}
		UserInfo user = userManager.getUserInfo(userId);
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

		if (jobStatus.getJobState() == AsynchJobState.FAILED) {
			if (jobStatus.getException() != null) {
				Throwable exception = null;
				try {
					@SuppressWarnings("unchecked")
					Class<Throwable> exceptionClass = (Class<Throwable>) Class.forName(jobStatus.getException());
					exception = exceptionClass.getConstructor(String.class).newInstance(jobStatus.getErrorMessage());
				} catch (Throwable t) {
					// ignore, just throw async job failed exception on any failure in trying to get a better exception
					// here
				}
				if (exception != null) {
					throw exception;
				}
			}
			throw new AsynchJobFailedException(jobStatus);
		}
		if (jobStatus.getJobState() == AsynchJobState.PROCESSING) {
			throw new NotReadyException(jobStatus);
		}
		return jobStatus;
	}
}
