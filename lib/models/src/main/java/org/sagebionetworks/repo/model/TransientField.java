package org.sagebionetworks.repo.model;

import java.lang.annotation.Retention;
import java.lang.annotation.*;
import java.lang.annotation.Target;


/**
 * Mark any field of an entity that should not be stored in the database as transient.
 * 
 * @author jmhill
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TransientField {
	
	

}
