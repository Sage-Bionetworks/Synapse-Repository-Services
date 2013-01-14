package org.sagebionetworks.repo.web;

/**
 * Thrown when the resources are temporarily unavailable.  
 * @author John
 *
 */
public class TemporarilyUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 3161191200224977763L;

	public TemporarilyUnavailableException() {
		super();
	}

	public TemporarilyUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public TemporarilyUnavailableException(String message) {
		super(message);
	}

	public TemporarilyUnavailableException(Throwable cause) {
		super(cause);
	}
	
}