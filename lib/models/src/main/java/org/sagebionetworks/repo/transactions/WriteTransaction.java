package org.sagebionetworks.repo.transactions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on a method that can join an existing transaction or start a new transaction.
 * Such a method will always run within a transaction.
 * </p>
 * Transaction-isolation-level = READ_COMMITED.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface WriteTransaction {

}
