package org.sagebionetworks.repo.model.gaejdo;

/**
 * The interface implemented by all annotatable persistent objects
 * 
 * @author bhoff
 * 
 */
public interface GAEJDOAnnotatable {
	/**
	 * 
	 * @return the annotations for the object
	 */
	GAEJDOAnnotations getAnnotations();

	void setAnnotations(GAEJDOAnnotations a);
}
