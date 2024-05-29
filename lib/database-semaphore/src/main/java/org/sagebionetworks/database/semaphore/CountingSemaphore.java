package org.sagebionetworks.database.semaphore;

import java.util.Optional;

/**
 * A Database backed semaphore that supports multiple locks to be issued for the
 * same key.
 * 
 * @see <a href=
 *      "http://en.wikipedia.org/wiki/Semaphore_%28programming%29">Semaphore</a>
 * 
 * 
 */
public interface CountingSemaphore {

	/**
	 * Attempt to acquire a lock with the given key. This method is non-blocking and
	 * a lock is either available and issued immediately or not at all.
	 * 
	 * @param key          A unique key to lock on
	 * @param maxLockCount The maximum number of locks of that can be issued to the
	 *                     given key.
	 * @param timeoutSec   The maximum life of the lock in seconds. If the lock is
	 *                     not released before this amount of time elapses then it
	 *                     will be forfeit. The lock timeout can be refreshed with
	 *                     {@link #refreshLockTimeout(String, String, long)}
	 * 
	 * @param context      Describes the context for which the lock will be used.
	 *                     When a lock cannot be acquired, the context of the
	 *                     blocking lock will be provided. Context is required an
	 *                     cannot be empty or more than 255 characters.
	 * 
	 * @return The token for the lock. This token must be used to release the lock.
	 *         Returns Optional.empty() when no locks are available. The caller is expected to
	 *         releases the lock {@link #releaseLock(String, String)} when finished
	 *         with it.
	 */
	public Optional<String> attemptToAcquireLock(String key, long timeoutSec, int maxLockCount, String context);
	

	/**
	 * Refresh the expiration for a lock that is currently being held.
	 * 
	 * @param key   The unique key for the lock.
	 * @param token The lock token issued from
	 *              {@link #attemptToAcquireLock(String, long, int)}
	 * 
	 * @throws LockReleaseFailedException When the given token has already expired.
	 */
	public void refreshLockTimeout(String key, String token, long timeoutSec);

	/**
	 * Release a lock using the token that was issued when the lock was acquired.
	 * 
	 * @param key   The same key that was used to acquire the lock.
	 * @param token The lock token issued from
	 *              {@link #attemptToAcquireLock(String, long, int)}
	 * @return True if the lock release was successful. False if the was not
	 *         released because it already timed out and was forcefully released.
	 * 
	 * @throws LockReleaseFailedException When the given token has already expired.
	 */
	public void releaseLock(String key, String token);

	/**
	 * Force the release of all locks.
	 */
	public void releaseAllLocks();


	/**
	 * Get the context of the first lock that is not expired with the given key.
	 * 
	 * @param key
	 * @return Optional.of(Context of the first lock found). If there are no locks
	 *         for the key then Optional.empty().
	 */
	Optional<String> getFirstUnexpiredLockContext(String key);

	/**
	 * Cleaning up infrequently used locks can improve lock acquisition performance.
	 * Calling this method will delete all lock rows that currently do not have a
	 * token and are past their expiration date. We suggest calling this method from
	 * a timer thread.
	 */
	public void runGarbageCollection();

	/**
	 * Get the number of lock rows in the database.
	 * 
	 * @return
	 */
	public long getLockRowCount();
}
