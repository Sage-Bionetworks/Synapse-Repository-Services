package org.sagebionetworks.repo.web;

import java.util.List;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;

/**
 * Strategy pattern interface for querying, sorting, filtering, and sorting
 * entities of a particular type
 * 
 * @author deflaux
 * 
 */
public interface EntitiesAccessor<T extends Base> {

	public List<T> getInRange(int offset, int limit) throws DatastoreException;

	public List<T> getInRangeSortedBy(int offset, int limit, String sortBy,
			Boolean ascending) throws DatastoreException;

	public List<T> getInRangeHaving(int offset, int limit, String attribute,
			Object value);

}
