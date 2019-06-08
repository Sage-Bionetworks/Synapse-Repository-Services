package org.sagebionetworks.repo.manager;

import org.sagebionetworks.repo.manager.loginlockout.UnsuccessfulLoginLockoutException;

/**
 * Used to check the password for a user
 */
public interface UserCredentialValidator {
	/**
	 * Checks whether the user's hashed password matches the stored hash.
	 * @param userId id of the user for the credential check
	 * @param password password to be hashed
	 * @return true if the password's hash matches the stored password hash
	 */
	boolean checkPassword(final long userId, final String password);

	/**
	 * Checks whether the user's hashed password matches the stored hash.
	 * If there has not been enough time between the last unsuccessful password check for that user
	 * (previously returned false for that particular principalId), an Exception will be thrown.
	 * @param userId id of the user for the credential check
	 * @param password password to be hashed
	 * @return true if the password's hash matches the stored password hash
	 * @throws UnsuccessfulLoginLockoutException If there has not been enough time between the last
	 * unsuccessful password check for that user (previously returned false for that particular principalId)
	 */
	boolean checkPasswordWithThrottling(final long userId, final String password);

	void forceResetLoginThrottle(final long userId);
}
