package org.sagebionetworks.repo.model.dbo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the class is a DBO table
 * 
 * @author marcel
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {
	/**
	 * the name of the table
	 */
	public String name();

	/**
	 * Additional constraints on the table, not captured in the field definition
	 */
	public String[] constraints() default {};
}
