package org.sagebionetworks.repo.model.semaphore;

/**
 * Thrown if a lock cannot be released.
 *
 */
public class LockReleaseFailedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LockReleaseFailedException() {
		super();
	}

	public LockReleaseFailedException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public LockReleaseFailedException(String message, Throwable cause) {
		super(message, cause);
	}

	public LockReleaseFailedException(String message) {
		super(message);
	}

	public LockReleaseFailedException(Throwable cause) {
		super(cause);
	}
	
	

}
