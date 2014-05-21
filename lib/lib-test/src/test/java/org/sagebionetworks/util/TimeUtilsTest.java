package org.sagebionetworks.util;

import static org.junit.Assert.*;

import org.sagebionetworks.util.TestClock;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Test;

import com.google.common.base.Predicate;

public class TimeUtilsTest {
	// we need to test the Clock dependent code in utils here, to avoid a circular dependency between lib-utils
	// (Clock.java) and lib-test (TestClock.java)
	@After
	public void after() {
		TestClock.resetClockProvider();
	}

	@Test
	public void testSqlDateParse() {
		assertEquals(0, TimeUtils.parseSqlDate("1970-01-01 00:00:00.000"));
		assertEquals(0, TimeUtils.parseSqlDate("1970-1-1 0:0:0.0"));
		assertEquals(0, TimeUtils.parseSqlDate("1970-1-1 00:00:00"));
		assertEquals(0, TimeUtils.parseSqlDate("1970-1-1"));
		assertEquals(123, TimeUtils.parseSqlDate("1970-1-1 00:00:00.123"));

		assertEquals(65844122200L, TimeUtils.parseSqlDate("1972-02-02 02:02:02.200"));
		assertEquals(65844122200L, TimeUtils.parseSqlDate("1972-2-2 2:2:2.2"));

		assertEquals(1400084625398L, TimeUtils.parseSqlDate("2014-5-14 16:23:45.398"));
	}

	@Test
	public void testNormal() {
		TestClock.useTestClockProvider();
		long start = Clock.currentTimeMillis();
		final AtomicInteger count = new AtomicInteger(0);
		boolean result = TimeUtils.waitFor(6000, 1000, "a", new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				return count.incrementAndGet() > 6;
			}
		});
		assertTrue(result);
		assertEquals(start + 6000, Clock.currentTimeMillis());
	}

	@Test
	public void testExponential() {
		TestClock.useTestClockProvider();
		long start = Clock.currentTimeMillis();
		final AtomicInteger count = new AtomicInteger(0);
		boolean result = TimeUtils.waitForExponential(6000, 1000, "a", new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				return count.incrementAndGet() > 5;
			}
		});
		assertTrue(result);
		assertEquals(start + 1000 * 7.4, Clock.currentTimeMillis(), 100);
	}

	@Test
	public void testFail() {
		TestClock.useTestClockProvider();
		long start = Clock.currentTimeMillis();
		final AtomicInteger count = new AtomicInteger(0);
		boolean result = TimeUtils.waitFor(6000, 1000, "a", new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				return count.incrementAndGet() > 7;
			}
		});
		assertFalse(result);
		assertEquals(7, count.get());
		assertEquals(start + 6000, Clock.currentTimeMillis());
	}

	@Test
	public void testExponentialFail() {
		TestClock.useTestClockProvider();
		long start = Clock.currentTimeMillis();
		final AtomicInteger count = new AtomicInteger(0);
		boolean result = TimeUtils.waitForExponential(6000, 1000, "a", new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				return count.incrementAndGet() > 6;
			}
		});
		assertFalse(result);
		assertEquals(6, count.get());
		assertEquals(start + 1000 * 7.4, Clock.currentTimeMillis(), 100);
	}
	
	@Test
	public void testExponentialMaxRetry() throws Exception{
		final int maxRetry = 3;
		final AtomicInteger count = new AtomicInteger(0);
		Boolean result = TimeUtils.waitForExponentialMaxRetry(maxRetry, 100, new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				if (count.incrementAndGet() < maxRetry)
					throw new RetryException("Failed, should retry");
				else return true;
			}
		});

		assertTrue(result);
		assertEquals(maxRetry, count.get());
	}
	
	@Test
	public void testExponentialMaxRetryFail() {
		final int maxRetry = 3;
		final AtomicInteger count = new AtomicInteger(0);
		try {
			Boolean result = TimeUtils.waitForExponentialMaxRetry(maxRetry, 100, new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					if (count.incrementAndGet() <= maxRetry)
						throw new RetryException("Failed, retry");
					else return true;
				}
			});
			fail("expected exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("User rate limit has been exceeded"));
		}
		
		//should have called apply maxRetry times, no more.
		assertEquals(maxRetry, count.get());
	}
}
