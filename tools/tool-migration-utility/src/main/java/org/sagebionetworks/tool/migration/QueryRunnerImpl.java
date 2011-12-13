package org.sagebionetworks.tool.migration;

import java.util.ArrayList;
import java.util.List;

import static org.sagebionetworks.tool.migration.Constants.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.client.Synapse;
import org.sagebionetworks.client.exceptions.SynapseException;

/**
 * This is the real implementation of the query runner.
 * 
 * @author jmhill
 *
 */
public class QueryRunnerImpl implements QueryRunner {
	

	public static final String SELECT_ENTITY_DATA = ENTITY_ID+", "+ENTITY_E_TAG+", "+ENTITY_PARENT_ID;
	private static final String QUERY_ROOT_ENTITY = "select "+SELECT_ENTITY_DATA+" from "+ENTITY+" where "+ENTITY_PARENT_ID+" == null and "+ENTITY_NAME+" == '"+ROOT_ENTITY_NAME+"'";
	
	/**
	 * Get all entity data from a given client.
	 * @param client
	 * @return
	 * @throws SynapseException 
	 * @throws IllegalAccessException 
	 */
	@Override
	public List<EntityData> getAllEntityData(Synapse client) throws SynapseException, IllegalAccessException{
		if(client == null) throw new IllegalAccessException("Client cannot be null"); 
		// First run a query to find the root Entity

		
		return null;
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
	 * 
	 * @param object
	 * @param key
	 * @return
	 * @throws JSONException
	 */
	private static String getStringWithNull(JSONObject object, String key) throws JSONException{
		if(object.isNull(key)) return null;
		return object.getString(key);
	}

}
