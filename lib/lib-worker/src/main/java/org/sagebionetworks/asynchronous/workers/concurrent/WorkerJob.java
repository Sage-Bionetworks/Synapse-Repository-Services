package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.concurrent.Future;

import org.sagebionetworks.common.util.progress.ProgressListener;

/**
 * Simple wrapper to track a work's {@link Future} and {@link ProgressListener}.
 *
 */
public class WorkerJob {

	private final Future<Void> future;
	private final ProgressListener listener;

	public WorkerJob(Future<Void> future, ProgressListener listener) {
		super();
		this.future = future;
		this.listener = listener;
	}

	/**
	 * Future used to track the progress of a worker.
	 * 
	 * @return the future
	 */
	public Future<Void> getFuture() {
		return future;
	}

	/**
	 * The listener used to refresh all of the worker's locks.
	 * 
	 * @return the listener
	 */
	public ProgressListener getListener() {
		return listener;
	}

}
