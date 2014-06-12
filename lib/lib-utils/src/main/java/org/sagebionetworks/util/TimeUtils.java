package org.sagebionetworks.util;

import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import com.google.common.base.Predicate;
import com.google.common.util.concurrent.UncheckedExecutionException;

public class TimeUtils {
	private static final DateTimeFormatter dateParser;

	static {
		// DateTimeFormat.forPattern("yy-M-d H:m:s.SSS");
		DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder().appendPattern("yy-M-d").toFormatter();
		DateTimeFormatter microSecondsFormatter = new DateTimeFormatterBuilder().appendLiteral('.').appendPattern("SSS").toFormatter();
		DateTimeFormatter secondsFormatter = new DateTimeFormatterBuilder().appendPattern(":s")
				.appendOptional(microSecondsFormatter.getParser()).toFormatter();
		DateTimeFormatter timeFormatter = new DateTimeFormatterBuilder().appendPattern(" H:m").appendOptional(secondsFormatter.getParser())
				.toFormatter();
		dateParser = new DateTimeFormatterBuilder().append(dateFormatter).appendOptional(timeFormatter.getParser()).toFormatter()
				.withZoneUTC();
	}

	/**
	 * Parse a string as a sql date with pattern yy-M-D[ H:m[:s[.SSS]]]
	 * 
	 * @param value
	 * @return
	 */
	public static long parseSqlDate(String value) {
		DateTime parsedDateTime = dateParser.parseDateTime(value);
		return parsedDateTime.getMillis();
	}

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

	/**
	 * Wait for at most maxRetryCount for condition to return true. Recheck every checkIntervalMillis with exponential
	 * back off Throws retry exception if the max number of retries is exceeded. That RetryException will have the same
	 * cause as the retry exception thrown from the callable
	 * 
	 * @param maxRetryCount
	 * @param initialCheckIntervalMillis check at this interval and back of by 1.2x
	 * @param condition
	 * @param input
	 * @return false if condition returned false
	 */
	public static <T> T waitForExponentialMaxRetry(int maxRetryCount, long initialCheckIntervalMillis, Callable<T> callable) throws Exception {
		return waitForInternalMaxRetry(maxRetryCount, initialCheckIntervalMillis, callable, true);
	}

	private static <T> T waitForInternalMaxRetry(int maxRetryCount, long initialCheckIntervalMillis, Callable<T> callable,
			boolean exponential) throws Exception{
		int count = 0;
		while (true) {
			try {
				return callable.call();	
			} catch (RetryException re) {
				if (++count >= maxRetryCount) {
					throw new RetryException("Exceeded maximum retries", re.getCause());
				}
				Clock.sleepNoInterrupt(initialCheckIntervalMillis);
				if (exponential) {
					initialCheckIntervalMillis *= 1.2;
				}
			} 
		}
	}
}
