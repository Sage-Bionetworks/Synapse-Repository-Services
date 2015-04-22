package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;

import com.google.common.base.Strings;

/**
 * This exception is thrown when an asynchronous result is failed.
 * 
 */
public class AsynchJobFailedException extends Exception {
	private static final long serialVersionUID = 3271255724245570890L;

	private AsynchronousJobStatus status;

	/**
	 * Create a AsynchJobFailedException always wraps an AsynchronousJobStatus
	 * 
	 * @param status
	 */
	public AsynchJobFailedException(AsynchronousJobStatus status) {
		super(extractMessage(status));
		this.status = status;
	}

	/**
	 * When this exception is thrown it will always include the status of the table.
	 * 
	 * @return
	 */
	public AsynchronousJobStatus getStatus() {
		return status;
	}

	private static String extractMessage(AsynchronousJobStatus status) {
		if (!Strings.isNullOrEmpty(status.getErrorMessage())) {
			return status.getErrorMessage();
		} else if (!Strings.isNullOrEmpty(status.getErrorDetails())) {
			return status.getErrorDetails();
		}
		return "Asynchronous job failed";
	}
}
