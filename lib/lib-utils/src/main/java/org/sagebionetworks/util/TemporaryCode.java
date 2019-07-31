package org.sagebionetworks.util;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

/**
 * Marker annotation for code that is added temporarily to the code base and should be soon removed (e.g. temporary migration code)
 */
@Retention(RUNTIME)
@Documented
public @interface TemporaryCode {
	
	String author();
	
	String comment() default "";

}
