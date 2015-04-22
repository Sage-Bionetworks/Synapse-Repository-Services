package org.sagebionetworks.util;

import java.util.Date;

public class DefaultClock implements Clock {

	@Override
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	@Override
	public void sleep(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}

	@Override
	public void sleepNoInterrupt(long millis) {
		try {
			sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public void sleepWithFrequentCallback(long millis, long frequencyInMillis, ProgressCallback<Long> progressCallback) {
		for (long i = millis; i > 0; i -= frequencyInMillis) {
			sleepNoInterrupt(Math.min(frequencyInMillis, i));
			if (progressCallback != null) {
				progressCallback.progressMade(i);
			}
		}
	}

	@Override
	public Date now() {
		return new Date();
	}
}
