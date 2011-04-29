package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.List;

import org.sagebionetworks.repo.web.NotFoundException;

/**
 * This interface defines the basic data access functionality which all DAO
 * classes will implement.
 * 
 * @author bhoff
 * 
 * @param <T>
 */
public interface BaseDAO<T> {
	
	/**
	 * @return the type of the object which the DAO serves
	 */
	public String getType();

	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(T dto) throws DatastoreException,
			InvalidModelException, UnauthorizedException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public T get(String id) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * This updates the 'shallow' properties of an object
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 * @throws NotFoundException
	 */
	public void update(T dto) throws DatastoreException, InvalidModelException,
			NotFoundException, UnauthorizedException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 *            the id of the object to be deleted
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException, UnauthorizedException;

	/**
	 * 
	 * @return the number of objects (not counting revisions (if revisable))
	 * @throws DatastoreException
	 */
	public int getCount() throws DatastoreException;

	/**
	 * Note: Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @return a subset of the results, starting at index 'start' and less than
	 *         index 'end'
	 */
	public List<T> getInRange(int start, int end) throws DatastoreException;

	public Collection<String> getPrimaryFields();

	/**
	 * Note: Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @param sortBy
	 * @param asc
	 *            if true then ascending, else descending
	 * @return a subset of the results, starting at index 'start' and not going
	 *         beyond index 'end' and sorted by the given primary field
	 */
	public List<T> getInRangeSortedByPrimaryField(int start, int end,
			String sortBy, boolean asc) throws DatastoreException;

	/**
	 * Note: Invalid range returns empty list rather than throwing exception
	 * 
	 * @param start
	 * @param end
	 * @param attribute
	 * @param value
	 * @return a subset of results, starting at index 'start' and not going
	 *         beyond index 'end', having the given value for the given field
	 */
	public List<T> getInRangeHavingPrimaryField(int start, int end,
			String attribute, Object value) throws NotFoundException, DatastoreException;
	
	// TODO need a clean way to convey Public access
	/**
	 * Use case:  Need to find out who has authority to add a new user to a group.
	 * Here the 'resource' refers to the group and 'accessType' = 'change'.  The method
	 * would return the administrative group who can modify the group of interest.
	 * 
	 * @param resource the resource of interest
	 * @param accessType a type of access to the object
	 * @return those user groups that have the specified type of access to the given object
	 */
	public Collection<UserGroup> whoHasAccess(T resource, String accessType) throws NotFoundException, DatastoreException ;
	
	/**
	 * Use case:  Need to find out if a user can download a resource.
	 * 
	 * @param resource the resource of interest
	 * @param user
	 * @param accessType
	 * @return
	 */
	public boolean hasAccess(T resource, String accessType) throws NotFoundException, DatastoreException ;

}
