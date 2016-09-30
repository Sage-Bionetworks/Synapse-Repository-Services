package org.sagebionetworks.repo.model.semaphore;

/**
 * An abstraction of a simple counting semaphore.
 * All locks for a given key will expire at the earliest set time.
 * Manual releasing is not supported.
 * @author zdong
 *
 */
public interface MemoryTimeBlockCountingSemaphore {
	
	/**
	 * Attempt to acquire a lock with the given key. This method is blocking
	 * and a lock is either available and issued immediately or not at all.
	 * 
	 * @param key
	 *            A unique key to lock on
	 * @param maxLockCount
	 *            The maximum number of locks of that can be issued from the semaphore to the given
	 *            key.
	 * @param timeoutSec
	 *            The maximum life of the semaphore for that key in seconds.
	 *            If the current semaphore for the given key has not yet expired, this parameter will be ignored.
	 * 
	 * @return boolean true if lock was acquired successfully. false otherwise.
	 */
	public boolean attemptToAcquireLock(String key, long timeoutSec,
			long maxLockCount);
	
	/**
	 * Force the release of all locks.
	 */
	public void releaseAllLocks();
}
