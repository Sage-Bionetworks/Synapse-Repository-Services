package org.sagebionetworks.util;

import java.util.Date;

public interface Clock {
	
	long nanoTime();

	long currentTimeMillis();

	void sleep(long millis) throws InterruptedException;

	void sleepNoInterrupt(long millis);

	Date now();
}