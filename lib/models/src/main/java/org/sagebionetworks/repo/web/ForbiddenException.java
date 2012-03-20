package org.sagebionetworks.repo.web;

/**
 * Application exception indicating that the desired resource is forbidden
 * 
 * 
 */
public class ForbiddenException extends Exception {

	private static final long serialVersionUID = 2L;
	private static final String DEFAULT_MESSAGE = "The resource you are attempting to access is forbidden.";

	/**
	 * Default constructor
	 */
	public ForbiddenException() {
		super(DEFAULT_MESSAGE);
	}

	/**
	 * @param message
	 */
	public ForbiddenException(String message) {
		super(message);
	}

	/**
	 * @param rootCause
	 */
	public ForbiddenException(Throwable rootCause) {
		super(DEFAULT_MESSAGE, rootCause);
	}

	/**
	 * @param message
	 * @param rootCause
	 */
	public ForbiddenException(String message, Throwable rootCause) {
		super(message, rootCause);
	}
}
