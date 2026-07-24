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
 * If a current transaction exists, Spring suspends it, and then the business logic is executed without a transaction.
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
	propagation = Propagation.NOT_SUPPORTED,
	rollbackFor = Throwable.class
)
public @interface TransactionNotSupported {

}
