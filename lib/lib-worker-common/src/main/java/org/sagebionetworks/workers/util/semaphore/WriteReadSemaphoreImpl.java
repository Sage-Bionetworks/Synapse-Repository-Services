package org.sagebionetworks.workers.util.semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.database.semaphore.CountingSemaphore;

public class WriteReadSemaphoreImpl implements WriteReadSemaphore {

	private static final Logger log = LogManager.getLogger(WriteReadSemaphoreImpl.class);

	final CountingSemaphore countingSemaphore;
	final int maxNumberOfReaders;

	public WriteReadSemaphoreImpl(CountingSemaphore countingSemaphore, int maxNumberOfReaders) {
		if (countingSemaphore == null) {
			throw new IllegalArgumentException("CountingSemaphore cannot be null");
		}
		this.countingSemaphore = countingSemaphore;
		this.maxNumberOfReaders = maxNumberOfReaders;
	}

	@Override
	public WriteLock getWriteLock(WriteLockRequest request) throws LockUnavilableException {
		if (request == null) {
			throw new IllegalArgumentException("Request cannot be null");
		}
		WriteLockImpl lock = createWriteLock(request);
		try {
			lock.attemptToAcquireLock();
			return lock;
		} catch (Exception e) {
			try {
				lock.close();
			} catch (Exception closeException) {
				log.error("Error on close:", closeException);
			}
			if (e instanceof LockUnavilableException) {
				throw (LockUnavilableException) e;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	WriteLockImpl createWriteLock(WriteLockRequest request) {
		return new WriteLockImpl(countingSemaphore, request);
	}

	@Override
	public ReadLock getReadLock(ReadLockRequest request) throws LockUnavilableException {
		if (request == null) {
			throw new IllegalArgumentException("Request cannot be null");
		}
		ReadLockImpl lock = createReadLock(request);
		try {
			lock.attemptToAcquireLock();
			return lock;
		} catch (Exception e) {
			try {
				lock.close();
			} catch (Exception closeException) {
				log.error("Error on close:", closeException);
			}
			if (e instanceof LockUnavilableException) {
				throw (LockUnavilableException) e;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	ReadLockImpl createReadLock(ReadLockRequest request) {
		return new ReadLockImpl(countingSemaphore, maxNumberOfReaders, request);
	}
}
