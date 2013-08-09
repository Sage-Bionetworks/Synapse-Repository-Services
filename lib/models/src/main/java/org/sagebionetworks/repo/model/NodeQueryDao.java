package org.sagebionetworks.repo.model;

import org.sagebionetworks.repo.model.query.BasicQuery;

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
	public NodeQueryResults executeQuery(BasicQuery query, UserInfo userInfo) throws DatastoreException;
	
	/**
	 * Execute the given query as a 'count' query.  The count will be the number of nodes that meet the passed criteria.
	 * @param query
	 * @param userInfo
	 * @return
	 * @throws DatastoreException
	 */
	public long executeCountQuery(BasicQuery query, UserInfo userInfo) throws DatastoreException;

}
