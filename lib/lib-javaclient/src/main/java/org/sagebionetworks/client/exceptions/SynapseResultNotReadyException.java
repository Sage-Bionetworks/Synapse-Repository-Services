package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;
import org.sagebionetworks.repo.model.asynch.AsynchronousJobStatus;
import org.sagebionetworks.repo.model.table.TableStatus;

/**
 * This exception is thrown when an asynch call is running and the result is not yet ready
 * 
 */
public class SynapseResultNotReadyException extends SynapseServerException {

	private static final long serialVersionUID = 3245759164904230603L;

	private AsynchronousJobStatus jobStatus;

	public SynapseResultNotReadyException(AsynchronousJobStatus jobStatus) {
		super(HttpStatus.SC_ACCEPTED);
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
