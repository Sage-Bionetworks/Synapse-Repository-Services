package org.sagebionetworks.database.semaphore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction configured to use a bean named semaphoreTransactionManager as the transaction manager and 
 * a propagation of REQUIRES_NEW. This is to avoid silent mis-configurations when spring tries to automatically
 * infer a transaction manager and picks the wrong one.
 * 
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(
	transactionManager = "txManager",
	isolation = Isolation.READ_COMMITTED, 
	propagation = Propagation.REQUIRES_NEW,
	rollbackFor = Throwable.class
)
public @interface SemaphoreTransaction {

}
