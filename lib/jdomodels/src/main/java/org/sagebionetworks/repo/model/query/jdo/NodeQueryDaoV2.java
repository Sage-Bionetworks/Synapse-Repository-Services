package org.sagebionetworks.repo.model.query.jdo;

import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.NodeQueryResults;
import org.sagebionetworks.repo.model.query.BasicQuery;

public interface NodeQueryDaoV2 {

	/**
	 * Execute the given query filtered by the provided benefactorIds.
	 * 
	 * @param query
	 * @param benefactorIds
	 * @return
	 * @throws DatastoreException
	 */
	public NodeQueryResults executeQuery(BasicQuery query, Set<Long> benefactorIds) throws DatastoreException;
	
}
