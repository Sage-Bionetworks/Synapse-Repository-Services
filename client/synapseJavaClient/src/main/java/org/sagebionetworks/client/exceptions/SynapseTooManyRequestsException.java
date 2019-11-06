package org.sagebionetworks.client.exceptions;

import org.sagebionetworks.repo.model.ErrorResponseCode;

/**
 * Exception throw for HTTP status code 429.
 *
 */
public class SynapseTooManyRequestsException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	//apache's HttpStatus is outdated and does not have the 429 error code included
	public static final int TOO_MANY_REQUESTS_STATUS_CODE = 429;

	public SynapseTooManyRequestsException() {
		super();
	}

	public SynapseTooManyRequestsException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseTooManyRequestsException(String message) {
		super(message);
	}

	public SynapseTooManyRequestsException(Throwable cause) {
		super(cause);
	}

	public SynapseTooManyRequestsException(String message, ErrorResponseCode errorResponseCode) {
		super(message, errorResponseCode);
	}
}