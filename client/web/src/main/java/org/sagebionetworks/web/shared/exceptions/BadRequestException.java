package org.sagebionetworks.web.shared.exceptions;

public class BadRequestException extends RestServiceException {
	
	private static final long serialVersionUID = 1L;

	public BadRequestException() {
		super();
	}

	public BadRequestException(String message) {
		super(message);
	}
}
