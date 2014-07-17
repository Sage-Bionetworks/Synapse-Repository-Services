package org.sagebionetworks.util;

public class TestClock extends DefaultClock {

	private long currentTime = 100000L;
	private boolean sleepIsThreaded = false;
	private int sleeperCount = 0;
	private Object lock = new Object();

	@Override
	public long currentTimeMillis() {
		return currentTime;
	}

	@Override
	public void sleep(long millis) throws InterruptedException {
		if (sleepIsThreaded) {
			long startTime = currentTime;
			synchronized (lock) {
				sleeperCount++;
				try {
					while (currentTime < startTime + millis) {
						lock.wait(60000);
					}
				} finally {
					sleeperCount--;
				}
			}
		} else {
			currentTime += millis;
		}
	}

	public void waitForSleepers(int expectedSleeperCount) {
		synchronized (lock) {
			while (expectedSleeperCount > sleeperCount) {
				try {
					lock.wait(10);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	public void setThreadedSleep(boolean sleepIsThreaded) {
		this.sleepIsThreaded = sleepIsThreaded;
	}

	public void warpForward(long millis) {
		currentTime += millis;
		synchronized (lock) {
			lock.notifyAll();
		}
	}

	public void warpBackward(long millis) {
		currentTime -= millis;
	}
}
