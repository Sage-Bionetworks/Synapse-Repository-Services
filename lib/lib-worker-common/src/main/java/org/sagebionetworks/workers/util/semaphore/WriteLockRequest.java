package org.sagebionetworks.workers.util.semaphore;

import java.util.Objects;

import org.sagebionetworks.util.progress.ProgressCallback;

/**
 * Request to create a write lock.
 *
 */
public class WriteLockRequest {

	private final ProgressCallback callback;
	private final String callersContext;
	private final String lockKey;

	/**
	 * Request to create a write lock.
	 * 
	 * @param callback       The callback used to refresh lock timeouts.
	 * @param callersContext Description of the caller's context. This is provided
	 *                       to caller when a lock is unavailable.
	 * @param lockKey        The key that defines the lock.
	 */
	public WriteLockRequest(ProgressCallback callback, String callersContext, String lockKey) {
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
		this.lockKey = lockKey;
	}

	public ProgressCallback getCallback() {
		return callback;
	}

	public String getCallersContext() {
		return callersContext;
	}

	public String getLockKey() {
		return lockKey;
	}

	@Override
	public int hashCode() {
		return Objects.hash(callback, callersContext, lockKey);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WriteLockRequest other = (WriteLockRequest) obj;
		return Objects.equals(callback, other.callback) && Objects.equals(callersContext, other.callersContext)
				&& Objects.equals(lockKey, other.lockKey);
	}

	@Override
	public String toString() {
		return "WriteLockRequest [callback=" + callback + ", callersContext=" + callersContext + ", lockKey=" + lockKey
				+ "]";
	}
	
}
