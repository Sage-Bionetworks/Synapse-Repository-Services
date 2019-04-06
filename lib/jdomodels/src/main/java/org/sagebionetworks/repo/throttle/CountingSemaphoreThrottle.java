package org.sagebionetworks.repo.throttle;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.common.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A throttle all methods of the CountingSemaphore to ensure we do not spend
 * more than 10% of the time on semaphores.
 *
 */
@Aspect
public class CountingSemaphoreThrottle {

	@Autowired
	Clock clock;

	long throttleCounter = 0;

	@Around("execution(* org.sagebionetworks.database.semaphore.CountingSemaphoreImpl.*(..))")
	public Object profile(ProceedingJoinPoint pjp) throws Throwable {
		long start = clock.currentTimeMillis();
		try {
			return pjp.proceed();
		} finally {
			throttleCounter++;
			long elapse = clock.currentTimeMillis() - start;
			if(elapse > 0) {
				clock.sleep(elapse * 10);
			}
		}
	}

	/**
	 * Get the number of times this throttle has sleept.
	 * 
	 * @return
	 */
	public long getCounter() {
		return throttleCounter;
	}

}
