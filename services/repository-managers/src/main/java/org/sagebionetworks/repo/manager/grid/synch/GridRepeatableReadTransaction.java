package org.sagebionetworks.repo.manager.grid.synch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A read-only grid transaction at {@link Isolation#REPEATABLE_READ} on the
 * {@code gridTransactionManager}.
 *
 * <p>
 * Inner per-page read queries (annotated {@code @GridTransaction(readOnly=true)}
 * with {@link Propagation#REQUIRED}) join this outer transaction and therefore
 * inherit its REPEATABLE_READ isolation.
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Transactional(
	transactionManager = "gridTransactionManager",
	isolation = Isolation.REPEATABLE_READ,
	propagation = Propagation.REQUIRED,
	readOnly = true,
	rollbackFor = Throwable.class
)
public @interface GridRepeatableReadTransaction {
}
