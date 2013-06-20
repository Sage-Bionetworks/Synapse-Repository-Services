package org.sagebionetworks.repo.model.dao.semaphore;

/**
 * An abstraction for a job runner that is gated by a semaphore.
 * The basic idea is to run if a lock can be acquired from a semaphore.
 * The implementation should be non-blocking.  This means it should not wait for
 * a lock to be acquired if one is not available.
 * 
 * @author John
 *
 */
public interface SemaphoreGatedRunner {

	/**
	 * This should be called from a timer.  When fired, an attempt should
	 * be made to acquire a lock from a semaphore.  If the lock is acquired,
	 * the the runner associated with the gate should be run().  After the runner
	 * terminates, either successfully or with exception the lock should be release.
	 * If a lock cannot be acquired, the method should terminate immediately (non-blocking).
	 * 
	 * Note: The implementation does not need to guarantee that if a lock is available, it will be
	 * acquired.  Instead, the caller should continue to attempt to run at a regular interval.
	 */
	public void attemptToRun();
}
