package org.sagebionetworks.client.exceptions;

import org.sagebionetworks.repo.model.ErrorResponseCode;

/**
 * Exception throw for HTTP status code of 403.
 *
 */
public class SynapseForbiddenException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	public SynapseForbiddenException() {
		super();
	}

	public SynapseForbiddenException(String message, Throwable cause) {
		super(message, cause);
	}

	public SynapseForbiddenException(String message) {
		super(message);
	}

	public SynapseForbiddenException(Throwable cause) {
		super(cause);
	}

	public SynapseForbiddenException(String message, ErrorResponseCode errorResponseCode) {
		super(message, errorResponseCode);
	}
}
