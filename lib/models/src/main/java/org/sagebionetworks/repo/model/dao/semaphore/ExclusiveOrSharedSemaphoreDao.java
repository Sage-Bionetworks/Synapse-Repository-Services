package org.sagebionetworks.repo.model.dao.semaphore;

import org.sagebionetworks.repo.model.exception.LockReleaseFailedException;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;

/**
 * An abstraction for a Semaphore that issues either exclusive or shared locks.
 * This type of semaphore is useful for scenarios where there can be only a
 * single writer (exclusive) or many readers (shared), but never both at the
 * same time.
 * 
 * Implementations of this DAO will have the following characteristics:
 * <ul>
 * <li>An existing write-lock (exclusive) means there are no outstanding
 * read-locks (shared) nor are there any other outstanding write-locks. There
 * can be only one write-lock at a time.</li>
 * <li>An existing read-lock means there can be many other outstanding
 * read-locks but no outstanding write-locks.</li>
 * <li>Acquiring a write-lock is a two phase operation:
 * <ul>
 * <li>First, a write-lock request token must be acquired. Once the
 * write-request token has been issued, all new read-lock requests will be
 * rejected.</li>
 * <li>The write-lock request token must be used to acquire the write-lock. As
 * soon as all outstanding read-locks are release, the write-lock can be issued
 * to the request token holder.</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * 
 * @author jmhill
 * 
 */
public interface ExclusiveOrSharedSemaphoreDao {

	/**
	 * Attempt to acquire a read-lock (shared) on the resource identified by the
	 * passed lock Key.
	 * 
	 * @param lockKey
	 *            Identifies the resource to lock.
	 * @param The
	 *            maximum number of milliseconds that this read-lock will be
	 *            held. If a lock is not release before this timeout it might be
	 *            forcefully released in order to acquire a write-lock.
	 * @return The token of the read-lock is returned when a lock is acquired.
	 *         Will never return null.
	 * @throws LockUnavilableException
	 *             Throw when the lock cannot be acquired at this time. This
	 *             will occur when another caller either requested a write-lock
	 *             (exclusive) or is holding a write-lock.
	 */
	public String aquireSharedLock(String lockKey, long timeoutMS)
			throws LockUnavilableException;

	/**
	 * When a read-lock (shared) is acquired, it is the responsibly of the lock
	 * holder to release the lock using this method.
	 * 
	 * @param lockKey
	 *            Identifies the resource that was locked.
	 * @param token
	 *            The token issued when the read-lock was acquired.
	 * @throws LockReleaseFailedException
	 *             This is thrown when the lock was forcibly revoked because it
	 *             was not release before the requested timeout expired. If this
	 *             occurs, the timeout set when the lock was acquired might be
	 *             too low.
	 */
	public void releaseSharedLock(String lockKey, String token)
			throws LockReleaseFailedException;

	/**
	 * The first phase of acquiring a write-lock (exclusive) on a resource. It
	 * indicates the desire to acquire a write-lock. When successful, a request
	 * token will be issued. This token must be provide to complete the
	 * write-lock acquisition(see:
	 * {@link #aquireExclusiveLock(String, String, long)}. Once a write-lock
	 * request token has been issued, all new read-lock requests with the same
	 * lock key will be rejected.
	 * <p>
	 * Note: The write-lock request token has a short automatic timeout. It must
	 * be used to attempt to get the write-lock within 5 seconds. However, each
	 * attempt to acquire the final write-lock will reset this timer. The
	 * request token can be held indefinitely as long as subsequent write-lock
	 * Acquisition attempts continue to be made.
	 * </p>
	 * 
	 * @param lockKey
	 *            Identifies the resource to lock.
	 * @return
	 * @throws LockUnavilableException
	 *             Throw when the lock cannot be acquired at this time. This
	 *             will occur when another caller either requested a write-lock
	 *             (exclusive) or is holding a write-lock.
	 */
	public String requestExclusiveLockToken(String lockKey)
			throws LockUnavilableException;

	/**
	 * The second phase of acquiring a write-lock (exclusive) on a resource.
	 * Once a write-lock request token has been issued with
	 * {@link #requestExclusiveLockToken(String)} the caller must wait for
	 * all outstanding read-locks (shared) to be released before the actual
	 * write-lock can be issued.  Each time this method is called with a valid request token
	 * a check for outstanding read-locks will be made.  When there are no longer any outstanding
	 * read-locks a write-lock will be issued to the caller identified by the returned token.
	 * Each time this method is called, the timeout on the write-lock request token will be reset.
	 * 
	 * @param lockKey
	 * @param requestToken
	 * @param timeoutMS
	 * @return
	 */
	public String aquireExclusiveLock(String lockKey, String requestToken,
			long timeoutMS);

	/**
	 * When a write-lock (exclusive) is acquired, it is the responsibly of the
	 * lock holder to release the lock using this method.
	 * 
	 * @param lockKey
	 *            Identifies the resource that was locked.
	 * @param token
	 *            The token issued when the write-lock was acquired.
	 * @throws LockReleaseFailedException
	 *             This is thrown when the lock was forcibly revoked because it
	 *             was not release before the requested timeout expired. If this
	 *             occurs, the timeout set when the lock was acquired might be
	 *             too low.
	 */
	public void releaseExclusiveLock(String lockKey, String token)
			throws LockReleaseFailedException;

}
