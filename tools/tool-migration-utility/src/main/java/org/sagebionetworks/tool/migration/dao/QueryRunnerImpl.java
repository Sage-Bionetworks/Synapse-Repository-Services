package org.sagebionetworks.tool.migration.dao;

import java.util.ArrayList;
import java.util.List;

import static org.sagebionetworks.tool.migration.Constants.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.tool.migration.Progress.BasicProgress;

/**
 * This is the real implementation of the query runner.
 * 
 * @author jmhill
 *
 */
public class QueryRunnerImpl implements QueryRunner {
	

	public static final String SELECT_ENTITY_DATA = ENTITY_ID+", "+ENTITY_E_TAG+", "+ENTITY_PARENT_ID;
	public static final String QUERY_ROOT_ENTITY = "select "+SELECT_ENTITY_DATA+" from "+ENTITY+" where "+ENTITY_PARENT_ID+" == null and "+ENTITY_NAME+" == '"+ROOT_ENTITY_NAME+"'";
	
	public static final String QUERY_CHILDREN_OF_ENTITY1 = "select "+SELECT_ENTITY_DATA+" from "+ENTITY+" where "+ENTITY_PARENT_ID+" == ";
	
	/**
	 * This query is just used to get the total number of results.  So we limit the results to a single entity.
	 */
	public static final String QUERY_TOTAL_ENTITY = "select "+SELECT_ENTITY_DATA+" from "+ENTITY;
	public static final String QUERY_TOTAL_ENTITY_COUNT = QUERY_TOTAL_ENTITY+" "+LIMIT+" 1 "+OFFSET+" 1";


	/**
	 * Get all entity data from a given client.
	 * @param client
	 * @return
	 * @throws SynapseException 
	 * @throws IllegalAccessException 
	 * @throws JSONException 
	 * @throws InterruptedException 
	 */
	@Override
	public List<EntityData> getAllEntityData(Synapse client, BasicProgress progress) throws SynapseException, JSONException, InterruptedException{
		if(client == null) throw new IllegalArgumentException("Client cannot be null"); 
		// Build up the results to get all pages of this query.
		return queryForAllPages(client, QUERY_TOTAL_ENTITY, MAX_PAGE_SIZE, progress);
	}
	
	@Override
	public long getTotalEntityCount(Synapse client) throws SynapseException, JSONException {
		JSONObject json = client.query(QUERY_TOTAL_ENTITY_COUNT);
		EntityQueryResults results = translateFromJSONObjectToEntityQueryResult(json);
		return results.getTotalCount();
	}
	
	/**
	 * This is a recursive method that will walk all entities.
	 * @param client
	 * @param results
	 * @param parentId
	 * @throws SynapseException
	 * @throws IllegalAccessException
	 * @throws JSONException
	 * @throws InterruptedException
	 */
	private void recursiveAddAllChildren(Synapse client, List<EntityData> results, String parentId) throws SynapseException, JSONException, InterruptedException{
		// First get all of the children for this parent
		List<EntityData> allChildren = getAllAllChildrenOfEntity(client, parentId);
		results.addAll(allChildren);
		// Now add the children of the children
		for(EntityData child: allChildren){
			recursiveAddAllChildren(client, results, child.getEntityId());
		}
	}
	
	/**
	 * Get the root Entity from a repository.
	 * @param client
	 * @return
	 * @throws SynapseException
	 * @throws JSONException
	 */
	@Override
	public EntityData getRootEntity(Synapse client) throws SynapseException, JSONException{
		JSONObject json = client.query(QUERY_ROOT_ENTITY);
		EntityQueryResults results = translateFromJSONObjectToEntityQueryResult(json);
		if(results.getTotalCount() != 1) throw new IllegalArgumentException("Found more than one entity with a null parentId and name= "+ROOT_ENTITY_NAME);
		return results.getResutls().get(0);
	}
	
