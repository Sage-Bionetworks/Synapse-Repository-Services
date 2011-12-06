package org.sagebionetworks.web.shared.exceptions;

public class UnknownErrorException extends RestServiceException {
	
	private static final long serialVersionUID = 1L;

	public UnknownErrorException() {
		super();
	}

	public UnknownErrorException(String message) {
		super(message);
	}
}
