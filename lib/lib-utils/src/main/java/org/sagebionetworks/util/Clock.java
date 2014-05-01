package org.sagebionetworks.util;

public class Clock {
	public interface ClockProvider {
		public long currentTimeMillis();

		public void sleep(long millis) throws InterruptedException;
	}

	private static ClockProvider systemProvider = new ClockProvider() {
		public long currentTimeMillis() {
			return System.currentTimeMillis();
		}

		public void sleep(long millis) throws InterruptedException {
			Thread.sleep(millis);
		}
	};

	private static ClockProvider provider = systemProvider;

	public static long currentTimeMillis() {
		return provider.currentTimeMillis();
	}

	public static void sleep(long millis) throws InterruptedException {
		provider.sleep(millis);
	}

	public static boolean sleepNoInterrupt(long millis) {
		try {
			provider.sleep(millis);
			return true;
		} catch (InterruptedException e) {
			return false;
		}

	}

	public static ClockProvider getClockProvider() {
		return provider;
	}

	public static void setProvider(ClockProvider provider) {
		Clock.provider = provider;
	}

	public static void setSystemProvider() {
		Clock.provider = systemProvider;
	}
}
