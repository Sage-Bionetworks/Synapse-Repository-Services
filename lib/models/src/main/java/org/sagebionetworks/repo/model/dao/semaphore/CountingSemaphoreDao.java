package org.sagebionetworks.repo.model.dao.semaphore;

import org.sagebionetworks.repo.web.NotFoundException;

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
	 * @return null if the lock was not acquired the lock token otherwise
	 */
	public String attemptToAcquireLock();

	/**
	 * Attempt to acquire at lock of a given type. This call is non-blocking, so if the lock cannot be acquired it will
	 * return without waiting for the lock.
	 * 
	 * @param extraKey - The key to append to the lock name of the lock to acquire.
	 * @return null if the lock was not acquired the lock token otherwise
	 */
	public String attemptToAcquireLock(String extraKey);

	/**
	 * When the process is finished it should release the lock. This method should only be called by a process that
	 * received the lock from {@link #attemptToAcquireLock()}
	 * 
	 * @param token - The token we got from attemptToAcquireLock.
	 */
	public void releaseLock(String token);

	/**
	 * When the process is finished it should release the lock. This method should only be called by a process that
	 * received the lock from {@link #attemptToAcquireLock()}
	 * 
	 * @param extraKey - The key to append to the lock name of the lock to release.
	 * @param token - The token we got from attemptToAcquireLock.
	 */
	public void releaseLock(String token, String extraKey);

	/**
	 * Extend the lease for this lock with the lock timeout
	 * 
	 * @param token
	 * @throws NotFoundException
	 */
	public void extendLockLease(String token) throws NotFoundException;

	/**
	 * Get the timeout for a lock in milliseconds
	 * 
	 * @return the lock timeout
	 */
	public long getLockTimeoutMS();
}
