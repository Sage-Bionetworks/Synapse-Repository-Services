package org.sagebionetworks.repo.model.gaejdo;

import com.google.appengine.api.datastore.Key;

/**
 * The base interface for all annotations. Concrete classes will have typed
 * values. Note:  'equals' and 'hashcode' are defined on the ID field, not
 * the attribute&value fields.
 * 
 * @author bhoff
 * 
 * @param <T>
 *            the type of the value of the annotation
 */
public interface GAEJDOAnnotation<T> {
	/**
	 * 
	 * @return the datastore key for the annotation
	 */
	Key getId();

	/**
	 * 
	 * @param a
	 *            the attribute for the annotation
	 */
	void setAttribute(String a);

	/**
	 * 
	 * @return the attribute for the annotation
	 */
	String getAttribute();

	/**
	 * 
	 * @param value
	 *            the value for the annotation
	 */
	void setValue(T value);

	/**
	 * 
	 * @return the value for the annotation
	 */
	T getValue();

}
