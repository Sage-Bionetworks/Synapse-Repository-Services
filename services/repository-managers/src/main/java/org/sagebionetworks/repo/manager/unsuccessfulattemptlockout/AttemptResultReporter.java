package org.sagebionetworks.repo.manager.unsuccessfulattemptlockout;

/**
 * Used to report success or failure on an attempt.
 * @see UnsuccessfulAttemptLockout#checkIsLockedOut(String key)
 */
public interface AttemptResultReporter {
	void reportSuccess();
	void reportFailure();
}
