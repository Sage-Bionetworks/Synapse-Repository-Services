package org.sagebionetworks.util.progress;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A thread-safe abstract {@link ProgressCallback} that will only allow a single
 * instance of each listeners type at a time.
 * 
 * Use {@link #fireProgressMade(Object)} to forward progress events to all
 * listeners.
 *
 */
public class SynchronizedProgressCallback implements ProgressCallback {

	private static final Logger log = LogManager
			.getLogger(SynchronizedProgressCallback.class);
	/*
	 * The map of all listeners. This map is not synchronized, so all access to
	 * it should only occur in synchronized methods. LinkedHashMap is used to
	 * ensure progress events are fired in the same order as listeners are
	 * added.
	 */
	private Map<Class<? extends ProgressListener>, ProgressListener> listeners = new LinkedHashMap<Class<? extends ProgressListener>, ProgressListener>();
	private long lockTimeoutSec;
	
	/**
	 * 
	 * @param lockTimeoutSec The timeout used by the stack in seconds.
	 */
	public SynchronizedProgressCallback(long lockTimeoutSec) {
		super();
		this.lockTimeoutSec = lockTimeoutSec;
	}

	/**
	 * Forward a progress event to all listeners.
	 * 
	 * @param t
	 */
	protected synchronized void fireProgressMade() {
		List<Class<? extends ProgressListener>> toRemove = new ArrayList<>();
		listeners.forEach((key, listener) -> {
			try {
				listener.progressMade();
			} catch (Exception e) {
				log.error(String.format("ProgressListener: '%s' failed and will be removed.", key), e);
				toRemove.add(key);
			}
		});
		toRemove.forEach(key -> {
			listeners.remove(key);
		});
	}


	/**
	 * 
	 */
	@Override
	public synchronized void addProgressListener(ProgressListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Listener cannot be null");
		}
		Class<? extends ProgressListener> key = (Class<? extends ProgressListener>) listener
				.getClass();
		if (listeners.containsKey(key)) {
			throw new IllegalArgumentException(
					"Cannot add more than one listener of type: "
							+ key.getName()
							+ ".  Please remove the previously added listener before adding an additional listener.");
		}
		listeners.put(key, listener);
	}

	@Override
	public synchronized void removeProgressListener(ProgressListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException("Listener cannot be null");
		}
		Class<? extends ProgressListener> key = (Class<? extends ProgressListener>) listener
				.getClass();
		listeners.remove(key);
	}

	@Override
	public long getLockTimeoutSeconds() {
		return lockTimeoutSec;
	}

}
