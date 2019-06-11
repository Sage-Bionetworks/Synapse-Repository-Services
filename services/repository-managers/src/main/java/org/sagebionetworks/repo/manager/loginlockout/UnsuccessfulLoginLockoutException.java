package org.sagebionetworks.repo.manager.loginlockout;

/**
 * Exception that is thrown when an attempt is being made while the lockout is still in place.
 * @see LoginLockoutStatus#checkIsLockedOut(long)
 */
public class UnsuccessfulLoginLockoutException extends RuntimeException{
	final long lockoutExpirationTimestampSec;
	final long numFailedAttempts;

	public UnsuccessfulLoginLockoutException(final long currentTimestampMilis, final long lockoutExpirationTimestampMillis, final long numFailedAttempts){
		super("You locked out from making any additional login attempts for " + (lockoutExpirationTimestampMillis - currentTimestampMilis) + " milliseconds");
		this.lockoutExpirationTimestampSec = lockoutExpirationTimestampMillis;
		this.numFailedAttempts = numFailedAttempts;
	}

	/**
	 * Timestamp, in seconds, when the lockout will expire
	 * @return
	 */
	public long getLockoutExpirationTimestampSec() {
		return lockoutExpirationTimestampSec;
	}

	public long getNumFailedAttempts() {
		return numFailedAttempts;
	}
}
