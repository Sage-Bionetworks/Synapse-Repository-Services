package org.sagebionetworks.repo.model;

public class InvalidUserException extends RuntimeException {

	private static final long serialVersionUID = 4756629644457463944L;

	public InvalidUserException(String message) {
		super(message);
	}
}
