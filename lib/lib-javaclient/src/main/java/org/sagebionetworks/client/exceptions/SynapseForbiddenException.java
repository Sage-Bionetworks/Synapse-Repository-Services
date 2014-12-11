package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;


public class SynapseForbiddenException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	private static final int FORBIDDEN_STATUS_CODE = HttpStatus.SC_FORBIDDEN;

	public SynapseForbiddenException() {
		super(FORBIDDEN_STATUS_CODE);
	}

	public SynapseForbiddenException(String message, Throwable cause) {
		super(FORBIDDEN_STATUS_CODE, message, cause);
	}

	public SynapseForbiddenException(String message) {
		super(FORBIDDEN_STATUS_CODE, message);
	}

	public SynapseForbiddenException(Throwable cause) {
		super(FORBIDDEN_STATUS_CODE, cause);
	}

}
