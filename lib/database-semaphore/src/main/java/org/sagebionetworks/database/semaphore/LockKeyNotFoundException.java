package org.sagebionetworks.database.semaphore;

/**
 * Thrown when a lock key cannot be found.
 *
 */
public class LockKeyNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 2381039440160420088L;

	public LockKeyNotFoundException(String message) {
		super(message);
	}

	
}
