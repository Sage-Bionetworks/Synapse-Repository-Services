package org.sagebionetworks.tool.migration.dao;

import static org.sagebionetworks.tool.migration.Constants.ENTITY;
import static org.sagebionetworks.tool.migration.Constants.ENTITY_E_TAG;
import static org.sagebionetworks.tool.migration.Constants.ENTITY_ID;
import static org.sagebionetworks.tool.migration.Constants.ENTITY_NAME;
import static org.sagebionetworks.tool.migration.Constants.ENTITY_PARENT_ID;
import static org.sagebionetworks.tool.migration.Constants.JSON_KEY_RESULTS;
import static org.sagebionetworks.tool.migration.Constants.JSON_KEY_TOTAL_NUMBER_OF_RESULTS;
import static org.sagebionetworks.tool.migration.Constants.LIMIT;
import static org.sagebionetworks.tool.migration.Constants.MAX_PAGE_SIZE;
import static org.sagebionetworks.tool.migration.Constants.MS_BETWEEN_SYNPASE_CALLS;
import static org.sagebionetworks.tool.migration.Constants.OFFSET;
import static org.sagebionetworks.tool.migration.Constants.ROOT_ENTITY_NAME;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;
import org.sagebionetworks.repo.model.EntityType;
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
	public static final String QUERY_TOTAL_ENTITY_TYPE_COUNT = "select "+SELECT_ENTITY_DATA+" from %1$s "+LIMIT+" 1 "+OFFSET+" 1";
	
	public static final String QUERY_ALL_OF_TYPE_FORMAT = "select "+SELECT_ENTITY_DATA+" from %1$s";

	private Synapse client;
	
	public QueryRunnerImpl(Synapse client) {
		this.client = client;
	}
	
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
	public List<EntityData> getAllEntityData(BasicProgress progress) throws SynapseException, JSONException, InterruptedException{
		if(client == null) throw new IllegalArgumentException("Client cannot be null"); 
		// Build up the results to get all pages of this query.
		return queryForAllPages(QUERY_TOTAL_ENTITY, ENTITY, MAX_PAGE_SIZE, progress);
	}
	
	@Override
	public List<EntityData> getAllEntityDataOfType(EntityType type, BasicProgress progress) throws SynapseException,
			JSONException, InterruptedException {
		// Build up the results to get all pages of this query.
		return queryForAllPages(String.format(QUERY_ALL_OF_TYPE_FORMAT, type.name()), type.name(), MAX_PAGE_SIZE, progress);
	}
	
	@Override
	public long getTotalEntityCount() throws SynapseException, JSONException {
		JSONObject json = client.query(QUERY_TOTAL_ENTITY_COUNT);
		EntityQueryResults results = translateFromJSONObjectToEntityQueryResult(json, ENTITY);
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
	private void recursiveAddAllChildren(List<EntityData> results, String parentId) throws SynapseException, JSONException, InterruptedException{
		// First get all of the children for this parent
		List<EntityData> allChildren = getAllAllChildrenOfEntity(parentId);
		results.addAll(allChildren);
		// Now add the children of the children
		for(EntityData child: allChildren){
			recursiveAddAllChildren(results, child.getEntityId());
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
	public EntityData getRootEntity() throws SynapseException, JSONException{
		JSONObject json = client.query(QUERY_ROOT_ENTITY);
		EntityQueryResults results = translateFromJSONObjectToEntityQueryResult(json, ENTITY);
		if(results.getTotalCount() != 1) throw new IllegalArgumentException("Found more than one entity with a null parentId and name= "+ROOT_ENTITY_NAME);
		return results.getResults().get(0);
	}
	
	/**
	 * Translate the query results from a JSONObject
	 * @param json
	 * @return
	 * @throws JSONException
	 */
	public static EntityQueryResults translateFromJSONObjectToEntityQueryResult(JSONObject json, String prefix) throws JSONException{
		// The total count
		long total = json.getLong(JSON_KEY_TOTAL_NUMBER_OF_RESULTS);
		JSONArray rows = json.getJSONArray(JSON_KEY_RESULTS);
		String idKey = prefix+"."+ENTITY_ID;
		String etagKey = prefix+"."+ENTITY_E_TAG;
		String parentKey = prefix+"."+ENTITY_PARENT_ID;
		List<EntityData> results = new ArrayList<EntityData>();
		for(int i=0; i<rows.length(); i++){
			JSONObject row = rows.getJSONObject(i);
			EntityData data = new EntityData(getStringWithNull(row, idKey), getStringWithNull(row, etagKey),getStringWithNull(row,parentKey));
			preProcessEntityData(data);
			results.add(data);
		}
		return new EntityQueryResults(results, total);
	}
	
	/**
	 * Check the id and parent ID for the prefix.
	 * @param data
	 * @return
	 */
	public static EntityData preProcessEntityData(EntityData data){
		// Convert the id and the parent ID.
		data.setEntityId(stripPrefixID(data.getEntityId()));
		data.setParentId(stripPrefixID(data.getParentId()));
		return data;
	}
	
	public static String stripPrefixID(String in){
		if (null == in)
			return null;
		if(in.startsWith(QueryRunner.ENTITY_ID_PREFIX)){
			return in.substring(QueryRunner.ENTITY_ID_PREFIX.length());
		}else{
			return in;
		}
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
	public List<EntityData> getAllAllChildrenOfEntity(String parentId) throws SynapseException, JSONException, InterruptedException {
		String rootQuery = QUERY_CHILDREN_OF_ENTITY1 + "\"" + parentId + "\"";
		return queryForAllPages(rootQuery, ENTITY, MAX_PAGE_SIZE, null);
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
	public List<EntityData> queryForAllPages(String rootQuery, String prefix, long limit, BasicProgress progress) throws SynapseException, JSONException, InterruptedException {
		List<EntityData> results = new ArrayList<EntityData>();
		// First run the first page
		long offset = 1;
		String query = getPageQuery(rootQuery, limit, offset);
		JSONObject json = client.query(query);
		EntityQueryResults page = translateFromJSONObjectToEntityQueryResult(json, prefix);
		results.addAll(page.getResults());
		long totalCount = page.getTotalCount();
		// Update the progress if we have any
		if(progress != null){
			progress.setTotal(totalCount);
		}
		// Get as many pages as needed
		while((offset = getNextOffset(offset, limit, totalCount)) > 0l){
			query = getPageQuery(rootQuery, limit, offset);
			json = client.query(query);
			page = translateFromJSONObjectToEntityQueryResult(json, prefix);
			if(progress != null){
				// Add this count to the current progress.
				progress.setCurrent(progress.getCurrent() + page.getResults().size());
			}
			results.addAll(page.getResults());
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

	@Override
	public long getCountForType(EntityType type)
			throws SynapseException, JSONException {
		String query = String.format(QUERY_TOTAL_ENTITY_TYPE_COUNT, type.name());
		JSONObject json = client.query(query);
		EntityQueryResults results = translateFromJSONObjectToEntityQueryResult(json, type.name());
		return results.getTotalCount();
	}

}
