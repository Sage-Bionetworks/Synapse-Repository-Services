package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;


public class SynapseBadRequestException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	private static final int BAD_REQUEST_STATUS_CODE = HttpStatus.SC_BAD_REQUEST;

	public SynapseBadRequestException() {
		super(BAD_REQUEST_STATUS_CODE);
	}

	public SynapseBadRequestException(String message, Throwable cause) {
		super(BAD_REQUEST_STATUS_CODE, message, cause);
	}

	public SynapseBadRequestException(String message) {
		super(BAD_REQUEST_STATUS_CODE, message);
	}

	public SynapseBadRequestException(Throwable cause) {
		super(BAD_REQUEST_STATUS_CODE, cause);
	}

}
