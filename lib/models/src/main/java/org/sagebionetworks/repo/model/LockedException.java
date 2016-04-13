package org.sagebionetworks.repo.model;

/**
 * Thrown when the requested object is being locked
 */
public class LockedException extends RuntimeException {

	private static final long serialVersionUID = 7107927776246694318L;

	public LockedException() {
		super();
	}

	public LockedException(String message, Throwable cause) {
		super(message, cause);
	}

	public LockedException(String s) {
		super(s);
	}

	public LockedException(Throwable cause) {
		super(cause);
	}

}
