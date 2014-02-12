package org.sagebionetworks.repo.model.dao.semaphore;

import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.exception.LockUnavilableException;

/**
 * An abstraction for a semaphore that will run a {@link Callable} while holding
 * either a write-lock (exclusive) or read-lock (shared). The locks will be
 * unconditionally released when the runner terminates either normally or with
 * exception.
 * <p>
 * This abstraction extends the functionality of {@link ExclusiveOrSharedSemaphoreDao}.
 * </p>
 * @author John
 * 
 */
public interface ExclusiveOrSharedSemaphoreRunner {

	/**
	 * <p>
	 * The passed runner will be run while holding the exclusive lock on the
	 * passed resource. The lock will be unconditionally released when the
	 * runner terminates either normally or with exception.
	 * </p>
	 * <p>
	 * Note: In order to acquire a write-lock this method might need to wait for
	 * outstanding read-locks to be released.
	 * </p>
	 * 
	 * @param lockKey
	 *            The key that identifies the resource to lock on.
	 * @param lockTimeoutMS
	 *            The maximum number of milliseconds that the lock will be held
	 *            for. This must be greater than the amount of time the passed
	 *            runner is expected to run.
	 * @param runner
	 *            The call() method of this runner will be called while the lock
	 *            is being held.
	 * @return
	 * @throws LockUnavilableException
	 *             Thrown if the requested lock cannot be acquired for any
	 *             reason.
	 * @throws InterruptedException Thrown if the waiting gets interrupted.
	 * @throws Exception 
	 */
	public <T> T runWithExclusiveLock(String lockKey, long lockTimeoutMS,
			Callable<T> runner) throws LockUnavilableException, InterruptedException, Exception;

	/**
	 * <p>
	 * The passed runner will be run while holding a read-lock (shared) on the
	 * passed resource. The lock will be unconditionally released when the
	 * runner terminates either normally or with exception.
	 * </p>
	 * 
	 * @param lockKey
	 *            The key that identifies the resource to lock on.
	 * @param lockTimeoutMS
	 *            The maximum number of milliseconds that the lock will be held
	 *            for. This must be greater than the amount of time the passed
	 *            runner is expected to run.
	 * @param runner
	 *            The call() method of this runner will be called while the lock
	 *            is being held.
	 * @return
	 * @throws LockUnavilableException
	 *             Thrown if the requested lock cannot be acquired for any
	 *             reason.
	 * @throws Exception 
	 */
	public <T> T runWithSharedLock(String lockKey, long lockTimeoutMS,
			Callable<T> runner) throws LockUnavilableException, Exception;

}
