package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

/**
 * Manages locking out consecutive unsuccessful attempts. Attempts are identified by a String key.
 */
public interface UnsuccessfulAttemptLockout { //TODO: rename. too close w/ UnsuccessfulAttemptLockoutDAO

	/**
	 * Check if the key is currently being locked out from any additional attempts.
	 *
	 * @param key the key used to identify an attempt
	 * @throws UnsuccessfulAttemptLockoutException if the key is currently locked out from making further attempts
	 * @return AttemptResultReporter which an be used to report the success or failure of this next attempt
	 */
	public AttemptResultReporter checkIsLockedOut(String key);
}
