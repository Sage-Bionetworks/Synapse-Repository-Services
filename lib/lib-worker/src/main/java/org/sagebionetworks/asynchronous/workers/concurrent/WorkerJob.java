package org.sagebionetworks.asynchronous.workers.concurrent;

import java.util.concurrent.Future;

import org.sagebionetworks.common.util.progress.ProgressListener;

public class WorkerJob {
	
	private final Future<Void> future;
	private final ProgressListener listener;
	
	
	public WorkerJob(Future<Void> future, ProgressListener listener) {
		super();
		this.future = future;
		this.listener = listener;
	}


	/**
	 * @return the future
	 */
	public Future<Void> getFuture() {
		return future;
	}


	/**
	 * @return the listener
	 */
	public ProgressListener getListener() {
		return listener;
	}
	
	

}
