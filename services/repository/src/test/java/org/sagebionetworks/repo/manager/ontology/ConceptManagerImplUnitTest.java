package org.sagebionetworks.repo.manager.ontology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptSummary;
import org.sagebionetworks.repo.web.NotFoundException;

/**
 * Unit test for ConceptManagerImpl.
 * @author jmhill
 *
 */
public class ConceptManagerImplUnitTest {
	
	ConceptManagerImpl manager;
	ConceptCacheLocalImpl cache;
	String baseURI;
	
	String parentURI;
	ConceptSummary childA;
	ConceptSummary childB;
	ConceptSummary childC;
	
	@Before
	public void before(){
		// The base URI
		baseURI = "http://example.com/";
		
		List<Concept> concepts = new ArrayList<Concept>();
		
		// The parent
		Concept parentA = new Concept();
		parentA.setSummary(new ConceptSummary());
		parentA.getSummary().setPreferredLabel("Parent A");
		parentURI = baseURI+"parentA";
		parentA.getSummary().setUri(parentURI);
		parentA.setSynonyms(new ArrayList<String>());
		parentA.getSynonyms().add("Frank");
		concepts.add(parentA);
		
		// Add some children
		Concept child = new Concept();
		childA = new ConceptSummary();
		child.setSummary(childA);
		child.getSummary().setPreferredLabel("Child A3");
		child.getSummary().setUri(baseURI+"childA");
		child.setParent(parentA.getSummary().getUri());
		child.setSynonyms(new ArrayList<String>());
		child.getSynonyms().add("one");
		child.getSynonyms().add("three");
		concepts.add(child);
		
		// Add some children
		child = new Concept();
		childB = new ConceptSummary();
		child.setSummary(childB);
		child.getSummary().setPreferredLabel("Child A4");
		child.getSummary().setUri(baseURI+"childB");
		child.setParent(parentA.getSummary().getUri());
		child.setSynonyms(new ArrayList<String>());
		child.getSynonyms().add("one");
		child.getSynonyms().add("two");
		concepts.add(child);
		
		child = new Concept();
		childC = new ConceptSummary();
		child.setSummary(childC);
		child.getSummary().setPreferredLabel("Child A");
		child.getSummary().setUri(baseURI+"childC");
		child.setParent(parentA.getSummary().getUri());
		child.setSynonyms(new ArrayList<String>());
		child.getSynonyms().add("two");
		child.getSynonyms().add("three");
		concepts.add(child);
		
		// Setup the stub
		StubConceptDAO dao = new StubConceptDAO(concepts);
		cache = new ConceptCacheLocalImpl();
		manager = new ConceptManagerImpl(dao, cache, baseURI);

	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testgetUniqueURIPartNull(){
		manager.getUniqueURIPart(null);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void getUniqueURIPartTooShort(){
		manager.getUniqueURIPart(baseURI);
	}
	@Test
	public void getUniqueURIPart(){
		String value = manager.getUniqueURIPart(baseURI+"a");
		assertEquals("a", value);
	}
	
	@Test
	public void testGetAllConcepts() throws DatastoreException, NotFoundException{
		// First with no prefix
		List<ConceptSummary> results = manager.getAllConcepts(parentURI, null);
		assertNotNull(results);
		assertEquals(3, results.size());
		// They should be alphabetical order
		assertEquals(childC, results.get(0));
		assertEquals(childA, results.get(1));
		assertEquals(childB, results.get(2));
		
		// Now add a filter they all share
		results = manager.getAllConcepts(parentURI, "CHILD");
		assertNotNull(results);
		assertEquals(3, results.size());
		// They should be alphabetical order
		assertEquals(childC, results.get(0));
		assertEquals(childA, results.get(1));
		assertEquals(childB, results.get(2));
		
		// Now add a filter they all share
		results = manager.getAllConcepts(parentURI, "t");
		assertNotNull(results);
		assertEquals(3, results.size());
		// They should be alphabetical order
		assertEquals(childC, results.get(0));
		assertEquals(childA, results.get(1));
		assertEquals(childB, results.get(2));
		
		// Only two should share this prefix
		results = manager.getAllConcepts(parentURI, "th");
		assertNotNull(results);
		assertEquals(2, results.size());
		// They should be alphabetical order
		assertEquals(childC, results.get(0));
		assertEquals(childA, results.get(1));
		
		// Only two should share this prefix
		results = manager.getAllConcepts(parentURI, "one");
		assertNotNull(results);
		assertEquals(2, results.size());
		// They should be alphabetical order
		assertEquals(childA, results.get(0));
		assertEquals(childB, results.get(1));

	}
	
	@Test (expected=NotFoundException.class)
	public void testGetConceptNotFound() throws DatastoreException, NotFoundException{
		Concept con = manager.getConcept("fake");
	}

	@Test
	public void testGetConcept() throws DatastoreException, NotFoundException{
		Concept con = manager.getConcept(parentURI);
		assertNotNull(con);
		assertNotNull(con.getSummary());
		assertEquals(parentURI, con.getSummary().getUri());
	}
}
