package org.sagebionetworks.repo.web;

import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.sagebionetworks.util.ThreadLocalProvider;

/**
 * This aspect listens to exception being throw during a transaction and captures them.
 */
@Aspect
public class TransactionExceptionListener {

	public static final String EXCEPTION = "EXCEPTION";
	private static final ThreadLocal<Throwable> exceptionThreadLocal = ThreadLocalProvider.getInstance(EXCEPTION, Throwable.class);

	@AfterThrowing(
			pointcut = "@annotation(org.sagebionetworks.repo.transactions.MandatoryWriteTransaction)",
			throwing = "throwable")
	public void captureThrowable(Throwable throwable) {
		exceptionThreadLocal.set(throwable);
	}
}
