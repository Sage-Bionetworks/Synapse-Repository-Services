package org.sagebionetworks.repo.transactions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If a current transaction exists, first Spring suspends it, and then the business logic is executed without a transaction.
 * </p>
 * Transaction-isolation-level = READ_COMMITED.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface TransactionNotSupported {

}
