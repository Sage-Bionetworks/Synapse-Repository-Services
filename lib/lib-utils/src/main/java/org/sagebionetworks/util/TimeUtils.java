package org.sagebionetworks.util;

import com.google.common.base.Predicate;

public class TimeUtils {
	/**
	 * Wait for at most maxTimeMillis for condition to return true. Recheck every checkIntervalMillis
	 * 
	 * @param maxTimeMillis
	 * @param checkIntervalMillis interval check time
	 * @param condition
	 * @param input
	 * @return false if timed out
	 */
	public static <T> boolean waitFor(long maxTimeMillis, long checkIntervalMillis, T input, Predicate<T> condition) {
		return waitForInternal(maxTimeMillis, checkIntervalMillis, input, condition, false);
	}

	/**
	 * Wait for at most maxTimeMillis for condition to return true. Recheck every checkIntervalMillis with exponential
	 * back off
	 * 
	 * @param maxTimeMillis
	 * @param initialCheckIntervalMillis check at this interval and back of by 1.2x
	 * @param condition
	 * @param input
	 * @return false if timed out
	 */
	public static <T> boolean waitForExponential(long maxTimeMillis, long initialCheckIntervalMillis, T input, Predicate<T> condition) {
		return waitForInternal(maxTimeMillis, initialCheckIntervalMillis, input, condition, true);
	}

	private static <T> boolean waitForInternal(long maxTimeMillis, long initialCheckIntervalMillis, T input, Predicate<T> condition,
			boolean exponential) {
		long startTimeMillis = Clock.currentTimeMillis();
		while (!condition.apply(input)) {
			long nowMillis = Clock.currentTimeMillis();
			if (nowMillis - startTimeMillis >= maxTimeMillis) {
				return false;
			}
			Clock.sleepNoInterrupt(initialCheckIntervalMillis);
			if (exponential) {
				initialCheckIntervalMillis *= 1.2;
			}
		}
		return true;
	}
}
