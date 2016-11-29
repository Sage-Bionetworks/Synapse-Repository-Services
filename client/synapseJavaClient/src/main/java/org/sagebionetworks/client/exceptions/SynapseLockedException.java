package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;


public class SynapseLockedException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	private static final int LOCKED_STATUS_CODE = HttpStatus.SC_LOCKED;

	public SynapseLockedException() {
		super(LOCKED_STATUS_CODE);
	}

	public SynapseLockedException(String message, Throwable cause) {
		super(LOCKED_STATUS_CODE, message, cause);
	}

	public SynapseLockedException(String message) {
		super(LOCKED_STATUS_CODE, message);
	}

	public SynapseLockedException(Throwable cause) {
		super(LOCKED_STATUS_CODE, cause);
	}

}
