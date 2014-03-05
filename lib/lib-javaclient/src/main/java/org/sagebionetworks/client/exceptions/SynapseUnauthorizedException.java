package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;

public class SynapseUnauthorizedException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	private static final int STATUS_CODE = HttpStatus.SC_UNAUTHORIZED;

	public SynapseUnauthorizedException() {
		super(STATUS_CODE);
	}

	public SynapseUnauthorizedException(String message, Throwable cause) {
		super(STATUS_CODE, message, cause);
	}

	public SynapseUnauthorizedException(String message) {
		super(STATUS_CODE, message);
	}

	public SynapseUnauthorizedException(Throwable cause) {
		super(STATUS_CODE, cause);
	}

}
