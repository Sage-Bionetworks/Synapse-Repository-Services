package org.sagebionetworks.repo.model.jdo;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * This aspect allows us to check if the connection pool is being used as expected.
 * @author jmhill
 *
 */
@Aspect
public class ConnectionPoolLogger {
	
	private static final Logger log = Logger.getLogger(ConnectionPoolLogger.class
			.getName());

	/**
	 * Watch everything from the org.apache.commons.dbcp.BasicDataSource which we
	 * are using as a connection pool.
	 * @param pjp
	 * @return
	 * @throws Throwable
	 */
//	@Around("execution(* org.apache.commons.dbcp..*.*(..))")
	public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {
		if(log.isDebugEnabled()){
//			log.debug(pjp.getSignature());
		}
		return pjp.proceed();
	}
}
