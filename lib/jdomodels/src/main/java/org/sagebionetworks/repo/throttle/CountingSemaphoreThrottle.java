package org.sagebionetworks.repo.throttle;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.common.util.Clock;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * A throttle on all methods of the CountingSemaphore to ensure lock requests
 * happen at a frequency the database can handle. The longer each lock request
 * takes the longer the back-off.
 * 
 * See PLFM-5465.
 */
@Aspect
public class CountingSemaphoreThrottle {
	
	/**
	 * The sleep times is a function of the method elapse time
	 * times this multiplier.
	 */
	private static final long ELAPSE_MULTIPLIER = 2;

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
				clock.sleep(elapse * ELAPSE_MULTIPLIER);
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
