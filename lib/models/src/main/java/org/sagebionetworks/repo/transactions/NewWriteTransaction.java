package org.sagebionetworks.repo.transactions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used on a method, a new transaction will be started to for the method.
 * If there is an existing transaction on the same thread, the existing transaction will be 
 * 'paused' while the new transaction executes.
 * </p>
 * Transaction-isolation-level = READ_COMMITED.
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface NewWriteTransaction {

}
