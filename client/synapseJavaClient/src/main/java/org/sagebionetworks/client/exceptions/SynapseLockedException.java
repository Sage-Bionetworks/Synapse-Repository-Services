package org.sagebionetworks.client.exceptions;

import org.sagebionetworks.repo.model.ErrorResponseCode;

/**
 * Throw for HTTP status code of 423.
 *
 */
public class SynapseLockedException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	public SynapseLockedException() {
		super();
	}

	public SynapseLockedException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseLockedException(String message) {
		super(message);
	}

	public SynapseLockedException(Throwable cause) {
		super(cause);
	}

	public SynapseLockedException(String message, ErrorResponseCode errorResponseCode) {
		super(message, errorResponseCode);
	}
}
