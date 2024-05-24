package org.sagebionetworks.util.progress;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An implementation of ProgressingCallable that can provide progress events for
 * a {@link java.util.concurrent.Callable}. The provided Callable will be run on
 * separate thread. While the Callable is running progress events will be made
 * on the original calling thread at the provided frequency.
 * 
 * @param <R>
 *            The return type of the callable.
 *            {@link #call(Object)}.
 */
public class AutoProgressingCallable<R> implements ProgressingCallable<R> {

	ExecutorService executor;
	ProgressingCallable<R> callable;
	long progressFrequencyMs;

	/**
	 * Create a new AutoProgressingCallable for each use.
	 * 
	 * @param executor
	 *            Provides threading
	 * @param callable
	 *            Callable to be run on a separate thread. Progress events will
	 *            be made on the calling thread while the callable runs on the
	 *            separate thread.
	 * @param progressFrequencyMs
	 *            The frequency in MS at which progress events will be made
	 *            while waiting for the callable's thread.
	 * @param parameter
	 *            The parameter to be passed to the progress callback.
	 */
	public AutoProgressingCallable(ExecutorService executor,
			ProgressingCallable<R> callable, long progressFrequencyMs) {
		super();
		if(executor == null){
			throw new IllegalArgumentException("Executor cannot be null");
		}
		if(callable == null){
			throw new IllegalArgumentException("Callable cannot be null");
		}
		this.executor = executor;
		this.callable = callable;
		this.progressFrequencyMs = progressFrequencyMs;
	}

	/**
	 * The actual call method requires an {@link SynchronizedProgressCallback}.
	 * @param callback
	 * @return
	 * @throws Exception
	 */
	private R call(final SynchronizedProgressCallback callback) throws Exception {
		// start the process
		Future<R> future = executor.submit(new Callable<R>(){
			@Override
			public R call() throws Exception {
				return callable.call(callback);
			}});
		// make progress at least once.
		callback.fireProgressMade();
		while (true) {
			// wait for the process to finish
			try {
				return future.get(progressFrequencyMs, TimeUnit.MILLISECONDS);
			}catch (ExecutionException e) {
				// concert to the cause if we can.
				Throwable cause = e.getCause();
				if(cause instanceof Exception){
					throw ((Exception)cause);
				}
				throw e;
			}catch (TimeoutException e) {
				// make progress for each timeout
				callback.fireProgressMade();
			}
		}
	}

	@Override
	public R call(ProgressCallback callback) throws Exception {
		if(!(callback instanceof SynchronizedProgressCallback)){
			throw new IllegalArgumentException("ProgressCallback must extend AbstractProgressCallback");
		}
		return this.call((SynchronizedProgressCallback)callback);
	}

}
