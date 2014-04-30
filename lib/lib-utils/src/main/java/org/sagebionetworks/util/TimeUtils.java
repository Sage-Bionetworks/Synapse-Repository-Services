package org.sagebionetworks.util;

import com.google.common.base.Predicate;

public class TimeUtils {
	/**
	 * Wait for at most maxTimeMillis for condition to return true. Recheck every checkIntervalMillis or use exponential
	 * back of
	 * 
	 * @param maxTimeMillis
	 * @param checkIntervalMillis if positive, check that interval, if negative, start at that interval and back of by
	 *        1.2x
	 * @param condition
	 * @param input
	 * @return false if timed out
	 */
	public static <T> boolean waitFor(long maxTimeMillis, long checkIntervalMillis, T input, Predicate<T> condition) {
		boolean isExponential = checkIntervalMillis < 0;
		if (isExponential) {
			checkIntervalMillis = -checkIntervalMillis;
		}
		long startTimeMillis = Clock.currentTimeMillis();
		while (!condition.apply(input)) {
			long nowMillis = Clock.currentTimeMillis();
			if (nowMillis - startTimeMillis >= maxTimeMillis) {
				return false;
			}
			Clock.sleepNoInterrupt(checkIntervalMillis);
			if (isExponential) {
				checkIntervalMillis *= 1.2;
			}
		}
		return true;
	}
}
