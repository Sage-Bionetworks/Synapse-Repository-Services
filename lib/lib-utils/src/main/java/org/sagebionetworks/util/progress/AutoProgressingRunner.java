package org.sagebionetworks.util.progress;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple wrapper for {@link AutoProgressingCallable}
 *
 */
public class AutoProgressingRunner implements ProgressingRunner {

	AutoProgressingCallable<Void> callable;

	public AutoProgressingRunner(final ProgressingRunner runner, long progressFrequencyMs){
		int numberOfThreads = 1;
		ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
		callable = new AutoProgressingCallable<Void>(executor, new ProgressingCallable<Void>() {

			@Override
			public Void call(ProgressCallback callback) throws Exception {
				runner.run(callback);
				return null;
			}
		}, progressFrequencyMs);
	}

	@Override
	public void run(ProgressCallback progressCallback) throws Exception {
		callable.call(progressCallback);
	}
	
}
