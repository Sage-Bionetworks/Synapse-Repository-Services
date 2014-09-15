package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;

/**
 * This exception is thrown when an asynchronous result is not ready yet.
 * 
 */
public class NotReadyException extends Exception {
	private static final long serialVersionUID = -6195866485604385780L;

	private AsynchronousJobStatus status;

	/**
	 * Create a NotReadyException always wraps an AsynchronousJobStatus
	 * 
	 * @param status
	 */
	public NotReadyException(AsynchronousJobStatus status) {
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
