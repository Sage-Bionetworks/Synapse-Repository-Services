package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ReflectionUtils;

import com.google.common.base.Predicate;

public class TimeUtilsTest {

	TestClock clock = new TestClock();

	@Before
	public void before() throws Exception {
		ReflectionStaticTestUtils.setStaticField(TimeUtils.class, "clock", clock);
	}

	@After
	public void after() throws Exception {
		ReflectionStaticTestUtils.setStaticField(TimeUtils.class, "clock", new DefaultClock());
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
		long start = clock.currentTimeMillis();
		final AtomicInteger count = new AtomicInteger(0);
		boolean result = TimeUtils.waitFor(6000, 1000, "a", new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				return count.incrementAndGet() > 6;
			}
		});
		assertTrue(result);
		assertEquals(start + 6000, clock.currentTimeMillis());
	}

	@Test
	public void testExponential() {
		long start = clock.currentTimeMillis();
		final AtomicInteger count = new AtomicInteger(0);
		boolean result = TimeUtils.waitForExponential(6000, 1000, "a", new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				return count.incrementAndGet() > 5;
			}
		});
		assertTrue(result);
		assertEquals(start + 1000 * 7.4, clock.currentTimeMillis(), 100);
	}

	@Test
	public void testFail() {
		long start = clock.currentTimeMillis();
		final AtomicInteger count = new AtomicInteger(0);
		boolean result = TimeUtils.waitFor(6000, 1000, "a", new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				return count.incrementAndGet() > 7;
			}
		});
		assertFalse(result);
		assertEquals(7, count.get());
		assertEquals(start + 6000, clock.currentTimeMillis());
	}

	@Test
	public void testExponentialFail() {
		long start = clock.currentTimeMillis();
		final AtomicInteger count = new AtomicInteger(0);
		boolean result = TimeUtils.waitForExponential(6000, 1000, "a", new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				return count.incrementAndGet() > 6;
			}
		});
		assertFalse(result);
		assertEquals(6, count.get());
		assertEquals(start + 1000 * 7.4, clock.currentTimeMillis(), 100);
	}
	
	@Test
	public void testExponentialMaxRetry() throws Exception{
		long start = clock.currentTimeMillis();
		final int maxRetry = 3;
		final AtomicInteger count = new AtomicInteger(0);
		Boolean result = TimeUtils.waitForExponentialMaxRetry(maxRetry, 1000, new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				if (count.incrementAndGet() < maxRetry)
					throw new RetryException("Failed, should retry");
				else return true;
			}
		});

		assertTrue(result);
		assertEquals(maxRetry, count.get());
		assertEquals(start + 1000 * 2.2, clock.currentTimeMillis(), 100);
	}
	
	@Test
	public void testExponentialMaxRetryFail() {
		long start = clock.currentTimeMillis();
		final int maxRetry = 3;
		final AtomicInteger count = new AtomicInteger(0);
		try {
			TimeUtils.waitForExponentialMaxRetry(maxRetry, 1000, new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					if (count.incrementAndGet() <= maxRetry)
						throw new RetryException("Failed, retry");
					else return true;
				}
			});
			fail("expected exception");
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("Exceeded maximum retries"));
		}
		
		//should have called apply maxRetry times, no more.
		assertEquals(maxRetry, count.get());
		assertEquals(start + 1000 * 2.2, clock.currentTimeMillis(), 100);
	}

	@Test
	public void testTimedAssert() {
		final AtomicInteger count = new AtomicInteger(3);
		TimedAssert.waitForAssert(10000, 10, new Runnable() {
			@Override
			public void run() {
				if (count.incrementAndGet() < 3) {
					fail();
				}
			}
		});
		assertEquals(4, count.get());
	}

	@Test
	public void testTimedAssertFail() {
		final AtomicInteger count = new AtomicInteger(3);
		boolean gotError;
		try {
			TimedAssert.waitForAssert(50, 1, new Runnable() {
				@Override
				public void run() {
					count.incrementAndGet();
					fail();
				}
			});
			gotError = false;
		} catch (AssertionError e) {
			// expected
			gotError = true;
		}
		assertTrue(gotError);
		assertTrue("should have been checked at least twice", count.get() > 2);
	}
}
