package org.sagebionetworks.repo.transactions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * When used on a method, a new transaction will be started for the method.
 * If there is an existing transaction on the same thread, the existing
 * transaction will be 'paused' while the new transaction executes.
 * <p>
 * Transaction-isolation-level = READ_COMMITTED. Warning: When
 * NewWriteTransaction is used from within an existing transaction, any attempt
 * to update rows locked by the previous transaction will deadlock the new
 * transaction.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional(
	transactionManager = "txManager",
	isolation = Isolation.READ_COMMITTED,
	propagation = Propagation.REQUIRES_NEW,
	rollbackFor = Throwable.class
)
public @interface NewWriteTransaction {

}
