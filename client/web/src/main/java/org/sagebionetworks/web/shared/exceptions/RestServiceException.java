package org.sagebionetworks.web.shared.exceptions;

public class RestServiceException extends Exception {
	public RestServiceException() {
		super();
	}

	public RestServiceException(String message, Throwable cause) {
		super(message, cause);
	}

	public RestServiceException(String message) {
		super(message);
	}

	public RestServiceException(Throwable cause) {
		super(cause);
	}

}
