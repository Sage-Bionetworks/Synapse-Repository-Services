package org.sagebionetworks.repo.transactions;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used on a method, a transaction must already exists, for this method to
 * join. It is invalid call a mandatory annotated method outside of a
 * transaction.
 * </p>
 * Transaction-isolation-level = READ_COMMITED.
 *
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MandatoryWriteTransaction {

}
