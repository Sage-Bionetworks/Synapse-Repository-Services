package org.sagebionetworks.repo.model.dao.semaphore;

/**
 * A very simple Semaphore enforced by a database.
 * 
 * @author jmhill
 *
 */
public interface CountingSemaphoreDao {
	

	/**
	 * Attempt to acquire at lock of a given type. This call is non-blocking, so if the lock cannot be acquired it will
	 * return without waiting for the lock.
	 * 
	 * @param key - The key of the lock to acquire.
	 * @return null if the lock was not acquired the lock token otherwise
	 */
	public String attemptToAcquireLock();

	/**
	 * When the process is finished it should release the lock. This method should only be called by a process that
	 * received the lock from {@link #attemptToAcquireLock()}
	 * 
	 * @param token - The token we got from attemptToAcquireLock.
	 */
	public void releaseLock(String token);
}
