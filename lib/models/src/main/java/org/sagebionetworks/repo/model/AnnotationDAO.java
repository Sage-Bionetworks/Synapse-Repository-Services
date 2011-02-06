package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This interface defines the CRUD methods for annotations.
 * 
 * @author bhoff
 * 
 * @param <S>
 *            the Data Transfer Object type for the annotatable object
 * @param <A>
 *            the annotation value type (String, Float, Date)
 */
public interface AnnotationDAO<S extends Base, A extends Comparable> {

//	/**
//	 * 
//	 * @param id
//	 *            the ID of the annotations owner
//	 * @param attribute
//	 * @param value
//	 * @throws DatastoreException
//	 * @throws NotFoundException
//	 */
//	@Deprecated
//	public void addAnnotation(String id, String attribute, A value)
//			throws DatastoreException, NotFoundException;
//
//	/**
//	 * 
//	 * @param id
//	 *            the ID of the annotations owner
//	 * @param attribute
//	 * @param value
//	 * @throws DatastoreException
//	 * @throws NotFoundException
//	 */
//	@Deprecated
//	public void removeAnnotation(String id, String attribute, A value)
//			throws DatastoreException, NotFoundException;
//
//	/**
//	 * Note: Since an object may have multiple values for the same attribute,
//	 * the values for the returned Map are Collection<A> rather than A.
//	 * 
//	 * @param id
//	 *            the ID of the annotations owner
//	 * @return all the annotations of the type given by A owned by the
//	 *         annotatable object
//	 * @throws DatastoreException
//	 * @throws NotFoundException
//	 */
//	@Deprecated
//	public Map<String, Collection<A>> getAnnotations(String id)
//			throws DatastoreException, NotFoundException;


	/**
	 * 
	 * @param attribute
	 * @param value
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void addAnnotation(String attribute, A value)
			throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @param attribute
	 * @param value
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void removeAnnotation(String attribute, A value)
			throws DatastoreException, NotFoundException;

	/**
	 * Note: Since an object may have multiple values for the same attribute,
	 * the values for the returned Map are Collection<A> rather than A.
	 * 
	 * @return all the annotations of the type given by A owned by the
	 *         annotatable object
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public Map<String, Collection<A>> getAnnotations()
			throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @return all attributes used for objects of type A in the system
	 */
	public Collection<String> getAttributes() throws DatastoreException;

	/**
	 * 
	 * Note: Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @param sortByAttr
	 * @params ascending if true then ascending, otherwise descending
	 * @return a subset of the results, starting at index 'start' and not going
	 *         beyond index 'end' and sorted by the given attribute
	 */
	public List<S> getInRangeSortedBy(int start, int end, String sortByAttr,
			boolean ascending) throws DatastoreException;

	/**
	 * 
	 * Note: Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @param attribute
	 * @param value
	 * @return a subset of results, starting at index 'start' and not going
	 *         beyond index 'end', having the given value for the given
	 *         attribute
	 * @throws DatastoreException
	 */
	public List<S> getInRangeHaving(int start, int end, String attribute,
			A value) throws DatastoreException;

}
