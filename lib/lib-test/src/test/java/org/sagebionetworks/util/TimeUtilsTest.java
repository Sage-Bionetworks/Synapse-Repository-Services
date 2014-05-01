package org.sagebionetworks.util;

import static org.junit.Assert.*;
import org.sagebionetworks.util.TestClock;

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
		boolean result = TimeUtils.waitFor(6000, -1000, "a", new Predicate<String>() {
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
		boolean result = TimeUtils.waitFor(6000, -1000, "a", new Predicate<String>() {
			@Override
			public boolean apply(String input) {
				return count.incrementAndGet() > 6;
			}
		});
		assertFalse(result);
		assertEquals(6, count.get());
		assertEquals(start + 1000 * 7.4, Clock.currentTimeMillis(), 100);
	}
}
