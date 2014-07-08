package org.sagebionetworks.util;

public interface Clock {

	public abstract long currentTimeMillis();

	public abstract void sleep(long millis) throws InterruptedException;

	public abstract void sleepNoInterrupt(long millis);

}