package org.sagebionetworks.workers.util.semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.util.Clock;
import org.sagebionetworks.util.progress.ProgressingCallable;

public class WriteReadSemaphoreImpl implements WriteReadSemaphore {

	private static final Logger log = LogManager.getLogger(WriteReadSemaphoreImpl.class);

	private final CountingSemaphore countingSemaphore;
	private final int maxNumberOfReaders;
	private final Clock clock;

	public WriteReadSemaphoreImpl(CountingSemaphore countingSemaphore, int maxNumberOfReaders, Clock clock) {
		if (countingSemaphore == null) {
			throw new IllegalArgumentException("CountingSemaphore cannot be null");
		}
		if (clock == null) {
			throw new IllegalArgumentException("Clock cannot be null");
		}
		this.countingSemaphore = countingSemaphore;
		this.maxNumberOfReaders = maxNumberOfReaders;
		this.clock = clock;
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

	@Override
	public <R> R tryRunWithReadLock(ReadLockRequest request, ProgressingCallable<R> runner) {
		try (ReadLock lock = getReadLock(request)) {
			return runner.call(request.getCallback());
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public <R> R tryRunWithWriteLock(WriteLockRequest request, ProgressingCallable<R> runner) {
		try (WriteLock lock = getWriteLock(request)) {
			int count = 1;
			while ((lock.getExistingReadLockContext()).isPresent()) {
				if (count > request.getMaxReaderWaitAttempts().get()) {
					throw new LockUnavilableException(LockType.Write, request.getLockKey(),
							request.getCallersContext());
				}
				clock.sleep(request.getWaitForReaderMS().get());
				count++;
			}
			return runner.call(request.getCallback());
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
