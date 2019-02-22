package org.sagebionetworks.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SingleThreadRunner<T> {
	Future<T> future;
	Callable<T> callable;
	ExecutorService executorService = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());

	public SingleThreadRunner(Callable<T> callable) {
		this.callable = callable;
	}

	public void start() {
		assertNull("Always call get() after each start()", future);
		future = executorService.submit(callable);
	}

	public T get() throws Exception {
		assertNotNull("Only call get() once after start()", future);
		T result = future.get();
		future = null;
		return result;
	}

	public void stop() {
		executorService.shutdownNow();
		try {
			executorService.awaitTermination(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
		}
	}
}

