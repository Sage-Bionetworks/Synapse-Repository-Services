/**
 *
 */
package org.sagebionetworks.repo.web;

import javax.servlet.ServletException;

/**
 * Application exception indicating that the desired resource was not found.
 * 
 * @author deflaux
 * 
 */
public class NotFoundException extends ServletException {

	private static final long serialVersionUID = 1L;
	private static final String DEFAULT_MESSAGE = "The resource you are attempting to retrieve cannot be found";

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
	 * @param rootCause
	 */
	public NotFoundException(Throwable rootCause) {
		super(DEFAULT_MESSAGE, rootCause);
	}

	/**
	 * @param message
	 * @param rootCause
	 */
	public NotFoundException(String message, Throwable rootCause) {
		super(message, rootCause);
	}
}
