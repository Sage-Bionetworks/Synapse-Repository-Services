package org.sagebionetworks.repo.manager.loginlockout;

/**
 * Exception that is thrown when an attempt is being made while the lockout is
 * still in place.
 * 
 * @see LoginLockoutStatus#checkIsLockedOut(long)
 */
public class UnsuccessfulLoginLockoutException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	final long lockoutExpirationTimestampSec;
	final long numFailedAttempts;

	public UnsuccessfulLoginLockoutException(final long remainingMillisecondsToNextLoginAttempt,
			final long numFailedAttempts) {
		super("You are locked out from making any additional login attempts for "
				+ remainingMillisecondsToNextLoginAttempt + " milliseconds");
		this.lockoutExpirationTimestampSec = remainingMillisecondsToNextLoginAttempt / 1000L;
		this.numFailedAttempts = numFailedAttempts;
	}

	/**
	 * Timestamp, in seconds, when the lockout will expire
	 * 
	 * @return
	 */
	public long getLockoutExpirationTimestampSec() {
		return lockoutExpirationTimestampSec;
	}

	public long getNumFailedAttempts() {
		return numFailedAttempts;
	}
}
