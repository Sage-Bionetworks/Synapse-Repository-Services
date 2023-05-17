package org.sagebionetworks.repo.throttle;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.sagebionetworks.database.semaphore.CountingSemaphore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:jdomodels-test-context.xml" })
public class CountingSemaphoreThrottleAutowireTest {
	
	@Autowired
	private CountingSemaphore countingSemaphore;
	
	@Autowired
	private CountingSemaphoreThrottle throttle;
	
	String key;
	long timeoutSec;
	int maxLockCount;
	
	@BeforeEach
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
		Optional<String> optional = countingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, CountingSemaphoreThrottleAutowireTest.class.getName());
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		optional.ifPresent((token)->{
			countingSemaphore.refreshLockTimeout(key, token, timeoutSec);
			countingSemaphore.releaseLock(key, token);
		});
		long elapseThrottleCount = throttle.getCounter()-startCount;
		assertEquals(3L, elapseThrottleCount);
	}
	
	@Test
	public void testThrottleFailedAttemptToAcquireLocks() {
		long startCount = throttle.getFailedLockAttemptCount();
		// call under test
		Optional<String> optional = countingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, CountingSemaphoreThrottleAutowireTest.class.getName());
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		optional.ifPresent((token)->{
			countingSemaphore.refreshLockTimeout(key, token, timeoutSec);
			countingSemaphore.releaseLock(key, token);
		});
		long failedAcquireCount = throttle.getFailedLockAttemptCount()-startCount;
		assertEquals(0, failedAcquireCount);
		// this should also return a lock
		optional = countingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, CountingSemaphoreThrottleAutowireTest.class.getName());
		assertNotNull(optional);
		assertTrue(optional.isPresent());
		// this acquire should fail to get a lock
		optional = countingSemaphore.attemptToAcquireLock(key, timeoutSec, maxLockCount, CountingSemaphoreThrottleAutowireTest.class.getName());
		assertNotNull(optional);
		assertTrue(optional.isEmpty());
		failedAcquireCount = throttle.getFailedLockAttemptCount()-startCount;
		assertEquals(1, failedAcquireCount);
	}

}
