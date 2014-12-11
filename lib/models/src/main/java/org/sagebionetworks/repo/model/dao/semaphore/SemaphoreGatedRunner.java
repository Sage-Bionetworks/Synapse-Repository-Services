package org.sagebionetworks.repo.model.dao.semaphore;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

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
	
	/**
	 * This should be called when access should be granted if at all possible. When called, attempts are made to acquire
	 * one of the lock from a semaphore. If the lock is acquired, the the runner associated with the gate should be
	 * run(). After the runner terminates, either successfully or with exception the lock should be release. If none of
	 * the locks can be acquired, the method should terminates immediately (eventually non-blocking).
	 * 
	 * Note: The implementation does not need to guarantee that if a lock is available, it will be acquired, but the
	 * method will try each possible slot once
	 */
	public <T> T attemptToRunAllSlots(Callable<T> task, String extraKey) throws Exception;

	/**
	 * Returns all keys needed to prevent this runner from starting any workers (For testing purposes)
	 */
	public List<String> getAllLockKeys(String extraKey);
}
