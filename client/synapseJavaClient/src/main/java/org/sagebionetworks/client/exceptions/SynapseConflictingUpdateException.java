package org.sagebionetworks.client.exceptions;

/**
 * Thrown when a conflict occurs on an update. This typically, means the object
 * that the caller is attempting to update has changed since it was last read.
 * The client is expected to get the latest value of the object, apply their
 * changes and then attempt the update again.
 * 
 */
public class SynapseConflictingUpdateException extends SynapseServerException {

	private static final long serialVersionUID = 1L;

	public SynapseConflictingUpdateException() {
		super();
	}

	public SynapseConflictingUpdateException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseConflictingUpdateException(String message) {
		super(message);
	}

	public SynapseConflictingUpdateException(Throwable cause) {
		super(cause);
	}

}
