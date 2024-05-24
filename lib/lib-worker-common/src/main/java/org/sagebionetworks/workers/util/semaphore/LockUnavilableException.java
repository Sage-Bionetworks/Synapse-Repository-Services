package org.sagebionetworks.workers.util.semaphore;

import java.util.Optional;

/**
 * An exception that is thrown when a lock cannot be acquired.
 */
public class LockUnavilableException extends RuntimeException {

	private static final long serialVersionUID = -8063688801740025755L;

	private final LockType lockType;
	private final String lockKey;
	private final String lockHoldersContext;

	/**
	 * Create a new LockUnavilableException.
	 * 
	 * @param lockType           The type of lock that could not be acquired.
	 * @param lockKey            The key of the lock that could not be acquired.
	 * @param lockHoldersContext The context of the current lock holder that
	 *                           prevented the acquisition of the lock.
	 */
	public LockUnavilableException(LockType lockType, String lockKey, String lockHoldersContext) {
		super(String.format("%s lock unavailable for key: '%s'. Current lock holder's context: '%s'", lockType.name(), lockKey, lockHoldersContext));
		this.lockType = lockType;
		this.lockKey = lockKey;
		this.lockHoldersContext = lockHoldersContext;
	}

	/**
	 * The type of lock that could not be acquired.
	 * 
	 * @return
	 */
	public LockType getLockType() {
		return lockType;
	}

	/**
	 * The key of the lock that could not be acquired.
	 * 
	 * @return
	 */
	public String getLockKey() {
		return lockKey;
	}

	/**
	 * The context of the current lock holder that prevented the acquisition of the
	 * lock.
	 * 
	 * @return The lock holder's context. Will be {@link Optional#empty()} if the
	 *         lock is released after the attempted lock acquisition, but before the
	 *         context could be fetched. This should be rare but possible.
	 */
	public Optional<String> getLockHoldersContext() {
		return Optional.ofNullable(lockHoldersContext);
	}
	

}
