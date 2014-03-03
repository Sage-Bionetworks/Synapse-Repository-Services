package org.sagebionetworks.repo.model.exception;

/**
 * An exception that is thrown when a lock cannot be acquired.
 * @author jmhill
 *
 */
public class LockUnavilableException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8063688801740025755L;

	public LockUnavilableException() {
		super();
	}

	public LockUnavilableException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public LockUnavilableException(String message, Throwable cause) {
		super(message, cause);
	}

	public LockUnavilableException(String message) {
		super(message);
	}

	public LockUnavilableException(Throwable cause) {
		super(cause);
	}

}
