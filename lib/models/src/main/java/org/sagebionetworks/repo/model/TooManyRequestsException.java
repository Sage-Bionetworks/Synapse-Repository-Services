package org.sagebionetworks.repo.model;

/**
 * Thrown when a threshold number of requests are performed in an interval
 */
public class TooManyRequestsException extends RuntimeException {

	private static final long serialVersionUID = -746510861033508781L;

	public TooManyRequestsException() {
		super();
	}

	public TooManyRequestsException(String message, Throwable cause) {
		super(message, cause);
	}

	public TooManyRequestsException(String s) {
		super(s);
	}

	public TooManyRequestsException(Throwable cause) {
		super(cause);
	}

}
