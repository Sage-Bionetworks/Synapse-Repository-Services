package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;

public class SynapseUnauthorizedException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	private static final int UNAUTHORIZED_STATUS_CODE = HttpStatus.SC_UNAUTHORIZED;

	public SynapseUnauthorizedException() {
		super(UNAUTHORIZED_STATUS_CODE);
	}

	public SynapseUnauthorizedException(String message, Throwable cause) {
		super(UNAUTHORIZED_STATUS_CODE, message, cause);
	}

	public SynapseUnauthorizedException(String message) {
		super(UNAUTHORIZED_STATUS_CODE, message);
	}

	public SynapseUnauthorizedException(Throwable cause) {
		super(UNAUTHORIZED_STATUS_CODE, cause);
	}

}
