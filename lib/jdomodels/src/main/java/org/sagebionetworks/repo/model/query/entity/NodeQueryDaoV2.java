package org.sagebionetworks.repo.model.query.entity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.DatastoreException;

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

	/**
	 * Get the set of distinct benefactor Ids for a given query.
	 * 
	 * @param model
	 * @param limit
	 * @return
	 */
	public Set<Long> getDistinctBenefactors(QueryModel model, long limit);
	
}
