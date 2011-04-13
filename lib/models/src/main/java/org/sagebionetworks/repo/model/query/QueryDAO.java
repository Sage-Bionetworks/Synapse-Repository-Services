package org.sagebionetworks.repo.model.query;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;

public interface QueryDAO {
	
	/**
	 * Execute a query.
	 * 
	 * @param query
	 * @return
	 */
	public QueryResults executeQuery(BasicQuery query)  throws DatastoreException;
	
	/**
	 * 
	 * @param fieldName
	 * @return
	 * @throws DatastoreException 

	 */
	public FieldType getFieldType(String fieldName) throws DatastoreException;
}
