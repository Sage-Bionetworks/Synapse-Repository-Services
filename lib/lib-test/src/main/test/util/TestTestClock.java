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

	@Test
	public void testFrequentCallback() {
		final TestClock clock = new TestClock();

		long start = System.currentTimeMillis();

		long now = clock.currentTimeMillis();

		final AtomicInteger count = new AtomicInteger(0);
		ProgressCallback<Long> progressCallback = new ProgressCallback<Long>() {
			@Override
			public void progressMade(Long message) {
				count.incrementAndGet();
			}
		};

		clock.sleepWithFrequentCallback(0, 30, progressCallback);
		assertEquals(now, clock.currentTimeMillis());
		assertEquals(0, count.get());

		now = clock.currentTimeMillis();
		clock.sleepWithFrequentCallback(29, 30, progressCallback);
		assertEquals(now + 29, clock.currentTimeMillis());
		assertEquals(1, count.get());

		now = clock.currentTimeMillis();
		count.set(0);
		clock.sleepWithFrequentCallback(30, 30, progressCallback);
		assertEquals(now + 30, clock.currentTimeMillis());
		assertEquals(1, count.get());

		now = clock.currentTimeMillis();
		count.set(0);
		clock.sleepWithFrequentCallback(31, 30, progressCallback);
		assertEquals(now + 31, clock.currentTimeMillis());
		assertEquals(2, count.get());

		now = clock.currentTimeMillis();
		count.set(0);
		clock.sleepWithFrequentCallback(75, 30, progressCallback);
		assertEquals(now + 75, clock.currentTimeMillis());
		assertEquals(3, count.get());
	}
}
