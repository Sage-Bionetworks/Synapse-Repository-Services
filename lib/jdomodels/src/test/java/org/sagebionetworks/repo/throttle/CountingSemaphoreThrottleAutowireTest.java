package org.sagebionetworks.repo.throttle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class CountingSemaphoreThrottleAutowireTest {
	
	@Autowired
	CountingSemaphore countingSemaphore;
	
	@Autowired
	CountingSemaphoreThrottle throttle;
	
	String key;
	long timeoutSec;
	int maxLockCount;
	
	@Before
	public void before() {
		key = "CountingSemaphoreThrottleTest";
		timeoutSec = 100;
		maxLockCount = 1;
		countingSemaphore.releaseAllLocks();
	}
	
	@Test
	public void testThrottle() {
		long startCount = throttle.getCounter();
		// call under test
		String token = countingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertNotNull(token);
		countingSemaphore.refreshLockTimeout(key, token, timeoutSec);
		countingSemaphore.releaseLock(key, token);
		long elapseThrottleCount = throttle.getCounter()-startCount;
		assertEquals("Three calls should have trottled three times",3L, elapseThrottleCount);
	}
	
	@Test
	public void testThrottleFailedAttemptToAcquireLocks() {
		long startCount = throttle.getFailedLockAttemptCount();
		// call under test
		String token = countingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertNotNull(token);
		countingSemaphore.refreshLockTimeout(key, token, timeoutSec);
		countingSemaphore.releaseLock(key, token);
		long failedAcquireCount = throttle.getFailedLockAttemptCount()-startCount;
		assertEquals("None of the calls were failed acquire locks calls", 0, failedAcquireCount);
		// this should also return a lock
		token = countingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertNotNull(token);
		// this acquire should fail to get a lock
		token = countingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount);
		assertEquals(null, token);
		failedAcquireCount = throttle.getFailedLockAttemptCount()-startCount;
		assertEquals("There should be two failed acquire lock calls", 1, failedAcquireCount);
	}

}
