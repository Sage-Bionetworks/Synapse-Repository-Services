package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

/**
 * Exception that is thrown when an attempt is being made while the lockout is still in place.
 */
public class UnsuccessfulAttemptLockoutException extends RuntimeException{
	long lockoutExpirationTimestampSec;

	public UnsuccessfulAttemptLockoutException(String message, long lockoutExpirationTimestampSec){
		super(message);
		this.lockoutExpirationTimestampSec = lockoutExpirationTimestampSec;
	}

	/**
	 * Timestamp, in seconds, when the lockout will expire
	 * @return
	 */
	public long getLockoutExpirationTimestampSec() {
		return lockoutExpirationTimestampSec;
	}
}
