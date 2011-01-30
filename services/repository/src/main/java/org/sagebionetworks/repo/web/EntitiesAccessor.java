package org.sagebionetworks.repo.web;

import java.util.List;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;

/**
 * Strategy pattern interface for querying, sorting, filtering, and sorting
 * entities of a particular type
 * 
 * @author deflaux
 * @param <T>
 *            the particular type of entity we are querying
 * 
 */
public interface EntitiesAccessor<T extends Base> {

	/**
	 * @param offset
	 * @param limit
	 * @return the list of zero or more entities found
	 * @throws DatastoreException
	 */
	public List<T> getInRange(int offset, int limit) throws DatastoreException;

	/**
	 * @param offset
	 * @param limit
	 * @param sortBy
	 * @param ascending
	 * @return the list of zero or more entities found
	 * @throws DatastoreException
	 */
	public List<T> getInRangeSortedBy(int offset, int limit, String sortBy,
			Boolean ascending) throws DatastoreException;

	/**
	 * @param offset
	 * @param limit
	 * @param attribute
	 * @param value
	 * @return the list of zero or more entities found
	 */
	public List<T> getInRangeHaving(int offset, int limit, String attribute,
			Object value);

	/**
	 * @param dao
	 */
	public void setDao(BaseDAO<T> dao);

}
