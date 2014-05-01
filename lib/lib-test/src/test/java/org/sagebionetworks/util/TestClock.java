package org.sagebionetworks.util;

import org.sagebionetworks.util.Clock.ClockProvider;

public class TestClock {

	private static class TestClockProvider implements ClockProvider {
		long currentTime = 100000L;
		boolean sleepIsThreaded = false;

		@Override
		public long currentTimeMillis() {
			return currentTime;
		}

		@Override
		public void sleep(long millis) throws InterruptedException {
			if (sleepIsThreaded) {
				long startTime = currentTime;
				synchronized (this) {
					while (currentTime < startTime + millis) {
						this.wait(60000);
					}
				}
			} else {
				currentTime += millis;
			}
		}
	};

	private static TestClockProvider testClockProvider = null;

	public static void useTestClockProvider() {
		testClockProvider = new TestClockProvider();
		Clock.setProvider(testClockProvider);
	}

	public static void resetClockProvider() {
		Clock.setSystemProvider();
	}

	public static void setThreadedSleep(boolean sleepIsThreaded) {
		testClockProvider.sleepIsThreaded = sleepIsThreaded;
	}

	public static void warpForward(long millis) {
		testClockProvider.currentTime += millis;
		synchronized (testClockProvider) {
			testClockProvider.notifyAll();
		}
	}

	public static void warpBackward(long millis) {
		testClockProvider.currentTime -= millis;
	}
}
