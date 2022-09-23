package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.sagebionetworks.common.util.progress.ProgressCallback;
import org.sagebionetworks.common.util.progress.ProgressListener;
import org.sagebionetworks.common.util.progress.SynchronizedProgressCallback;

/**
 * A thread-safe implementation of ProgressCallback. All calls that access the
 * listeners are synchronized. Also implements ProgressListener to notify all
 * list
 */
public class ConcurrentProgressCallback implements ProgressCallback, ProgressListener {

	private static final Logger log = LogManager.getLogger(SynchronizedProgressCallback.class);

	private final Set<ProgressListener> listeners;
	private final long lockTimeoutSeconds;

	@Override
	public synchronized void addProgressListener(ProgressListener listener) {
		listeners.add(listener);
	}

	public ConcurrentProgressCallback(long lockTimeoutSeconds) {
		super();
		this.listeners = new LinkedHashSet<>();
		this.lockTimeoutSeconds = lockTimeoutSeconds;
	}

	@Override
	public synchronized void removeProgressListener(ProgressListener listener) {
		listeners.remove(listener);
	}

	@Override
	public long getLockTimeoutSeconds() {
		return lockTimeoutSeconds;
	}

	/**
	 * Forward a {@link #progressMade()} call to each listener. Note: If a listener
	 * throws an exception it will be removed from the listeners.
	 */
	@Override
	public synchronized void progressMade() {
		Iterator<ProgressListener> it = listeners.iterator();
		while (it.hasNext()) {
			try {
				ProgressListener listener = it.next();
				listener.progressMade();
			} catch (Exception e) {
				log.error(String.format("Error on progressMade for: '%s'. Listener will be removed.",
						listeners.getClass()), e);
				it.remove();
			}
		}
	}

}
