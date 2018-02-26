package org.sagebionetworks.profiler;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is a Profiler that logs the results.
 * 
 * @author jmhill
 * 
 */
@Aspect
public class Profiler {
	@Autowired
	private ProfilerFrameStackManager frameStackManager;

	// execution(* org.sagebionetworks..*.*(..)) means profile any bean in the
	// package org.sagebionetworks or any sub-packages
	@Around("execution(* org.sagebionetworks..*.*(..)) && !within(org.sagebionetworks.profiler.*)")
	public Object doBasicProfiling(ProceedingJoinPoint pjp) throws Throwable {
		// Do nothing if logging is not on
		if (!frameStackManager.shouldCaptureData()) {
			// Just proceed if logging is off.
			return pjp.proceed();
		}

		Signature signature = pjp.getSignature();
		Class<?> declaring = pjp.getTarget().getClass();
		String methodName = declaring.getName() + "." + signature.getName();

		long startTime = System.nanoTime();
		try {
			frameStackManager.startProfiling(methodName);
			return pjp.proceed();
		} finally {
			long endTime = System.nanoTime();
			frameStackManager.endProfiling(methodName, (endTime - startTime) / 1000000);
		}
	}

}
