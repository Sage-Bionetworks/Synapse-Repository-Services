package org.sagebionetworks.repo.manager.ontology;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ontology.Concept;

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
	private Map<String, List<Concept>> localCache = Collections.synchronizedMap(new HashMap<String, List<Concept>>());
	private Map<String, Concept> localConceptCache = Collections.synchronizedMap(new HashMap<String, Concept>());

	@Override
	public boolean containsKey(String key) {
		return localCache.containsKey(key);
	}

	@Override
	public Concept getConcept(String key) {
		return localConceptCache.get(key);
	}

	@Override
	public void put(String key, Concept value) {
		localConceptCache.put(key, value);
	}

	@Override
	public List<Concept> getConceptsForKey(String key) {
		return localCache.get(key);
	}

	@Override
	public void put(String key, List<Concept> value) {
		localCache.put(key, value);
	}

	@Override
	public void putAll(Map<String, List<Concept>> map) {
		localCache.putAll(map);
	}

}
