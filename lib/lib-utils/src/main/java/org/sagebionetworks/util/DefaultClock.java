package org.sagebionetworks.util;


public class DefaultClock implements Clock {

	/* (non-Javadoc)
	 * @see org.sagebionetworks.util.Clock2#currentTimeMillis()
	 */
	@Override
	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.util.Clock2#sleep(long)
	 */
	@Override
	public void sleep(long millis) throws InterruptedException {
		Thread.sleep(millis);
	}

	/* (non-Javadoc)
	 * @see org.sagebionetworks.util.Clock2#sleepNoInterrupt(long)
	 */
	@Override
	public void sleepNoInterrupt(long millis) {
		try {
			sleep(millis);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}
