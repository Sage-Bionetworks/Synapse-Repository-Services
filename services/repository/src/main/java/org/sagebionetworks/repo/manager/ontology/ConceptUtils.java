package org.sagebionetworks.repo.manager.ontology;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptSummary;
import org.sagebionetworks.repo.model.ontology.ConceptSummaryComparator;

/**
 * Functional methods for working with concepts.
 * 
 * @author John
 *
 */
public class ConceptUtils {
	
	/**
	 * Build up the list of prefixes for a String.
	 * @param uniquePart
	 * @param value
	 * @return
	 */
	public static void addAllLowerCasePrefixToSet(String uniquePart, String value, Set<String> results){
		if(uniquePart == null) throw new IllegalArgumentException("UniquePart cannot be null");
		if(value == null) throw new IllegalArgumentException("Value cannot be null");
		char[] chars = value.toLowerCase().toCharArray();
		for(int i=1; i<chars.length+1; i++){
			results.add(uniquePart+new String(chars, 0, i));
		}
	}
	
	/**
	 * Create the set of all lower case prefixes for a concept.
	 * @param uniquePart
	 * @param concept
	 * @return
	 */
	public static Set<String> getAllLowerCasePefix(String uniquePart, Concept concept){
		if(uniquePart == null) throw new IllegalArgumentException("UniquePart cannot be null");
		if(concept == null) throw new IllegalArgumentException("Concept cannot be null");
		if(concept.getSummary() == null) throw new IllegalArgumentException("Concept summary cannot be null");
		Set<String> pefixSet = new HashSet<String>();
		// Add the preferred label
		ConceptUtils.addAllLowerCasePrefixToSet(uniquePart, concept.getSummary().getPreferredLabel(), pefixSet);
		// Add all of the synonyms
		List<String> synonyms = concept.getSynonyms();
		if(synonyms != null){
			for(String synonym: synonyms){
				// Add the synonym label
				ConceptUtils.addAllLowerCasePrefixToSet(uniquePart, synonym, pefixSet);
			}
		}
		return pefixSet;
	}
	
	/**
	 * Populate the map with all lower case prefix values for a givne concept.
	 * @param uniquePart
	 * @param con
	 * @param result
	 */
	public static void populateMapWithLowerCasePrefixForConcept(String uniquePart, Concept con, Map<String, List<ConceptSummary>> map){
		// First get the set of prefix keys
		Set<String> prefixKeys = ConceptUtils.getAllLowerCasePefix(uniquePart, con);
		// For each value in the set add to the map
		for(String key: prefixKeys){
			// Is there a list for this 
			List<ConceptSummary> list = map.get(key);
			if(list == null){
				list = new LinkedList<ConceptSummary>();
				map.put(key, list);
			}
			// Add the summary to this list
			list.add(con.getSummary());
		}
	}
	
	/**
	 * Sort all lists based fist on preferred label then URI.
	 * @param map
	 */
	public static void sortAllSummaryLists(Map<String, List<ConceptSummary>> map){
		// Sort each list using the comparator.
		ConceptSummaryComparator comparator = new ConceptSummaryComparator();
		for(List<ConceptSummary> list: map.values()){
			Collections.sort(list, comparator);
		}
	}
}
