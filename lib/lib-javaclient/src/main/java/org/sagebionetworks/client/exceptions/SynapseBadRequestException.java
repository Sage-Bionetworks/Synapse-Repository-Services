package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;


public class SynapseBadRequestException extends SynapseUserException {
	
	private static final long serialVersionUID = 1L;

	private static final int STATUS_CODE = HttpStatus.SC_BAD_REQUEST;

	public SynapseBadRequestException() {
		super(STATUS_CODE);
	}

	public SynapseBadRequestException(String message, Throwable cause) {
		super(STATUS_CODE, message, cause);
	}

	public SynapseBadRequestException(String message) {
		super(STATUS_CODE, message);
	}

	public SynapseBadRequestException(Throwable cause) {
		super(STATUS_CODE, cause);
	}

}
