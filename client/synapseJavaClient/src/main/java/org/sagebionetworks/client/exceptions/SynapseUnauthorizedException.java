package org.sagebionetworks.client.exceptions;

import org.sagebionetworks.repo.model.ErrorResponseCode;

/**
 * Exception throw for HTTP status code of 401.
 */
public class SynapseUnauthorizedException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	public SynapseUnauthorizedException() {
		super();
	}

	public SynapseUnauthorizedException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseUnauthorizedException(String message) {
		super(message);
	}

	public SynapseUnauthorizedException(String message, ErrorResponseCode errorResponseCode) {
		super(message, errorResponseCode);
	}

	public SynapseUnauthorizedException(Throwable cause) {
		super(cause);
	}

}
