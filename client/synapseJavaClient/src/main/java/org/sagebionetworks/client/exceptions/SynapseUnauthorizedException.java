package org.sagebionetworks.client.exceptions;

public class SynapseUnauthorizedException extends SynapseUserException {
	
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
