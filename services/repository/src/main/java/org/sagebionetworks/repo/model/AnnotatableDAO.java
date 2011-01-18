package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.gaejdo.GAEJDOAnnotatable;

import com.google.appengine.api.datastore.Text;

public interface AnnotatableDAO<T> {
	
	public void addAnnotation(String id, String attribute, String value) throws DatastoreException;
	public void addAnnotation(String id, String attribute, Integer value) throws DatastoreException;
	public void addAnnotation(String id, String attribute, Float value) throws DatastoreException;
	public void addAnnotation(String id, String attribute, Boolean value) throws DatastoreException;
	public void addAnnotation(String id, String attribute, Date value) throws DatastoreException;
	
	public void removeAnnotation(String id, String attribute, String value) throws DatastoreException;
	public void removeAnnotation(String id, String attribute, Integer value) throws DatastoreException;
	public void removeAnnotation(String id, String attribute, Float value) throws DatastoreException;
	public void removeAnnotation(String id, String attribute, Boolean value) throws DatastoreException;
	public void removeAnnotation(String id, String attribute, Date value) throws DatastoreException;
	
	/**
	 * @param id
	 * @return annotations for the given object and the given attribute
	 */
	public Annotations getAnnotations(String id) throws DatastoreException ;

//	// if needed we could add methods to retrieve 
//	public Collection<Date> getStringAnnotations(String id, String attribute) throws DatastoreException;
//	public Collection<Date> getBooleanAnnotations(String id, String attribute) throws DatastoreException;
//	public Collection<Date> getFloatAnnotations(String id, String attribute) throws DatastoreException;
//	public Collection<Date> getIntegerAnnotations(String id, String attribute) throws DatastoreException;
//	public Collection<Date> getDateAnnotations(String id, String attribute) throws DatastoreException;
	

	/**
	 * 
	 * @return all attributes used for objects of type T in the system
	 */
	public Collection<String> getStringAttributes() throws DatastoreException ;
	public Collection<Integer> getIntegerAttributes() throws DatastoreException ;
	public Collection<Boolean> getBooleanAttributes() throws DatastoreException ;
	public Collection<Float> getFloatAttributes() throws DatastoreException ;
	public Collection<Date> getDateAttributes() throws DatastoreException ;
	
	/**
	 * 
	 * @param start
	 * @param end
	 * @param sortByAttr
	 * @return a subset of the results, starting at index 'start' and not going beyond index 'end'
	 * and sorted by the given attribute
	 */
	public List<T> getInRangeSortedByString(int start, int end, String sortByAttr) throws DatastoreException ;
	public List<T> getInRangeSortedByInteger(int start, int end, String sortByAttr) throws DatastoreException ;
	public List<T> getInRangeSortedByBoolean(int start, int end, String sortByAttr) throws DatastoreException ;
	public List<T> getInRangeSortedByFloat(int start, int end, String sortByAttr) throws DatastoreException ;
	public List<T> getInRangeSortedByDate(int start, int end, String sortByAttr) throws DatastoreException ;
	
	public List<T> getInRangeHaving(int start, int end, String attribute, String value) throws DatastoreException ;
	public List<T> getInRangeHaving(int start, int end, String attribute, Integer value) throws DatastoreException ;
	public List<T> getInRangeHaving(int start, int end, String attribute, Boolean value) throws DatastoreException ;
	public List<T> getInRangeHaving(int start, int end, String attribute, Float value) throws DatastoreException ;
	public List<T> getInRangeHaving(int start, int end, String attribute, Date value) throws DatastoreException ;
	

}
