package org.sagebionetworks.repo.model.semaphore;

import org.sagebionetworks.database.semaphore.CountingSemaphore;

/**
 * A synchronized counting semaphore that is used as a singleton to gate the
 * number of semaphore requests per machine.  This was added as a fix for PLFM-4027.
 *
 */
public class SynchronizedCountingSemaphore implements CountingSemaphore {
	
	private CountingSemaphore wrapped;
	
	public SynchronizedCountingSemaphore(CountingSemaphore wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public synchronized String attemptToAcquireLock(String key, long timeoutSec,
			int maxLockCount) {
		return wrapped.attemptToAcquireLock(key, timeoutSec, maxLockCount);
	}

	@Override
	public synchronized void refreshLockTimeout(String key, String token, long timeoutSec) {
		wrapped.refreshLockTimeout(key, token, timeoutSec);
	}

	@Override
	public synchronized void releaseLock(String key, String token) {
		wrapped.releaseLock(key, token);
	}

	@Override
	public synchronized void releaseAllLocks() {
		wrapped.releaseAllLocks();
	}

}
