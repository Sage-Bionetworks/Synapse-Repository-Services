package org.sagebionetworks.workers.util.semaphore;

import java.io.IOException;
import java.util.Optional;

import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.sagebionetworks.util.progress.ProgressListener;

class WriteLockImpl implements WriteLock {

	private final CountingSemaphore countingSemaphore;
	private final WriteLockRequest request;
	private final String readLockKey;
	private final String writeLockKey;
	private String writeToken;
	private ProgressListener listener;

	public WriteLockImpl(CountingSemaphore countingSemaphore, WriteLockRequest request) {
		super();
		if(countingSemaphore == null) {
			throw new IllegalArgumentException("CountingSemaphore cannot be null");
		}
		this.countingSemaphore = countingSemaphore;
		if(request == null) {
			throw new IllegalArgumentException("WriteLockRequest cannot be null");
		}
		this.request = request;
		this.readLockKey = Constants.createReaderLockKey(request.getLockKey());
		this.writeLockKey = Constants.createWriterLockKey(request.getLockKey());
	}

	void attemptToAcquireLock() {
		// reserve a writer token if possible
		Optional<String> tokenOptional = this.countingSemaphore.attemptToAcquireLock(writeLockKey,
				request.getCallback().getLockTimeoutSeconds(), Constants.WRITER_MAX_LOCKS, request.getCallersContext());
		if (tokenOptional.isEmpty()) {
			throw new LockUnavilableException(LockType.Write, request.getLockKey(),
					this.countingSemaphore.getFirstUnexpiredLockContext(writeLockKey).orElse(null));
		}
		this.writeToken = tokenOptional.get();

		// Listen to progress events
		listener = () -> {
			// as progress is made refresh the write lock
			countingSemaphore.refreshLockTimeout(writeLockKey, this.writeToken,
					request.getCallback().getLockTimeoutSeconds());
		};
		request.getCallback().addProgressListener(listener);
	}

	@Override
	public void close() throws Exception {
		Exception lastException = null;
		if (this.listener != null) {
			try {
				request.getCallback().removeProgressListener(listener);
			} catch (Exception e) {
				lastException = e;
			}
		}
		if (this.writeToken != null) {
			try {
				countingSemaphore.releaseLock(this.writeLockKey, this.writeToken);
			} catch (Exception e) {
				lastException = e;
			}
		}
		if(lastException != null) {
			throw new IOException(lastException);
		}
	}

	@Override
	public Optional<String> getExistingReadLockContext() {
		return countingSemaphore.getFirstUnexpiredLockContext(this.readLockKey);
	}

}
