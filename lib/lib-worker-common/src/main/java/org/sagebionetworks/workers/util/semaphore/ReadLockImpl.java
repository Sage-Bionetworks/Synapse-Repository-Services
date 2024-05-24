package org.sagebionetworks.workers.util.semaphore;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.util.progress.ProgressListener;

class ReadLockImpl implements ReadLock {

	private static final Logger log = LogManager.getLogger(ReadLockImpl.class);

	private final CountingSemaphore countingSemaphore;
	private final int maxNumberOfReaders;
	private final ReadLockRequest request;
	private final Map<String, String> keyToTokenMap;
	private ProgressListener listener;

	public ReadLockImpl(CountingSemaphore countingSemaphore, int maxNumberOfReaders, ReadLockRequest request) {
		if(countingSemaphore == null) {
			throw new IllegalArgumentException("CountingSemaphore cannot be null");
		}
		this.countingSemaphore = countingSemaphore;
		this.maxNumberOfReaders = maxNumberOfReaders;
		if(request == null) {
			throw new IllegalArgumentException("ReadLockRequest cannot be null");
		}
		this.request = request;
		this.keyToTokenMap = new HashMap<>(request.getLockKeys().length);
	}
	
	void attemptToAcquireLock() throws LockUnavilableException {
		// Stop if there are any outstanding write locks.
		for (String requestKey : request.getLockKeys()) {
			String writeLockKey = Constants.createWriterLockKey(requestKey);
			Optional<String> existingContext = countingSemaphore.getFirstUnexpiredLockContext(writeLockKey);
			if (existingContext.isPresent()) {
				throw new LockUnavilableException(LockType.Write, requestKey, existingContext.get());
			}
		}

		// acquire a read lock for each key
		for (String requestKey : request.getLockKeys()) {
			String readLockKey = Constants.createReaderLockKey(requestKey);
			Optional<String> readToken = this.countingSemaphore.attemptToAcquireLock(readLockKey,
					request.getCallback().getLockTimeoutSeconds(), maxNumberOfReaders, request.getCallersContext());
			if (readToken.isEmpty()) {
				throw new LockUnavilableException(LockType.Read, requestKey, this.countingSemaphore
						.getFirstUnexpiredLockContext(readLockKey).orElse(null));
			}
			keyToTokenMap.put(readLockKey, readToken.get());
		}

		// listen to callback events
		this.listener = () -> {
			Iterator<String> iterator = keyToTokenMap.keySet().iterator();
			while (iterator.hasNext()) {
				String readLockKey = iterator.next();
				String readToken = keyToTokenMap.get(readLockKey);
				countingSemaphore.refreshLockTimeout(readLockKey, readToken,
						request.getCallback().getLockTimeoutSeconds());
			}
		};
		request.getCallback().addProgressListener(listener);
	}

	@Override
	public void close() throws IOException {
		if (this.listener != null) {
			request.getCallback().removeProgressListener(this.listener);
		}
		Iterator<String> iterator = keyToTokenMap.keySet().iterator();
		Exception lastException = null;
		while (iterator.hasNext()) {
			// each lock must be released even if some of the lock release attempts fail.
			try {
				String readLockKey = iterator.next();
				String readToken = keyToTokenMap.get(readLockKey);
				countingSemaphore.releaseLock(readLockKey, readToken);
			} catch (Exception e) {
				lastException = e;
				log.error("Failed to release lock:", e);
			}
		}
		if (lastException != null) {
			throw new IOException(lastException);
		}
	}

}