	/**
	 * Translate the query results from a JSONObject
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	public static EntityQueryResults translateFromJSONObjectToEntityQueryResult(JSONObject json) throws JSONException{
		// The total count
		long total = json.getLong(JSON_KEY_TOTAL_NUMBER_OF_RESULTS);
		JSONArray rows = json.getJSONArray(JSON_KEY_RESULTS);
		List<EntityData> results = new ArrayList<EntityData>();
		for(int i=0; i<rows.length(); i++){
			JSONObject row = rows.getJSONObject(i);
			EntityData data = new EntityData(getStringWithNull(row, ENTITY_DOT_ID), getStringWithNull(row, ENTITY_DOT_E_TAG),getStringWithNull(row,ENTITY_DOT_PARENT_ID));
			results.add(data);
		}
		return new EntityQueryResults(results, total);
	}
	
	/**
	 * Returns null if the value is null, else the string.
	 * @param object
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	private static String getStringWithNull(JSONObject object, String key) throws JSONException{
		if(object.isNull(key)) return null;
		return object.getString(key);
	}

	@Override
	public List<EntityData> getAllAllChildrenOfEntity(Synapse client, String parentId) throws SynapseException, JSONException, InterruptedException {
		String rootQuery = QUERY_CHILDREN_OF_ENTITY1 +parentId;
		return queryForAllPages(client, rootQuery, MAX_PAGE_SIZE, null);
	}
	
	/**
	 * Get all of the pages starting with a root query.
	 * This will execute the query one page at a time until all of the results are fetched. The page size is set by the limit.
	 * @param client
	 * @param rootQuery
	 * @param limit
	 * @return
	 * @throws SynapseException
	 * @throws IllegalAccessException
	 * @throws JSONException
	 * @throws InterruptedException 
	 */
	public List<EntityData> queryForAllPages(Synapse client, String rootQuery, long limit, BasicProgress progress) throws SynapseException, JSONException, InterruptedException {
		List<EntityData> results = new ArrayList<EntityData>();
		// First run the first page
		long offset = 1;
		String query = getPageQuery(rootQuery, limit, offset);
		JSONObject json = client.query(query);
		EntityQueryResults page = translateFromJSONObjectToEntityQueryResult(json);
		results.addAll(page.getResutls());
		long totalCount = page.getTotalCount();
		// Update the progress if we have any
		if(progress != null){
			progress.setTotal(totalCount);
		}
		// Get as many pages as needed
		while((offset = getNextOffset(offset, limit, totalCount)) > 0l){
			query = getPageQuery(rootQuery, limit, offset);
			json = client.query(query);
			page = translateFromJSONObjectToEntityQueryResult(json);
			if(progress != null){
				// Add this count to the current progress.
				progress.setCurrent(progress.getCurrent() + page.getResutls().size());
			}
			results.addAll(page.getResutls());
			// Yield between queries
			Thread.sleep(MS_BETWEEN_SYNPASE_CALLS);
		}
		
		if(progress != null){
			// Add this count to the current progress.
			progress.setCurrent(progress.getTotal());
		}
		return results;
	}
	/**
	 * What is the next offset given the current-offset, limit, and total count.
	 * Returns -1 when done paging.
	 * @param offset
	 * @param limit
	 * @param totalCount
	 * @return
	 */
	public static long getNextOffset(long offset, long limit, long totalCount){
		long next = offset+limit;
		if(next > totalCount) return -1;
		else return next;
	}
	
	/**
	 * Add paging to a root query.
	 * @param rootQuery
	 * @param limit
	 * @param offset
	 * @return
	 */
	private String getPageQuery(String rootQuery, long limit, long offset){
		StringBuilder builder = new StringBuilder();
		builder.append(rootQuery);
		builder.append(" ");
		builder.append(LIMIT);
		builder.append(" ");
		builder.append(limit);
		builder.append(" ");
		builder.append(OFFSET);
		builder.append(" ");
		builder.append(offset);
		return builder.toString();
	}



}
