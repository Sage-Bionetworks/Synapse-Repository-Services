package org.sagebionetworks.util;

import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import junit.framework.Assert;

public class ThreadStepper {

	private static boolean printProgress = false;

	private final List<Thread> runners = Lists.newArrayList();

	private final long waitTimeInSeconds;

	private int threadCount;
	private CountDownLatch start;
	private CountDownLatch end;

	private Map<String, Pair<CountDownLatch, CountDownLatch>> waiters = Maps.newHashMap();

	public ThreadStepper() {
		this.waitTimeInSeconds = 5L;
	}

	public ThreadStepper(long waitTimeInSeconds) {
		this.waitTimeInSeconds = waitTimeInSeconds;
	}

	public void add(final Callable<?> callable) {
		runners.add(new Thread() {
			@Override
			public void run() {
				ThreadStepper.this.start();
				try {
					callable.call();
				} catch (Throwable t) {
					t.printStackTrace();
					Assert.fail("Got unexpected exception:" + t);
				} finally {
					ThreadStepper.this.end();
				}
			}
		});
	}

	public void run() {
		threadCount = runners.size();
		start = new CountDownLatch(threadCount);
		end = new CountDownLatch(threadCount);

		ThreadTestUtils.doBefore();
		try {
			for (Thread t : runners) {
				t.start();
			}
			for (Thread t : runners) {
				t.join(30000);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			Assert.fail("Got interrupted :" + e);
		} finally {
			ThreadTestUtils.doAfter();
		}
	}

	public void waitForStepDone(String step) {
		if (printProgress) {
			System.out.println("wait for  " + step);
		}
		try {
			Pair<CountDownLatch, CountDownLatch> stepWaiter = getStepWaiter(step);
			stepWaiter.getFirst().await(waitTimeInSeconds, TimeUnit.SECONDS);
			stepWaiter.getSecond().countDown();
		} catch (InterruptedException e) {
			fail("Unexpected exception " + e);
		}
	}

	public void stepDone(String step) {
		if (printProgress) {
			System.out.println("done with " + step);
		}
		try {
			Pair<CountDownLatch, CountDownLatch> stepWaiter = getStepWaiter(step);
			stepWaiter.getFirst().countDown();
			stepWaiter.getSecond().await(waitTimeInSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail("Unexpected exception " + e);
		}
	}

	private synchronized Pair<CountDownLatch, CountDownLatch> getStepWaiter(String step) {
		Pair<CountDownLatch, CountDownLatch> latches = waiters.get(step);
		if (latches == null) {
			latches = Pair.create(new CountDownLatch(1), new CountDownLatch(threadCount - 1));
			waiters.put(step, latches);
		}
		return latches;
	}

	private void start() {
		if (printProgress) {
			System.out.println("start");
		}
		start.countDown();
		try {
			start.await(waitTimeInSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail("Unexpected exception " + e);
		}
	}

	private void end() {
		if (printProgress) {
			System.out.println("end");
		}
		end.countDown();
		try {
			end.await(waitTimeInSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail("Unexpected exception " + e);
		}
	}
}
