package org.sagebionetworks.repo.model;

/**
 * Exception thrown when a limit is exceeded.
 * 
 *
 */
public class LimitExceededException extends Exception {

	private static final long serialVersionUID = 1L;

	public LimitExceededException(String message) {
		super(message);
	}

}
