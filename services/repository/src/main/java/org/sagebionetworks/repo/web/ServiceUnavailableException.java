package org.sagebionetworks.repo.web;

/**
 * Thrown when a service is Service unavailable do to maintenance.
 * @author John
 *
 */
public class ServiceUnavailableException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ServiceUnavailableException() {
	}

	public ServiceUnavailableException(String message) {
		super(message);
	}

	public ServiceUnavailableException(Throwable cause) {
		super(cause);
	}

	public ServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}
