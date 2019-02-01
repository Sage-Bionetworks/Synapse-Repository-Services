package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

/**
 * Package to manage
 */
public interface UnsuccessfulAttemptLockout {

	/**
	 * Report that an successful attempt has been made for this key.
	 * @param key
	 */
	public void reportSuccess(String key);

	/**
	 * Report that an unsuccessful attempt has been made for this key.
	 * @param key
	 */
	public void reportFailure(String key);

	/**
	 * Check if the key is currently being locked out from any attempts at the moment
	 * @param key
	 * @throws UnsuccessfulAttemptLockoutException if the key is currently locked out from making further attempts
	 */
	public void checkIsLockedOut(String key);
}
