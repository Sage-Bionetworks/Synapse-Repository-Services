package org.sagebionetworks.repo.model.dbo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Field {

	String name();

	/**
	 * Is this field an etag?
	 * 
	 * @return
	 */
	boolean etag() default false;

	/**
	 * Is this field a backup id?
	 * 
	 * @return
	 */
	boolean backupId() default false;

	/**
	 * Is this field a primary key?
	 * 
	 * @return
	 */
	boolean primary() default false;

	/**
	 * override of default type conversion
	 * 
	 * @return
	 */
	String type() default "";

	/**
	 * Is nullable?
	 * @return
	 */
	boolean nullable() default true;

	/**
	 * Is default null
	 * @return
	 */
	boolean defaultNull() default false;

	/**
	 * Is varchar(...)
	 * 
	 * @return
	 */
	int varchar() default 0;

	/**
	 * Is char(...)
	 * 
	 * @return
	 */
	int fixedchar() default 0;

	// always append
	String sql() default "";
}
