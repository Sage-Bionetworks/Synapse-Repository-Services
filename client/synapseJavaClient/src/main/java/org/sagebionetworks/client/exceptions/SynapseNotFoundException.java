package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;

public class SynapseNotFoundException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	private static final int NOT_FOUND_STATUS_CODE = HttpStatus.SC_NOT_FOUND;

	public SynapseNotFoundException() {
		super(NOT_FOUND_STATUS_CODE);
	}

	public SynapseNotFoundException(String message, Throwable cause) {
		super(NOT_FOUND_STATUS_CODE, message, cause);
	}

	public SynapseNotFoundException(String message) {
		super(NOT_FOUND_STATUS_CODE, message);
	}

	public SynapseNotFoundException(Throwable cause) {
		super(NOT_FOUND_STATUS_CODE, cause);
	}

}
