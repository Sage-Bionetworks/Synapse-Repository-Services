package org.sagebionetworks.repo.manager.loginlockout;

/**
 * Used to report success or failure on an attempt.
 * @see UnsuccessfulLoginLockout#checkIsLockedOut(long)
 */
public interface AttemptResultReporter {
	void reportSuccess();
	void reportFailure();
}
