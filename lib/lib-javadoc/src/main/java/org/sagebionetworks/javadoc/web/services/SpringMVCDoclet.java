package org.sagebionetworks.javadoc.web.services;

import com.sun.javadoc.*;
import com.sun.tools.doclets.standard.Standard;
/**
 * Java Doclet for generating javadocs for Spring MVC web-services.
 * 
 * @author jmhill
 *
 */
public class SpringMVCDoclet {

	public static boolean start(RootDoc root) {
		// Pass this along to the standard doclet
		Standard.start(root);
		return true;
	}
}
