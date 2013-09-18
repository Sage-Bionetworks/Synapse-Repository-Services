package org.sagebionetworks.repo.manager.ontology;

import java.util.List;
import java.util.Map;

import org.sagebionetworks.repo.model.ontology.Concept;

public interface ConceptCache {
		
	/**
	 * Get the ordered list of concepts that match the given key.
	 * @param key
	 * @return
	 */
	public List<Concept> getConceptsForKey(String key);
	
	/**
	 * Get a concpet by its URL
	 * @param key
	 * @return
	 */
	public Concept getConcept(String uri);
	
	/**
	 * Set the concept summary list for a given key.
	 * @param key
	 * @param value
	 */
	public void put(String key, List<Concept> value);
	
	/**
	 * Put a concept.
	 * @param key
	 * @param value
	 */
	public void put(String key, Concept value);
	
	/**
	 * Add all values from the passed map.
	 * @param map
	 */
	public void putAll(Map<String, List<Concept>> map);
	
	/**
	 * Does the cache contain the given key.
	 * @param key
	 * @return
	 */
	public boolean containsKey(String key);
}
