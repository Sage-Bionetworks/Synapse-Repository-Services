package org.sagebionetworks.repo.web;

/**
 * Should be thrown when a Service is Unavailable.
 * @author John
 *
 */
public class ServiceUnavailableException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2304717284960254759L;

	public ServiceUnavailableException() {
		super();
	}

	public ServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServiceUnavailableException(String message) {
		super(message);
	}

	public ServiceUnavailableException(Throwable cause) {
		super(cause);
	}

}
