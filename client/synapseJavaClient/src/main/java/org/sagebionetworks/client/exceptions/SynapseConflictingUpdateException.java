package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;

/**
 * Thrown when a conflict occurs on an update. This typically, means the object
 * that the caller is attempting to update has changed since it was last read.
 * The client is expected to get the latest value of the object, apply their
 * changes and then attempt the update again.
 * 
 * @author jmhill
 * 
 */
public class SynapseConflictingUpdateException extends SynapseServerException {

	private static final long serialVersionUID = 1L;

	private static final int PRECONDITION_FAILED = HttpStatus.SC_PRECONDITION_FAILED;

	public SynapseConflictingUpdateException() {
		super(PRECONDITION_FAILED);
	}

	public SynapseConflictingUpdateException(String message, Throwable cause) {
		super(PRECONDITION_FAILED, message, cause);
	}

	public SynapseConflictingUpdateException(String message) {
		super(PRECONDITION_FAILED, message);
	}

	public SynapseConflictingUpdateException(Throwable cause) {
		super(PRECONDITION_FAILED, cause);
	}

}
