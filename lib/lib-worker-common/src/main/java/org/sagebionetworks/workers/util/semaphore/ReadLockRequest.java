package org.sagebionetworks.workers.util.semaphore;

import java.util.Arrays;
import java.util.Objects;

import org.sagebionetworks.util.progress.ProgressCallback;

/**
 * Request to create a lock.
 */
public class ReadLockRequest {

	private final ProgressCallback callback;
	private final String callersContext;
	private final String[] lockKeys;

	/**
	 * Request to create a new lock.
	 * 
	 * @param callback       The callback used to refresh lock timeouts.
	 * @param callersContext Description of the caller's context. This is provided
	 *                       to caller when a lock is unavailable.
	 * @param lockKey        The key that defines the lock.
	 */
	public ReadLockRequest(ProgressCallback callback, String callersContext, String... lockKey) {
		if (callback == null) {
			throw new IllegalArgumentException("ProgressCallback cannot be null");
		}
		this.callback = callback;
		if (this.callback.getLockTimeoutSeconds() < Constants.MINIMUM_LOCK_TIMEOUT_SEC) {
			throw new IllegalArgumentException("LockTimeout cannot be less than 2 seconds");
		}
		if (callersContext == null) {
			throw new IllegalArgumentException("Caller's context cannot be null");
		}
		this.callersContext = callersContext;
		if (lockKey == null) {
			throw new IllegalArgumentException("LockKey cannot be null");
		}
		this.lockKeys = lockKey;
		if (this.lockKeys.length < 1) {
			throw new IllegalArgumentException("Must include at least one lock key");
		}
		for (String key : lockKey) {
			if (key == null) {
				throw new IllegalArgumentException("Lock key cannot be null");
			}
		}
	}

	public ProgressCallback getCallback() {
		return callback;
	}

	public String[] getLockKeys() {
		return lockKeys;
	}

	public String getCallersContext() {
		return callersContext;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(lockKeys);
		result = prime * result + Objects.hash(callback, callersContext);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReadLockRequest other = (ReadLockRequest) obj;
		return Objects.equals(callback, other.callback) && Objects.equals(callersContext, other.callersContext)
				&& Arrays.equals(lockKeys, other.lockKeys);
	}

	@Override
	public String toString() {
		return "ReadLockRequest [callback=" + callback + ", callersContext=" + callersContext + ", lockKeys="
				+ Arrays.toString(lockKeys) + "]";
	}

}
