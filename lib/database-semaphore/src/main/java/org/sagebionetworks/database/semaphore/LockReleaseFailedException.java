package org.sagebionetworks.database.semaphore;

/**
 * This exception is thrown when the lock for a given token does not exist.
 * Typically, this means the the lock was forfeit because it timed out.
 * 
 */
public class LockReleaseFailedException extends RuntimeException {

	private static final long serialVersionUID = 6405189525543796326L;

	public LockReleaseFailedException(String message) {
		super(message);
	}

}
