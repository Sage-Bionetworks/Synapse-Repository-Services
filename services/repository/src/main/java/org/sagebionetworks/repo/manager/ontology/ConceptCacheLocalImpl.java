package org.sagebionetworks.repo.manager.ontology;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ontology.Concept;
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
	private Map<String, Concept> localConceptCache = Collections.synchronizedMap(new HashMap<String, Concept>());

	@Override
	public List<ConceptSummary> getConceptSummary(String key) {
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

	@Override
	public Concept getConcept(String key) {
		return localConceptCache.get(key);
	}

	@Override
	public void put(String key, Concept value) {
		localConceptCache.put(key, value);
	}

}
