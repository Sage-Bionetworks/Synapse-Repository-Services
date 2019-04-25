package org.sagebionetworks.repo.throttle;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.common.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Will throttle the methods of CountingSemaphoreImpl by sleeping based on the
 * execution time of the call. So as calls slow down the sleep time increases.
 *
 */
@Aspect
public class CountingSemaphoreThrottle {

	@Autowired
	Clock clock;

	long throttleCounter = 0;
	long failedLockAttemptCount = 0;

	@Around("execution(* org.sagebionetworks.database.semaphore.CountingSemaphoreImpl.*(..))")
	public Object profile(ProceedingJoinPoint pjp) throws Throwable {
		Object result = null;
		long start = clock.currentTimeMillis();
		try {
			result = pjp.proceed();
			return result;
		} finally {
			throttleCounter++;
			long elapse = clock.currentTimeMillis() - start;
			long sleepTimeMs = elapse;
			if (result == null && pjp.getSignature().getName().equals("attemptToAcquireLock")) {
				/*
				 * For the case where a lock is not acquired we sleep longer as this is the main
				 * source of too many calls, and throttling here has a limited impact on
				 * performance.
				 */
				sleepTimeMs = elapse * 10;
				failedLockAttemptCount++;
			}
			if (sleepTimeMs > 0) {
				clock.sleep(sleepTimeMs);
			}
		}
	}

	/**
	 * Get the number of times this throttle has been applied.
	 * 
	 * @return
	 */
	public long getCounter() {
		return throttleCounter;
	}

	/**
	 * Get the number of times failed attemptToAcquireLock calls were throttled.
	 */
	public long getFailedLockAttemptCount() {
		return failedLockAttemptCount;
	}

}
