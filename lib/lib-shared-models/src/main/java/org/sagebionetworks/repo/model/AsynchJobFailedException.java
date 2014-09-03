package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;

/**
 * This exception is thrown when an asynchronous result is failed.
 * 
 */
public class AsynchJobFailedException extends Exception {
	private static final long serialVersionUID = 3271255724245570890L;

	private AsynchronousJobStatus status;

	/**
	 * Create a TableUnavilableException always wraps a TableStatus
	 * 
	 * @param status
	 */
	public AsynchJobFailedException(AsynchronousJobStatus status) {
		super(status.getErrorMessage());
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
}
