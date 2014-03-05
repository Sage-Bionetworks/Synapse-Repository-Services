package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;

public class SynapseNotFoundException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	private static final int STATUS_CODE = HttpStatus.SC_NOT_FOUND;

	public SynapseNotFoundException() {
		super(STATUS_CODE);
	}

	public SynapseNotFoundException(String message, Throwable cause) {
		super(STATUS_CODE, message, cause);
	}

	public SynapseNotFoundException(String message) {
		super(STATUS_CODE, message);
	}

	public SynapseNotFoundException(Throwable cause) {
		super(STATUS_CODE, cause);
	}

}
