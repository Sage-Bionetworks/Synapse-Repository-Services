package org.sagebionetworks.util;

import java.util.Date;

public interface Clock {

	public abstract long currentTimeMillis();

	public abstract void sleep(long millis) throws InterruptedException;

	public abstract void sleepNoInterrupt(long millis);

	public abstract void sleepWithFrequentCallback(long millis, long frequencyInMillis, ProgressCallback<Long> progressCallback);

	public abstract Date now();
}