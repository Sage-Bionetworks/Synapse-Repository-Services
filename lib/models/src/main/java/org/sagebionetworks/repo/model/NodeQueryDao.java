package org.sagebionetworks.repo.model;

import java.util.Set;

import org.sagebionetworks.repo.model.query.BasicQuery;
import org.sagebionetworks.repo.model.query.FieldType;

/**
 * Used to query for Node IDs.
 * 
 * @author jmhill
 *
 */
public interface NodeQueryDao {
	
	/**
	 * Execute a query, and return a paginated list of node ids.
	 * @param query
	 * @return
	 * @throws DatastoreException 
	 */
	public NodeQueryResults executeQuery(BasicQuery query) throws DatastoreException;

}
