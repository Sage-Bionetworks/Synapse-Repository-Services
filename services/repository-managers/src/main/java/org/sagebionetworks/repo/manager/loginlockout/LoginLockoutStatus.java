package org.sagebionetworks.repo.manager.loginlockout;

/**
 * Manages locking out consecutive unsuccessful attempts. Attempts are identified by a String key.
 */
public interface LoginLockoutStatus {

	/**
	 * Check if the key is currently being locked out from any additional attempts.
	 *
	 * @param userId the key used to identify an user
	 * @throws UnsuccessfulLoginLockoutException if the key is currently locked out from making further attempts
	 * @return {@link LoginAttemptResultReporter} which an be used to report the success or failure of this next attempt
	 */
	public LoginAttemptResultReporter checkIsLockedOut(final long userId);

	/**
	 * Forces a reset on the users lockout count and expires their lockout timestamp
	 * @param userId the key used to identify an user
	 */
	public void forceResetLockoutCount(final long userId);
}
