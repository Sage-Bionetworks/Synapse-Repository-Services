package org.sagebionetworks.repo.model;

public class UnauthorizedException extends Exception {

	private static final long serialVersionUID = 1130258734762622224L;

	public UnauthorizedException() {
		super("You are not authorized to access the requested resource and/or perform the requested activity");
	}

	public UnauthorizedException(String message) {
		super(message);
	}

	public UnauthorizedException(Throwable cause) {
		super(cause);
	}

	public UnauthorizedException(String message, Throwable cause) {
		super(message, cause);
	}

}
