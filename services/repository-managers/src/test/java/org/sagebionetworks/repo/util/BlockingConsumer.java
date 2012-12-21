package org.sagebionetworks.repo.util;

import org.sagebionetworks.repo.util.FixedMemoryPool.BlockConsumer;

/**
 * This is a test consumer that will block until told to stop.
 * 
 * @author John
 *
 */
public class BlockingConsumer implements BlockConsumer<Long>{
	
	/**
	 * How long this runner will sleep while blocking.
	 */
	public static final long SLEEP_MS = 100;
	/**
	 * This must be volatile as it is read and modified from concurrent threads.
	 */
	private volatile boolean blocking = true;

	/**
	 * Set to false to stop blocking.
	 * @param blocking
	 */
	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	@Override
	public Long useBlock(byte[] block) throws Exception {
		long start = System.currentTimeMillis();
		while(blocking){
			// Sleep until told to stop.
			Thread.sleep(SLEEP_MS);
		}
		return System.currentTimeMillis()-start;
	}
	
}