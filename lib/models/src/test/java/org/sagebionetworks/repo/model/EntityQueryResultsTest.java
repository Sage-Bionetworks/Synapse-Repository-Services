package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sagebionetworks.repo.model.ontology.ConceptSummary;

public class EntityQueryResultsTest {
	
	List<ConceptSummary> fullList;
	
	@Before
	public void before(){
		fullList = new ArrayList<ConceptSummary>();
		int size = 5;
		for(int i=0; i<size; i++){
			ConceptSummary con = new ConceptSummary();
			con.setPreferredLabel(""+i);
			con.setUri("urn:i"+i);
			fullList.add(con);
		}
	}

	@Test
	public void testConstructorFullListMaxLimitZeroOffset(){
		int limit = Integer.MAX_VALUE;
		int offest = 0;
		EntityQueryResults<ConceptSummary> results = new EntityQueryResults<ConceptSummary>(fullList, limit, offest);
		assertEquals(fullList.size(), results.getTotalNumberOfResults());
		assertNotNull(results.getResults());
		assertEquals(fullList.size(), results.getResults().size());
	}
	
	@Test
	public void testConstructorFullListMaxLimitOneOffset(){
		int limit = Integer.MAX_VALUE;
		int offest = 1;
		EntityQueryResults<ConceptSummary> results = new EntityQueryResults<ConceptSummary>(fullList, limit, offest);
		assertEquals(fullList.size(), results.getTotalNumberOfResults());
		assertNotNull(results.getResults());
		int expectedSize = fullList.size()-1;
		assertEquals(expectedSize, results.getResults().size());
		// The first element should be one
		assertNotNull(results.getResults().get(0));
		assertEquals("1", results.getResults().get(0).getPreferredLabel());
		int lastIndex = expectedSize-1;
		assertNotNull(results.getResults().get(lastIndex));
		assertEquals("4", results.getResults().get(lastIndex).getPreferredLabel());
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNullList(){
		EntityQueryResults<ConceptSummary> results = new EntityQueryResults<ConceptSummary>(null, 1, 10);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNegativeLimit(){
		EntityQueryResults<ConceptSummary> results = new EntityQueryResults<ConceptSummary>(fullList, -1, 10);
	}
	
	@Test (expected=IllegalArgumentException.class)
	public void testNegativeOffest(){
		EntityQueryResults<ConceptSummary> results = new EntityQueryResults<ConceptSummary>(fullList, 10, -1);
	}
	
	@Test
	public void testConstructorSubList(){
		int limit = 3;
		int offest = 2;
		EntityQueryResults<ConceptSummary> results = new EntityQueryResults<ConceptSummary>(fullList, limit, offest);
		assertEquals(fullList.size(), results.getTotalNumberOfResults());
		assertNotNull(results.getResults());
		int expectedSize = limit;
		assertEquals(expectedSize, results.getResults().size());
		// The first element should be one
		assertNotNull(results.getResults().get(0));
		assertEquals("2", results.getResults().get(0).getPreferredLabel());
		int lastIndex = expectedSize-1;
		assertNotNull(results.getResults().get(lastIndex));
		assertEquals("4", results.getResults().get(lastIndex).getPreferredLabel());
	}
	
	@Test
	public void testBeyondSize(){
		int limit = 3;
		int offest = 5;
		EntityQueryResults<ConceptSummary> results = new EntityQueryResults<ConceptSummary>(fullList, limit, offest);
		assertEquals(fullList.size(), results.getTotalNumberOfResults());
		assertNotNull(results.getResults());
		int expectedSize = 0;
		assertEquals(expectedSize, results.getResults().size());
	}
	@Test
	public void testLastOne(){
		int limit = 3;
		int offest = 4;
		EntityQueryResults<ConceptSummary> results = new EntityQueryResults<ConceptSummary>(fullList, limit, offest);
		assertEquals(fullList.size(), results.getTotalNumberOfResults());
		assertNotNull(results.getResults());
		int expectedSize = 1;
		assertEquals(expectedSize, results.getResults().size());
		assertNotNull(results.getResults().get(0));
		assertEquals("4", results.getResults().get(0).getPreferredLabel());
	}
}
