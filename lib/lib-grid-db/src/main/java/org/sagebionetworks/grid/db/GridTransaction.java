package org.sagebionetworks.grid.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction configured to use an explicit gridTransactionManager. The annotation allows to modify the readOnly attribute.
 * 
 * This annotation was introduced due to a bug in Spring when using multiple transaction managers and a mix of class and 
 * method level Transactional annotations. Theoretically when specifying the transactionManager attribute for the Transactional
 * annotation at the class level, any method level Transactional annotation should pick up the same transaction manager. 
 * However, we observed that spring would still give precedence to a "Primary" transaction manager configured in the application 
 * and in the grid case it wasn't the correct transaction manager.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Transactional(
	transactionManager = "gridTransactionManager",
	isolation = Isolation.READ_COMMITTED, 
	propagation = Propagation.REQUIRED,
	rollbackFor = Throwable.class
)
public @interface GridTransaction {
	
	/**
	 * Alias for {@link Transactional#readOnly}. Defaults to {@code false}.
	 */
	@AliasFor(annotation = Transactional.class)
	boolean readOnly() default false;
}
