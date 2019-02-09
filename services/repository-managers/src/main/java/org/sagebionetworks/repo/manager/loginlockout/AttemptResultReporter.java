package org.sagebionetworks.repo.manager.loginlockout;

/**
 * Used to report success or failure on an attempt.
 * @see UnsuccessfulLoginLockout#checkIsLockedOut(String key)
 */
public interface AttemptResultReporter {
	void reportSuccess();
	void reportFailure();
}
