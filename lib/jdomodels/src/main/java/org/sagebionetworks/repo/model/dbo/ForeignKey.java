package org.sagebionetworks.repo.model.dbo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates this field is a foreign key
 * 
 * @author marcel
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ForeignKey {

	/**
	 * The name of the foreign key. Optional, if not specified, a readable name will be generated based on the table and
	 * key.
	 */
	String name() default "";

	/**
	 * The name of the foreign table
	 */
	String table();

	/**
	 * the name of the field in the foreign table
	 */
	String field();

	/**
	 * is this a cascade delete foreign key (default=false)
	 */
	boolean cascadeDelete() default false;

}
