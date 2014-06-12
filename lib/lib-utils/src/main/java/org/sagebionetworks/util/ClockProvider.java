package org.sagebionetworks.util;

/**
 * Abstraction for a Clock.
 * 
 * @author mblonk
 *
 */
public interface ClockProvider {
	
	public long currentTimeMillis();

	public void sleep(long millis) throws InterruptedException;
}