package org.sagebionetworks.repo.model;

public class UnauthenticatedException extends RuntimeException {

	public static final String MESSAGE_USERNAME_PASSWORD_COMBINATION_IS_INCORRECT = "The provided username/password combination is incorrect";
	
	private static final long serialVersionUID = 11356746209283224L;

	public UnauthenticatedException(String message) {
		super(message);
	}

	public UnauthenticatedException(String message, Throwable cause) {
		super(message, cause);
	}

}
