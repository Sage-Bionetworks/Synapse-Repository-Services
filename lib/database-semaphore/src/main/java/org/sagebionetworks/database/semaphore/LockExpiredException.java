package org.sagebionetworks.database.semaphore;

public class LockExpiredException extends RuntimeException {
	
	private static final long serialVersionUID = 640518952554396326L;

	public LockExpiredException(String message) {
		super(message);
	}

}
