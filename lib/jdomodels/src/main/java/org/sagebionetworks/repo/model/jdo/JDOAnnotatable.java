package org.sagebionetworks.repo.model.jdo;

/**
 * The interface implemented by all annotatable persistent objects
 * 
 * @author bhoff
 * 
 */
public interface JDOAnnotatable {
	/**
	 * 
	 * @return the annotations for the object
	 */
	JDOAnnotations getAnnotations();

	void setAnnotations(JDOAnnotations a);
}
