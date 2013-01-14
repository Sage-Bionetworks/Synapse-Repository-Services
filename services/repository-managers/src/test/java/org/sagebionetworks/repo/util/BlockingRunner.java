package org.sagebionetworks.repo.util;

import java.util.concurrent.Callable;

/**
 * This is a simple test runner that will block until told to stop.
 * 
 * @author John
 *
 */
public class BlockingRunner implements Callable<Boolean> {
	
	/**
	 * How long this runner will sleep while blocking.
	 */
	public static final long SLEEP_MS = 100;
	/**
	 * This must be volatile as it is set and read from different threads.
	 * 
	 */
	private volatile boolean blocking = true;

	/**
	 * When set to true, the run method will block until until told to stop.
	 * @param blocking
	 */
	public void setBlocking(boolean blocking){
		this.blocking = blocking;
	}

	@Override
	public Boolean call() throws Exception {
		// Block until told to stop.
		while(blocking){
			try {
				Thread.sleep(SLEEP_MS);
			} catch (InterruptedException e) {
				// stop on Interrupt
				return blocking;
			}
		}
		return blocking;
	}
	
}