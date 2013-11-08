package org.sagebionetworks.repo.web;

/**
 * Application exception indicating that the desired resource is forbidden
 */
public class ForbiddenException extends RuntimeException {

	private static final long serialVersionUID = 2L;
	private static final String DEFAULT_MESSAGE = "The resource you are attempting to access is forbidden.";

	/**
	 * Default constructor with a default (un-informative) message
	 */
	public ForbiddenException() {
		super(DEFAULT_MESSAGE);
	}

	public ForbiddenException(String message) {
		super(message);
	}

	public ForbiddenException(Throwable rootCause) {
		super(DEFAULT_MESSAGE, rootCause);
	}

	public ForbiddenException(String message, Throwable rootCause) {
		super(message, rootCause);
	}
}
