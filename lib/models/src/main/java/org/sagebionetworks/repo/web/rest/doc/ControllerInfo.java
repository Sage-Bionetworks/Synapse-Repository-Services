package org.sagebionetworks.repo.web.rest.doc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation provides additional information used to generating the REST API documents for a Controller.
 * REST documentation will only be created for controller with this annotation.
 * 
 * @author John
 *
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ControllerInfo {

	/**
	 * The display name of the controller. When provide, this name will be used instead of the name of the 
	 * controller class in the REST documentation.
	 * 
	 * @return
	 */
	String displayName();
	
	/**
	 * The paths used by this controller for example:  repo/v1 or auth/v1 or file/v1.
	 * @return
	 */
	String path();
	
}
