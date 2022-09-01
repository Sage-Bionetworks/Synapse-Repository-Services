package org.sagebionetworks.repo.model.datasource;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation that allows to change the {@link DataSourceType} at runtime to be used in a transactional context. 
 * Can be used to annotated a bean so that all the public methods will set the provided {@link DataSourceType}. 
 * Note that this will bound the type to the current thread and after each method execution the type will be cleared, 
 * it is therefore advisable to set this only on the calling manager or service without the need to propagate it to the dao layer.
 * Adding the annotation with a different type to the invoked type will retain the parent type.
 */
@Documented
@Inherited
@Retention(RUNTIME)
@Target({TYPE})
public @interface DataSourceContext {
	DataSourceType value() default DataSourceType.REPO;
}
