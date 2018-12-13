package org.sagebionetworks.client.exceptions;

import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;

/**
 * This exception is thrown when an asynch call is running and the result is not yet ready
 * 
 * Throw for HTTP status code of 202.
 * 
 */
public class SynapseResultNotReadyException extends SynapseServerException {

	private static final long serialVersionUID = 3245759164904230603L;

	private AsynchronousJobStatus jobStatus;

	public SynapseResultNotReadyException(AsynchronousJobStatus jobStatus) {
		super();
		this.jobStatus = jobStatus;
	}

	/**
	 * When a TableUnavilableException is thrown from the server, the status of the table is always included.
	 * 
	 * @return
	 */
	public AsynchronousJobStatus getJobStatus() {
		return jobStatus;
	}
}
