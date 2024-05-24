package org.sagebionetworks.util.progress;

import java.util.concurrent.Callable;

/**
 * Similar to {@link Callable} but the call method is provided with a
 * {@link ProgressCallback} that can be use to notify a container that progress
 * is still being made.
 * 
 * @param <R>
 *            The return type of the callable.
 *            {@link #call(Object)}.
 * */
public interface ProgressingCallable<R> {

	/**
	 * Similar to {@link Callable#call()} except a {@link ProgressCallback} is
	 * provided so the callable can notify a container that progress is still
	 * being made.
	 * 
	 * @param callback <R>
	 * @return
	 * @throws Exception
	 */
	public R call(ProgressCallback callback) throws Exception;

}
