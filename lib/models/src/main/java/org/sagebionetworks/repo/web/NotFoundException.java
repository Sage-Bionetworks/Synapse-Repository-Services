package org.sagebionetworks.repo.web;

/**
 * Application exception indicating that the desired resource was not found.
 * 
 * @author deflaux
 * 
 */
public class NotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_MESSAGE = "The resource you are attempting to access cannot be found";

	/**
	 * Default constructor
	 */
	public NotFoundException() {
		super(DEFAULT_MESSAGE);
	}

	/**
	 * @param message
	 */
	public NotFoundException(String message) {
		super(message);
	}

	/**
	 * @param message
	 * @param rootCause
	 */
	public NotFoundException(String message, Throwable rootCause) {
		super(message, rootCause);
	}
}
