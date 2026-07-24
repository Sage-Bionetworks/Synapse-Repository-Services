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
 * Used on a method that can join an existing transaction or start a new transaction.
 * Such a method will always run within a transaction.
 * <p>
 * Transaction-isolation-level = READ_COMMITTED.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Transactional(
	transactionManager = "txManager",
	isolation = Isolation.READ_COMMITTED,
	propagation = Propagation.REQUIRED,
	rollbackFor = Throwable.class
)
public @interface WriteTransaction {

}
