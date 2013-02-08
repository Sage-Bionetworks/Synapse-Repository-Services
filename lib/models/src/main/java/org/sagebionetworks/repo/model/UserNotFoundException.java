package org.sagebionetworks.repo.model;

public class UserNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 4756629644457463944L;

	public UserNotFoundException(String message) {
		super(message);
	}
}
