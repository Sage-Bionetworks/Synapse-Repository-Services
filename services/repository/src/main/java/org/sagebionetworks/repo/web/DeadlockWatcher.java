package org.sagebionetworks.repo.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.dao.TransientDataAccessException;

/**
 * This aspect is used to detect Deadlock and make another attempt.
 * 
 * This as how we are addressing PLFM-1341
 * 
 * @author jmhill
 *
 */
@Aspect
public class DeadlockWatcher {
	
	public static final int MAX_NUMBER_TRIES = 4;
	public static final long WAIT_TIME_MS = 10;
	static private Log log = LogFactory.getLog(DeadlockWatcher.class);
	
	public static final String START_MESSAGE = "Deadlock detected: %1$s";
	public static final String FAILED_ATTEMPT = "Failed attempt: %1$d after waiting for: %2$d ms";
	public static final String EXAUSTED_ALL_ATTEMPTS = "Exausted all attempts to recover from deadlock.  Total attempts: %1$d";
	
	/**
	 * this is used by tests to inject a mock log.
	 */
	void setLog(Log mockLock){
		log = mockLock;
	}
	
	@Around("execution(* org.sagebionetworks.repo.web.service.EntityService.*(..))")
	public Object detectDeadlock(ProceedingJoinPoint pjp) throws Throwable {
		try{
			// Let all methods proceed as expected.
			return pjp.proceed();
		} catch (TransientDataAccessException e) {
			log.debug(String.format(START_MESSAGE, e.getMessage()));
			// When deadlock occurs, we wait and try one more time
			int attempt = 2;
			long wait = WAIT_TIME_MS;
			while(attempt <= MAX_NUMBER_TRIES){
				// Wait
				Thread.sleep(wait);
				try{
					return pjp.proceed();
				} catch (TransientDataAccessException e2) {
					// We might get another shot.
				}
				log.debug(String.format(FAILED_ATTEMPT, attempt, wait));
				// make an exponential wait
				wait *= wait;
				attempt++;
			}
			// throw the error
			log.debug(String.format(EXAUSTED_ALL_ATTEMPTS, attempt-1));
			throw e;
		}
	}

}
