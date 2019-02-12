package org.sagebionetworks.repo.manager.loginlockout;

/**
 * Exception that is thrown when an attempt is being made while the lockout is still in place.
 * @see UnsuccessfulLoginLockout#checkIsLockedOut(long)
 */
public class UnsuccessfulLoginLockoutException extends RuntimeException{
	final long lockoutExpirationTimestampSec;
	final long numFailedAttempts;

	public UnsuccessfulLoginLockoutException(final long lockoutExpirationTimestampSec, final long numFailedAttempts){
		super("You locked out from making any additional login attempts until " + lockoutExpirationTimestampSec);
		this.lockoutExpirationTimestampSec = lockoutExpirationTimestampSec;
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
