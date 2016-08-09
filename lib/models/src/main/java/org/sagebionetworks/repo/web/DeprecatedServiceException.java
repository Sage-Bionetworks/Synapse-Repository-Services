package org.sagebionetworks.repo.web;

/**
 * Application exception indicating that the desired service was deprecated.
 */
public class DeprecatedServiceException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_MESSAGE = "The service you are attempting to access is deprecated.";

	/**
	 * Default constructor
	 */
	public DeprecatedServiceException() {
		super(DEFAULT_MESSAGE);
	}

	/**
	 * @param message
	 */
	public DeprecatedServiceException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param rootCause
	 */
	public DeprecatedServiceException(String message, Throwable rootCause) {
		super(message, rootCause);
	}
}
