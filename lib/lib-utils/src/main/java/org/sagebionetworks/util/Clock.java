package org.sagebionetworks.util;

import java.util.Date;

import org.sagebionetworks.common.util.progress.ProgressCallback;

public interface Clock {

	public abstract long currentTimeMillis();

	public abstract void sleep(long millis) throws InterruptedException;

	public abstract void sleepNoInterrupt(long millis);



	public abstract Date now();
}