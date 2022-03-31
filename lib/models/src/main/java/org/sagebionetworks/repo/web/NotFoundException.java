package org.sagebionetworks.repo.web;

/**
 * Application exception indicating that the desired resource was not found.
 * 
 * @author deflaux
 * 
 */
public class NotFoundException extends RuntimeException {

	private static final long serialVersionUID = 1L;

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
