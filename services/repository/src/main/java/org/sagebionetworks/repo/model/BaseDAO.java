package org.sagebionetworks.repo.model;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.jdo.PersistenceManager;

import org.sagebionetworks.repo.model.gaejdo.GAEJDOScript;
import org.sagebionetworks.repo.model.gaejdo.PMF;
import org.sagebionetworks.repo.web.NotFoundException;

import com.google.appengine.api.datastore.Key;

public interface BaseDAO<T> {

	/**
	 * @param dto
	 *            object to be created
	 * @return the id of the newly created object
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	public String create(T dto) throws DatastoreException,
			InvalidModelException;

	/**
	 * Retrieves the object given its id
	 * 
	 * @param id
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public T get(String id) throws DatastoreException, NotFoundException;

	/**
	 * This updates the 'shallow' properties of an object
	 * 
	 * @param dto
	 *            non-null id is required
	 * @throws DatastoreException
	 */
	public void update(T dto) throws DatastoreException;

	/**
	 * delete the object given by the given ID
	 * 
	 * @param id
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	public void delete(String id) throws DatastoreException, NotFoundException;

	/**
	 * 
	 * @return the number of objects (not counting revisions (if revisable))
	 * @throws DatastoreException
	 */
	public int getCount() throws DatastoreException;

	/**
	 * 
	 * @param start
	 * @param end
	 * @return a subset of the results, starting at index 'start' and not going
	 *         beyond index 'end'
	 */
	public List<T> getInRange(int start, int end);

	public Collection<String> getPrimaryFields();

	/**
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
			String sortBy, boolean asc);

	public List<T> getInRangeHavingPrimaryField(int start, int end,
			String attribute, Object value);

}
