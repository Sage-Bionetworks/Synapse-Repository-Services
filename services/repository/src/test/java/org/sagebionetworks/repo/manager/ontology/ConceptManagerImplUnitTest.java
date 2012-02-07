package org.sagebionetworks.repo.manager.ontology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptDAO;
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
	StubConceptDAO dao;
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
		dao = new StubConceptDAO(concepts);
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
	
	
	/**
	 * Validate that a cache miss results in data pushed to the cache.
	 */
	@Test
	public void testConceptCacheMiss() throws DatastoreException, NotFoundException{
		ConceptCache mockCache = Mockito.mock(ConceptCache.class);
		ConceptDAO mockDao = Mockito.mock(ConceptDAO.class);
		String conceptUri = "test-uri";
		Concept con = new Concept();
		con.setSummary(new ConceptSummary());
		con.getSummary().setUri(conceptUri);
		con.getSummary().setPreferredLabel("Test Concept");
		// The dao should return this concept.
		when(mockDao.getConceptForUri(conceptUri)).thenReturn(con);
		// This is a cache miss
		when(mockCache.getConcept(conceptUri)).thenReturn(null);
		manager = new ConceptManagerImpl(mockDao, mockCache, baseURI);
		// The first time should hit the dao
		Concept result = manager.getConcept(conceptUri);
		assertEquals(con, result);
		// Now validate that the concept was placed in the cache
		verify(mockCache, times(1)).put(conceptUri, con);
	}
	
	/**
	 * Validate that a cache hit prevents a dao hit.
	 */
	@Test
	public void testConceptCacheHit() throws DatastoreException, NotFoundException{
		ConceptCache mockCache = Mockito.mock(ConceptCache.class);
		ConceptDAO mockDao = Mockito.mock(ConceptDAO.class);
		String conceptUri = "test-uri";
		Concept con = new Concept();
		con.setSummary(new ConceptSummary());
		con.getSummary().setUri(conceptUri);
		con.getSummary().setPreferredLabel("Test Concept");
		// The dao should return this concept.
		when(mockDao.getConceptForUri(conceptUri)).thenThrow(new IllegalStateException("ConceptDao.getConceptForUri() should not have been called because it was in the cache"));
		// This is a cache hit
		when(mockCache.getConcept(conceptUri)).thenReturn(con);
		manager = new ConceptManagerImpl(mockDao, mockCache, baseURI);
		// The first time should hit the dao
		Concept result = manager.getConcept(conceptUri);
		assertEquals(con, result);
		// Now validate that the concept was not put in the cache (it should already be there)
		verify(mockCache, never()).put(conceptUri, con);
	}
	
	/**
	 * Validate that a cache miss results in data pushed to the cache.
	 */
	@Test
	public void testGetAllConceptsCacheMiss() throws DatastoreException, NotFoundException{
		ConceptCache mockCache = Mockito.mock(ConceptCache.class);
		ConceptDAO mockDao = Mockito.mock(ConceptDAO.class);
		String uniquePart = "test-uri";
		String conceptUri = baseURI+uniquePart;
		List<ConceptSummary> list = new ArrayList<ConceptSummary>();
		ConceptSummary summary = new ConceptSummary();
		summary.setUri(conceptUri);
		summary.setPreferredLabel("Test Concept");
		list.add(summary);
		Concept con = new Concept();
		con.setSummary(summary);
		// The dao should return this concept.
		when(mockDao.getAllConcepts(conceptUri)).thenReturn(list);
		// The dao should return this concept.
		when(mockDao.getConceptForUri(conceptUri)).thenReturn(con);
		// This is a cache miss
		when(mockCache.containsKey(uniquePart)).thenReturn(false);
		manager = new ConceptManagerImpl(mockDao, mockCache, baseURI);
		// The first time should hit the dao
		List<ConceptSummary> result = manager.getAllConcepts(conceptUri, null);
//		assertEquals(list, result);
		// Now validate that the concept was placed in the cache
		verify(mockCache, times(1)).putAll((Map<String, List<ConceptSummary>>) any());
	}

	/**
	 * Validate that a cache hit prevents a dao hit.
	 */
	@Test
	public void testGetAllConceptsCacheHit() throws DatastoreException, NotFoundException{
		ConceptCache mockCache = Mockito.mock(ConceptCache.class);
		ConceptDAO mockDao = Mockito.mock(ConceptDAO.class);
		String uniquePart = "test-uri";
		String conceptUri = baseURI+uniquePart;
		List<ConceptSummary> list = new ArrayList<ConceptSummary>();
		ConceptSummary summary = new ConceptSummary();
		summary.setUri(conceptUri);
		summary.setPreferredLabel("Test Concept");
		list.add(summary);
		Concept con = new Concept();
		con.setSummary(summary);
		// The dao should return this concept.
		when(mockDao.getAllConcepts(conceptUri)).thenThrow(new IllegalStateException("ConceptDao.getAllConcepts() should not have been called because it was in the cache"));
		// The dao should return this concept.
		when(mockDao.getConceptForUri(conceptUri)).thenThrow(new IllegalStateException("ConceptDao.getConceptForUri() should not have been called because it was in the cache"));
		// This is a cache miss
		when(mockCache.containsKey(uniquePart)).thenReturn(true);
		manager = new ConceptManagerImpl(mockDao, mockCache, baseURI);
		// The first time should hit the dao
		List<ConceptSummary> result = manager.getAllConcepts(conceptUri, null);
//		assertEquals(list, result);
		// Now validate that the concept was placed in the cache
		verify(mockCache, never()).putAll((Map<String, List<ConceptSummary>>) any());
	}
}
