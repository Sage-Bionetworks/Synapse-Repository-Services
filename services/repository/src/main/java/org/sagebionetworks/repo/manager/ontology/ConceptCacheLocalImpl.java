package org.sagebionetworks.repo.manager.ontology;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ontology.ConceptSummary;

/**
 * This is a local memory implementation of the ConceptCache
 * 
 * @author jmhill
 *
 */
public class ConceptCacheLocalImpl implements ConceptCache {
	
	/**
	 * The local in-memory cache.
	 */
	private Map<String, List<ConceptSummary>> localCache = Collections.synchronizedMap(new HashMap<String, List<ConceptSummary>>());

	@Override
	public List<ConceptSummary> get(String key) {
		return localCache.get(key);
	}

	@Override
	public void put(String key, List<ConceptSummary> value) {
		localCache.put(key, value);
	}

	@Override
	public boolean containsKey(String key) {
		return localCache.containsKey(key);
	}

	@Override
	public void putAll(Map<String, List<ConceptSummary>> map) {
		localCache.putAll(map);
	}

}
