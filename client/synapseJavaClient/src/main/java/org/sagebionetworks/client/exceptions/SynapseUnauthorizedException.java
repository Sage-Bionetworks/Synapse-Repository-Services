package org.sagebionetworks.client.exceptions;

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

	public SynapseUnauthorizedException(Throwable cause) {
		super(cause);
	}

}
