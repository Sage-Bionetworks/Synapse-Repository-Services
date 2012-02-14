package org.sagebionetworks.repo.manager.ontology;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptSummary;

public class ConceptUtilsTest {
	
	Concept concept;
	ConceptSummary summary;
	Concept concept2;
	ConceptSummary summary2;
	
	@Before
	public void before(){
		concept = new Concept();
		concept.setPreferredLabel("Cat");
		concept.setUri("urn:cat");
		concept.setSynonyms(new ArrayList<String>());
		concept.getSynonyms().add("Feline");
		concept.getSynonyms().add("Cats");
		summary = new ConceptSummary();
		summary.setPreferredLabel(concept.getPreferredLabel());
		summary.setUri(concept.getUri());
		
		concept2 = new Concept();
		concept2.setPreferredLabel("Can");
		concept2.setUri("urn:can");
		concept2.setSynonyms(new ArrayList<String>());
		concept2.getSynonyms().add("Tin Can");
		summary = new ConceptSummary();
		summary.setPreferredLabel(concept2.getPreferredLabel());
		summary.setUri(concept2.getUri());
	}
	
	@Test
	public void testAddAllLowerCasePrefixToSet(){
		String key = "aBcD";
		String unique = "one#";
		Set<String> results = new HashSet<String>();
		ConceptUtils.addAllLowerCasePrefixToSet(unique, key, results);
		// We expect the set to contain the following strings
		assertEquals(4, results.size());
		assertTrue(results.contains("one#a"));
		assertTrue(results.contains("one#ab"));
		assertTrue(results.contains("one#abc"));
		assertTrue(results.contains("one#abcd"));
	}
	

	@Test
	public void testGetAllLowerCasePefix(){
		String unique = "one#";
		Set<String> results = ConceptUtils.getAllLowerCasePefix(unique, concept);
		// These are the expected prefixes
		assertEquals(10, results.size());
		assertTrue(results.contains("one#c"));
		assertTrue(results.contains("one#ca"));
		assertTrue(results.contains("one#cat"));
		assertTrue(results.contains("one#cats"));
		assertTrue(results.contains("one#f"));
		assertTrue(results.contains("one#fe"));
		assertTrue(results.contains("one#fel"));
		assertTrue(results.contains("one#feli"));
		assertTrue(results.contains("one#felin"));
		assertTrue(results.contains("one#feline"));
	}
	
	@Test
	public void testPopulateMapWithLowerCasePrefixForConcept(){
		String unique = "one#";
		// This is the map we will populate
		Map<String, List<Concept>> map = new HashMap<String, List<Concept>>();
		ConceptUtils.populateMapWithLowerCasePrefixForConcept(unique, concept, map);
		assertEquals(10, map.size());
		assertNotNull(map.get("one#c"));
		assertEquals(1, map.get("one#c").size());
		assertEquals(concept, map.get("one#c").iterator().next());
		// Now add the second concept to the map
		ConceptUtils.populateMapWithLowerCasePrefixForConcept(unique, concept2, map);
		assertEquals(18, map.size());
		// There should now be two things that start with 'c'
		assertNotNull(map.get("one#c"));
		List<Concept> list = map.get("one#c");
		assertEquals(2, list.size());
		assertEquals(concept, list.get(0));
		assertEquals(concept2, list.get(1));
		// There should now be two things that start with 'ca'
		assertNotNull(map.get("one#ca"));
		list = map.get("one#ca");
		assertEquals(2, list.size());
		assertEquals(concept, list.get(0));
		assertEquals(concept2, list.get(1));
		// There should only be one thing that starts with 'can'
		assertNotNull(map.get("one#can"));
		list = map.get("one#can");
		assertEquals(1, list.size());
		assertEquals(concept2, list.get(0));
	}

}
