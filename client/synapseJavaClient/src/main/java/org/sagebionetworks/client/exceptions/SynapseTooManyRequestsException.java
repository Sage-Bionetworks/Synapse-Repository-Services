package org.sagebionetworks.client.exceptions;


public class SynapseTooManyRequestsException extends SynapseServerException {
	
	private static final long serialVersionUID = 1L;

	//apache's HttpStatus is outdated and does not have the 429 error code included
	public static final int TOO_MANY_REQUESTS_STATUS_CODE = 429;

	public SynapseTooManyRequestsException() {
		super(TOO_MANY_REQUESTS_STATUS_CODE);
	}

	public SynapseTooManyRequestsException(String message, Throwable cause) {
		super(TOO_MANY_REQUESTS_STATUS_CODE, message, cause);
	}

	public SynapseTooManyRequestsException(String message) {
		super(TOO_MANY_REQUESTS_STATUS_CODE, message);
	}

	public SynapseTooManyRequestsException(Throwable cause) {
		super(TOO_MANY_REQUESTS_STATUS_CODE, cause);
	}

}