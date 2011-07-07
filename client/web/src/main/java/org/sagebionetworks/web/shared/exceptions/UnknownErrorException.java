package org.sagebionetworks.web.shared.exceptions;

public class UnknownErrorException extends RestServiceException {
	
	private static final long serialVersionUID = 1L;

	public UnknownErrorException() {
		super();
	}

	public UnknownErrorException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnknownErrorException(String message) {
		super(message);
	}

	public UnknownErrorException(Throwable cause) {
		super(cause);
	}

}
