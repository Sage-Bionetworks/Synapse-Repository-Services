package org.sagebionetworks.client.exceptions;

import org.sagebionetworks.repo.model.ErrorResponseCode;

/**
 * Throw for HTTP status code of 404.
 *
 */
public class SynapseNotFoundException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	public SynapseNotFoundException() {
		super();
	}

	public SynapseNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseNotFoundException(String message) {
		super(message);
	}

	public SynapseNotFoundException(Throwable cause) {
		super(cause);
	}

	public SynapseNotFoundException(String message, ErrorResponseCode errorResponseCode) {
		super(message, errorResponseCode);
	}
}
