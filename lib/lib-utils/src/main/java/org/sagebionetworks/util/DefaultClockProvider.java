package org.sagebionetworks.util;

/**
 * Basic ClockProvider that uses the system clock.
 *  
 * @author mblock
 *
 */
public class DefaultClockProvider implements ClockProvider{

	@Override
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	@Override
	public void sleep(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}

}
