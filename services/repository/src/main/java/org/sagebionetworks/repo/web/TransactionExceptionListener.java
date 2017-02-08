package org.sagebionetworks.repo.web;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.repo.model.exception.ExceptionThreadLocal;

/**
 * This aspect listens to exception being throw during a transaction and captures them.
 */
@Aspect
public class TransactionExceptionListener {

	@AfterThrowing(
			pointcut = "execution(* org.sagebionetworks..*.*(..))",
			throwing = "throwable")
	public void captureThrowable(Throwable throwable) {
		ExceptionThreadLocal.push(throwable);
	}
}
