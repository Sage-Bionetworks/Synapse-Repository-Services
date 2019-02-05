package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

/**
 * Package to manage
 */
public interface UnsuccessfulAttemptLockout { //TODO: rename. too close w/ UnsuccessfulAttemptLockoutDAO

	/**
	 * Check if the key is currently being locked out from any attempts at the moment.
	 *
	 * @param key the key used to identify an attempt
	 * @throws UnsuccessfulAttemptLockoutException if the key is currently locked out from making further attempts
	 * @return AttemptResultReporter which an be used to report the success or failure of this attempt
	 */
	public AttemptResultReporter checkIsLockedOut(String key);
}
