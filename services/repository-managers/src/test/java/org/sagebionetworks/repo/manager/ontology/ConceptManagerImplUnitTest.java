package org.sagebionetworks.repo.manager.ontology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.QueryResults;
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
	
	String parentUniquePart;
	String parentURI;
	Concept childA;
	Concept childB;
	Concept childC;
	
	@Before
	public void before(){
		// The base URI
		baseURI = "http://example.com/";
		
		List<Concept> concepts = new ArrayList<Concept>();
		
		// The parent
		Concept parentA = new Concept();
		parentA.setPreferredLabel("Parent A");
		parentUniquePart = "parentA";
		parentURI = baseURI+parentUniquePart;
		parentA.setUri(parentURI);
		parentA.setSynonyms(new ArrayList<String>());
		parentA.getSynonyms().add("Frank");
		concepts.add(parentA);
		
		// Add some children
		Concept child = new Concept();
		childA = child;
		child.setPreferredLabel("Child A3");
		child.setUri(baseURI+"childA");
		child.setParent(parentA.getUri());
		child.setSynonyms(new ArrayList<String>());
		child.getSynonyms().add("one");
		child.getSynonyms().add("three");
		concepts.add(child);
		
		// Add some children
		child = new Concept();
		childB = child;
		child.setPreferredLabel("Child A4");
		child.setUri(baseURI+"childB");
		child.setParent(parentA.getUri());
		child.setSynonyms(new ArrayList<String>());
		child.getSynonyms().add("one");
		child.getSynonyms().add("two");
		concepts.add(child);
		
		child = new Concept();
		childC = child;
		child.setPreferredLabel("Child A");
		child.setUri(baseURI+"childC");
		child.setParent(parentA.getUri());
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
		QueryResults<Concept> paged = manager.getChildConcepts(parentURI, null, Integer.MAX_VALUE, 0);
		List<Concept> results = paged.getResults();
		assertNotNull(results);
		assertEquals(3, results.size());
		// They should be alphabetical order
		assertEquals(childC, results.get(0));
		assertEquals(childA, results.get(1));
		assertEquals(childB, results.get(2));
		
		// Now add a filter they all share
		paged = manager.getChildConcepts(parentURI,  "CHILD", Integer.MAX_VALUE, 0);
		results = paged.getResults();
		assertNotNull(results);
		assertEquals(3, results.size());
		// They should be alphabetical order
		assertEquals(childC, results.get(0));
		assertEquals(childA, results.get(1));
		assertEquals(childB, results.get(2));
		
		// Now add a filter they all share
		paged = manager.getChildConcepts(parentURI,  "t", Integer.MAX_VALUE, 0);
		results = paged.getResults();
		assertNotNull(results);
		assertEquals(3, results.size());
		// They should be alphabetical order
		assertEquals(childC, results.get(0));
		assertEquals(childA, results.get(1));
		assertEquals(childB, results.get(2));
		
		// Only two should share this prefix
		paged = manager.getChildConcepts(parentURI,  "th", Integer.MAX_VALUE, 0);
		results = paged.getResults();
		assertNotNull(results);
		assertEquals(2, results.size());
		// They should be alphabetical order
		assertEquals(childC, results.get(0));
		assertEquals(childA, results.get(1));
		
		// Only two should share this prefix
		paged = manager.getChildConcepts(parentURI,  "one", Integer.MAX_VALUE, 0);
		results = paged.getResults();
		assertNotNull(results);
		assertEquals(2, results.size());
		// They should be alphabetical order
		assertEquals(childA, results.get(0));
		assertEquals(childB, results.get(1));
		
		// nothing has this prefix
		paged = manager.getChildConcepts(parentURI,  "does not exist", Integer.MAX_VALUE, 0);
		results = paged.getResults();
		assertNotNull(results);
		assertEquals(0, results.size());

	}
	
	@Test (expected=NotFoundException.class)
	public void testGetConceptNotFound() throws DatastoreException, NotFoundException{
		Concept con = manager.getConcept("fake");
	}

	@Test
	public void testGetConcept() throws DatastoreException, NotFoundException{
		Concept con = manager.getConcept(parentURI);
		assertNotNull(con);
		assertEquals(parentURI, con.getUri());
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
		con.setUri(conceptUri);
		con.setPreferredLabel("Test Concept");
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
		con.setUri(conceptUri);
		con.setPreferredLabel("Test Concept");
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
		con.setUri(summary.getUri());
		con.setPreferredLabel(summary.getPreferredLabel());
		// The dao should return this concept.
		when(mockDao.getAllConcepts(parentURI)).thenReturn(list);
		// The dao should return this concept.
		when(mockDao.getConceptForUri(conceptUri)).thenReturn(con);
		// This is a cache miss
		when(mockCache.containsKey(parentUniquePart)).thenReturn(false);
		manager = new ConceptManagerImpl(mockDao, mockCache, baseURI);
		// The first time should hit the dao
		QueryResults<Concept> paged = manager.getChildConcepts(parentURI, null, Integer.MAX_VALUE, 0);
		List<Concept> results = paged.getResults();
//		assertEquals(list, result);
		// Now validate that the concept was placed in the cache
		verify(mockCache, times(1)).putAll((Map<String, List<Concept>>) any());
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
//		Concept con = new Concept();
//		con.setUri(conceptUri);
//		con.setPreferredLabel("Test Concept");
		// The dao should return this concept.
		when(mockDao.getAllConcepts(parentURI)).thenThrow(new IllegalStateException("ConceptDao.getAllConcepts() should not have been called because it was in the cache"));
		// The dao should return this concept.
		when(mockDao.getConceptForUri(conceptUri)).thenThrow(new IllegalStateException("ConceptDao.getConceptForUri() should not have been called because it was in the cache"));
		// This is a cache hit
		when(mockCache.containsKey(parentUniquePart)).thenReturn(true);
		manager = new ConceptManagerImpl(mockDao, mockCache, baseURI);
		// The first time should hit the dao
		QueryResults<Concept> paged = manager.getChildConcepts(parentURI, null, Integer.MAX_VALUE, 0);
		List<Concept> results = paged.getResults();
//		assertEquals(list, result);
		// Now validate that the concept was placed in the cache
		verify(mockCache, never()).putAll((Map<String, List<Concept>>) any());
	}
}
