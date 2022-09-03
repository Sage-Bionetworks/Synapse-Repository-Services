package org.sagebionetworks.repo.model.transactions;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Special annotation to enable a write transaction that is bound to a data source that is optimized for high throughput 
 */
@Documented
@Retention(RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface MigrationWriteTransaction {

}
