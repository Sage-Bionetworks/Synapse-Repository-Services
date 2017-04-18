package org.sagebionetworks.repo.model.query.jdo;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.query.entity.QueryModel;

public interface NodeQueryDaoV2 {

	/**
	 * Execute the given query filtered by the provided benefactorIds.
	 * 
	 * @param query
	 * @param benefactorIds
	 * @return
	 * @throws DatastoreException
	 */
	public List<Map<String, Object>> executeQuery(QueryModel model);
	
	/**
	 * Execute a count query for the given model.
	 * 
	 * @param model
	 * @return
	 */
	public long executeCountQuery(QueryModel model);
	
	/**
	 * Add all of the annotations to the given results.
	 * 
	 * @param results
	 */
	public void addAnnotationsToResults(List<Map<String, Object>> results);
	
}
