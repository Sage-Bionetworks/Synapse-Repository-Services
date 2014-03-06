package org.sagebionetworks.client.exceptions;

import org.apache.http.HttpStatus;


public class SynapseForbiddenException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	private static final int STATUS_CODE = HttpStatus.SC_FORBIDDEN;

	public SynapseForbiddenException() {
		super(STATUS_CODE);
	}

	public SynapseForbiddenException(String message, Throwable cause) {
		super(STATUS_CODE, message, cause);
	}

	public SynapseForbiddenException(String message) {
		super(STATUS_CODE, message);
	}

	public SynapseForbiddenException(Throwable cause) {
		super(STATUS_CODE, cause);
	}

}
