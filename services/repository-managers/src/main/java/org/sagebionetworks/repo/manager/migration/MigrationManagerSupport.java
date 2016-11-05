package org.sagebionetworks.repo.manager.migration;

import java.util.concurrent.Callable;

import org.sagebionetworks.common.util.progress.ProgressCallback;

public interface MigrationManagerSupport {
	
	/**
	 * Execute the given callback with automatic progress events generated for
	 * the provided callback. This allows the callable to run for long periods
	 * of time while maintaining progress events.
	 * 
	 * @param callback
	 *            Progress events will be generated for the provided callback at
	 *            a fix frequency regardless of the amount of time the callable
	 *            takes to execute.
	 * 
	 * @param parameter
	 *            The parameter to pass to the callback.
	 * 
	 * @param callable
	 *            The callable to be executed.
	 * @return
	 * @throws Exception 
	 */
	public <R> R callWithAutoProgress(ProgressCallback<Void> callback, Callable<R> callable) throws Exception;
	
}
