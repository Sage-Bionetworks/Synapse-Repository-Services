package org.sagebionetworks.workers.util.semaphore;

import org.sagebionetworks.database.semaphore.CountingSemaphore;

/**
 * An abstraction to provide both read (shared) or write (exclusive) locks for a
 * cluster of machines. This functionality is built on top of the database
 * backed {@link CountingSemaphore}. If a lock cannot be issued immediately a
 * {@link LockUnavilableException} with be thrown without waiting
 * (non-blocking).
 * 
 */
public interface WriteReadSemaphore {

	/**
	 * Get a write lock for the given request. A write lock can be acquired as long
	 * as there are no other write locks with the same key. When a write lock is
	 * acquired, it will block the creation of new read locks, but exist read locks
	 * will be unchanged. The caller can check if there are existing read locks by
	 * calling {@link WriteLock#getExistingReadLockContext()}.
	 * <p>
	 * See the following example:
	 * 
	 * <pre>
	 * try (WriteLock lock = writeReadSemaphore.getWriteLock(request)) {
	 * 	// wait for all read locks to be released
	 * 	Optional<String> readerContextOption;
	 * 	while ((readerContextOption = lock.getExistingReadLockContext()).isPresent()) {
	 * 		log.info("Waiting for read lock to be released: " + readerContextOption.get());
	 * 		Thread.sleep(2000);
	 * 	}
	 * 	// code to execute while holding the lock added here...
	 * }
	 * </pre>
	 * 
	 * 
	 * @param request
	 * @return
	 * @throws LockUnavilableException Throw if the write lock cannot be acquired.
	 * 
	 */
	WriteLock getWriteLock(WriteLockRequest request) throws LockUnavilableException;

	/**
	 * Get a read lock for the given request. Multiple read locks can be acquired
	 * for the same key concurrently. There is a limit on the number of read locks
	 * allowed for a single key that is configured in the constructor:
	 * {@link WriteReadSemaphoreImpl#WriteReadSemaphoreImpl(org.sagebionetworks.database.semaphore.CountingSemaphore, int)}.
	 * While a write lock is held for a key, new read locks cannot be acquired until
	 * the write lock is released. However, any read locks acquired before a write
	 * lock is acquired will be allowed to continue normally.
	 * 
	 * <pre>
	 * try (ReadLock lock = writeReadSemaphore.getReadLock(request)) {
	 * 	// code to execute while holding the lock added here...
	 * }
	 * </pre>
	 * 
	 * @param request
	 * @return
	 * @throws LockUnavilableException Thrown if any of the requested read locks
	 *                                 cannot be be acquired.
	 */
	ReadLock getReadLock(ReadLockRequest request) throws LockUnavilableException;
}
