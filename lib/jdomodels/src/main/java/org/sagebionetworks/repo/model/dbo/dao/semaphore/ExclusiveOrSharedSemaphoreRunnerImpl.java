package org.sagebionetworks.repo.model.dbo.dao.semaphore;

import java.util.concurrent.Callable;

import org.sagebionetworks.repo.model.dao.semaphore.ExclusiveOrSharedSemaphoreDao;
import org.sagebionetworks.repo.model.dao.semaphore.ExclusiveOrSharedSemaphoreRunner;
import org.sagebionetworks.repo.model.exception.LockUnavilableException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic implementation ExclusiveOrSharedSemaphoreRunner that leverages the
 * ExclusiveOrSharedSemaphoreDao to issue/release all locks.
 * 
 * @author John
 * 
 */
public class ExclusiveOrSharedSemaphoreRunnerImpl implements
		ExclusiveOrSharedSemaphoreRunner {

	public static long WAIT_FOR_READ_RELEASE_MS = 1000;
	
	@Autowired
	ExclusiveOrSharedSemaphoreDao exclusiveOrSharedSemaphoreDao;

	@Override
	public <T> T runWithExclusiveLock(String lockKey, long lockTimeoutMS,
			Callable<T> runner) throws Exception {
		// First we need to acquire the write-lock-precursor to block all new read-locks
		String writeLockPrecursor = exclusiveOrSharedSemaphoreDao.acquireExclusiveLockPrecursor(lockKey);
		String writeLock = null;
		long start = System.currentTimeMillis();
		try {
			while (writeLock == null) {
				// Break out of the loop if we have failed to acquire the write-lock before the given timeout
				if((System.currentTimeMillis()-start) > lockTimeoutMS) throw new LockUnavilableException("Failed to acquire a write-lock on: "+lockKey+" before timing-out.");
				// Try to get the lock
				writeLock = exclusiveOrSharedSemaphoreDao.acquireExclusiveLock(
						lockKey, writeLockPrecursor, lockTimeoutMS);
				// Did we get a lock yet?
				if (writeLock == null) {
					// We need to wait for all outstanding read locks to be released.
					Thread.sleep(WAIT_FOR_READ_RELEASE_MS);
				}
			}
			// Now that we are holding the write-lock we can call the caller
			return runner.call();
		} finally {
			// We must release the write lock if one was issued.
			if(writeLock != null){
				exclusiveOrSharedSemaphoreDao.releaseExclusiveLock(lockKey, writeLock);
			}
		}
	}

	@Override
	public <T> T runWithSharedLock(String lockKey, long lockTimeoutMS,
			Callable<T> runner) throws Exception {
		// Acquire a read-lock on this resource
		String readLockToken = exclusiveOrSharedSemaphoreDao.acquireSharedLock(lockKey, lockTimeoutMS);
		try{
			// Call the runner while we hold the read-lock
			return runner.call();
		}finally{
			// Unconditionally release the read-lock
			exclusiveOrSharedSemaphoreDao.releaseSharedLock(lockKey, readLockToken);
		}
	}

}
