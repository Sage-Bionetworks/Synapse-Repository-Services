package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author bhoff
 *
 * @param <A>  the annotation value type (String, Boolean, Float, Date, Integer)
 */
public interface AnnotationDAO<S extends Base, A> {
	
	public void addAnnotation(String id, String attribute, A value) throws DatastoreException;
	
	public void removeAnnotation(String id, String attribute, A value) throws DatastoreException;
	
	public Map<String,Collection<A>> getAnnotations(String id) throws DatastoreException;


	/**
	 * 
	 * @return all attributes used for objects of type A in the system
	 */
	public Collection<String> getAttributes() throws DatastoreException ;
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @param sortByAttr
	 * @return a subset of the results, starting at index 'start' and not going beyond index 'end'
	 * and sorted by the given attribute
	 */
	public List<S> getInRangeSortedBy(int start, int end, String sortByAttr) throws DatastoreException ;
	
	public List<S> getInRangeHaving(int start, int end, String attribute, A value) throws DatastoreException ;
	

}
