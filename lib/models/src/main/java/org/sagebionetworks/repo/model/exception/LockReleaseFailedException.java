package org.sagebionetworks.repo.model.exception;

/**
 * This exception is thrown when releasing a lock failed.
 * 
 * @author jmhill
 *
 */
public class LockReleaseFailedException extends RuntimeException {

	private static final long serialVersionUID = 1121605776704903477L;

	public LockReleaseFailedException() {
		super();
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
