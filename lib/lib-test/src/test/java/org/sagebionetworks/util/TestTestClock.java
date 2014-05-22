package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TestTestClock {
	@Before
	public void before() {
		TestClock.useTestClockProvider();
	}

	@After
	public void after() {
		TestClock.resetClockProvider();
	}

	@Test
	public void testNonThreaded() throws InterruptedException {
		long start = System.currentTimeMillis();

		TestClock.useTestClockProvider();
		long now = Clock.currentTimeMillis();
		Clock.sleep(60000);
		assertEquals(now + 60000, Clock.currentTimeMillis());

		// make sure we didn't actually wait that long
		assertTrue(System.currentTimeMillis() - start < 5000);
	}

	@Test
	public void testThreaded() throws InterruptedException {
		long start = System.currentTimeMillis();

		TestClock.useTestClockProvider();
		TestClock.setThreadedSleep(true);
		long now = Clock.currentTimeMillis();
		final CountDownLatch wait1 = new CountDownLatch(1);
		final CountDownLatch wait2 = new CountDownLatch(1);
		final AtomicInteger done = new AtomicInteger(0);
		Thread t = new Thread() {
			@Override
			public void run() {
				assertTrue(done.compareAndSet(0, 1));
				wait1.countDown();
				Clock.sleepNoInterrupt(60000);
				assertTrue(done.compareAndSet(3, 4));
				wait2.countDown();
			}
		};
		t.start();
		assertTrue(wait1.await(60, TimeUnit.SECONDS));
		TestClock.waitForSleepers(1);
		assertTrue(done.compareAndSet(1, 2));
		TestClock.warpForward(30000);
		// make sure the thread stays sleeping
		Thread.sleep(500);
		assertTrue(done.compareAndSet(2, 3));
		// now wake it up
		TestClock.warpForward(30000);
		assertTrue(wait2.await(60, TimeUnit.SECONDS));
		assertTrue(done.compareAndSet(4, 5));
		t.join(60000);
		assertEquals(now + 60000, Clock.currentTimeMillis());

		// make sure we didn't actually wait that long
		assertTrue(System.currentTimeMillis() - start < 5000);
	}
}
