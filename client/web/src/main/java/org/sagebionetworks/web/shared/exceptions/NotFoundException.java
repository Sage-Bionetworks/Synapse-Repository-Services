package org.sagebionetworks.web.shared.exceptions;

public class NotFoundException extends RestServiceException {
	
	private static final long serialVersionUID = 1L;

	public NotFoundException() {
		super();
	}

	public NotFoundException(String message) {
		super(message);
	}

}
