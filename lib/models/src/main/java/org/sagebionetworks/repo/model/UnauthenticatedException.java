package org.sagebionetworks.repo.model;

public class UnauthenticatedException extends RuntimeException {

	private static final long serialVersionUID = 11356746209283224L;

	public UnauthenticatedException() {
		super("Authenticated required.");
	}

	public UnauthenticatedException(String message) {
		super(message);
	}

	public UnauthenticatedException(Throwable cause) {
		super(cause);
	}

	public UnauthenticatedException(String message, Throwable cause) {
		super(message, cause);
	}

}
