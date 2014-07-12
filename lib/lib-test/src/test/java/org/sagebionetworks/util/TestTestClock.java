package org.sagebionetworks.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class TestTestClock {

	@Test
	public void testNonThreaded() throws InterruptedException {
		Clock clock = new TestClock();

		long start = System.currentTimeMillis();

		long now = clock.currentTimeMillis();
		clock.sleep(60000);
		assertEquals(now + 60000, clock.currentTimeMillis());

		// make sure we didn't actually wait that long
		assertTrue(System.currentTimeMillis() - start < 5000);
	}

	@Test
	public void testThreaded() throws InterruptedException {
		final TestClock clock = new TestClock();

		long start = System.currentTimeMillis();

		clock.setThreadedSleep(true);
		long now = clock.currentTimeMillis();
		final CountDownLatch wait1 = new CountDownLatch(1);
		final CountDownLatch wait2 = new CountDownLatch(1);
		final AtomicInteger done = new AtomicInteger(0);
		Thread t = new Thread() {
			@Override
			public void run() {
				assertTrue(done.compareAndSet(0, 1));
				wait1.countDown();
				clock.sleepNoInterrupt(60000);
				assertTrue(done.compareAndSet(3, 4));
				wait2.countDown();
			}
		};
		t.start();
		assertTrue(wait1.await(60, TimeUnit.SECONDS));
		clock.waitForSleepers(1);
		assertTrue(done.compareAndSet(1, 2));
		clock.warpForward(30000);
		// make sure the thread stays sleeping
		Thread.sleep(500);
		assertTrue(done.compareAndSet(2, 3));
		// now wake it up
		clock.warpForward(30000);
		assertTrue(wait2.await(60, TimeUnit.SECONDS));
		assertTrue(done.compareAndSet(4, 5));
		t.join(60000);
		assertEquals(now + 60000, clock.currentTimeMillis());

		// make sure we didn't actually wait that long
		assertTrue(System.currentTimeMillis() - start < 5000);
	}
}
