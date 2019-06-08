package org.sagebionetworks.repo.manager.loginlockout;

/**
 * Used to report success or failure on an attempt. Multiple calls to methods are idempotent
 * @see LoginLockoutStatus#checkIsLockedOut(long)
 */
public interface LoginAttemptResultReporter {
	/**
	 * Reports the success of the last login attempt. No effect after the first call to either {@link #reportSuccess()} or {@link #reportFailure()}
	 */
	void reportSuccess();

	/**
	 * Reports the failure of the last login attempt. No effect after the first call to either {@link #reportSuccess()} or {@link #reportFailure()}
	 */
	void reportFailure();
}
