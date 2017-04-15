package org.sagebionetworks.repo.model.query.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for query parameters.
 *
 */
public class Parameters {

	Map<String, Object> params;
	
	public Parameters(){
		params = new HashMap<String, Object>();
	}
	
	/**
	 * Bind a parameter to this query.
	 * 
	 * @param key
	 * @param value
	 */
	public void put(String key, Object value){
		Object old = params.put(key, value);
		if(old != null){
			throw new IllegalStateException("Duplicate parameter key: "+key);
		}
	}

}
