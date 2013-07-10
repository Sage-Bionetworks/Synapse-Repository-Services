package org.sagebionetworks.javadoc.velocity;

import org.apache.velocity.context.Context;

/**
 * Context for a given class.
 * 
 * @author John
 *
 */
public class ClassContext {

	String className;
	String templateName;
	Context context;
	
	public ClassContext(String className, String templateName, Context context) {
		super();
		this.className = className;
		this.templateName = templateName;
		this.context = context;
	}
	
	public String getClassName() {
		return className;
	}
	public String getTemplateName() {
		return templateName;
	}
	public Context getContext() {
		return context;
	}
	@Override
	public String toString() {
		return "ClassContext [className=" + className + ", templateName="
				+ templateName + ", context=" + context + "]";
	}
	
}
