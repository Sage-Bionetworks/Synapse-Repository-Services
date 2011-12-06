package org.sagebionetworks.web.shared.exceptions;

public class UnauthorizedException extends RestServiceException {
	
	private static final long serialVersionUID = 1L;

	public UnauthorizedException() {
		super();
	}

	public UnauthorizedException(String message) {
		super(message);
	}
}
