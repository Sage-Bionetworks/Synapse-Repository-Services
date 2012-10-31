package org.sagebionetworks.repo.model;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sagebionetworks.repo.model.ontology.ConceptSummary;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;

public class QueryResultsTest {

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
		QueryResults<ConceptSummary> results = new QueryResults<ConceptSummary>(fullList, limit, offest);
		assertEquals(fullList.size(), results.getTotalNumberOfResults());
		assertNotNull(results.getResults());
		assertEquals(fullList.size(), results.getResults().size());
	}

	@Test
	public void testConstructorFullListMaxLimitOneOffset(){
		int limit = Integer.MAX_VALUE;
		int offest = 1;
		QueryResults<ConceptSummary> results = new QueryResults<ConceptSummary>(fullList, limit, offest);
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
		QueryResults<ConceptSummary> results = new QueryResults<ConceptSummary>(null, 1, 10);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testNegativeLimit(){
		QueryResults<ConceptSummary> results = new QueryResults<ConceptSummary>(fullList, -1, 10);
	}

	@Test (expected=IllegalArgumentException.class)
	public void testNegativeOffest(){
		QueryResults<ConceptSummary> results = new QueryResults<ConceptSummary>(fullList, 10, -1);
	}

	@Test
	public void testConstructorSubList(){
		int limit = 3;
		int offest = 2;
		QueryResults<ConceptSummary> results = new QueryResults<ConceptSummary>(fullList, limit, offest);
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
		QueryResults<ConceptSummary> results = new QueryResults<ConceptSummary>(fullList, limit, offest);
		assertEquals(fullList.size(), results.getTotalNumberOfResults());
		assertNotNull(results.getResults());
		int expectedSize = 0;
		assertEquals(expectedSize, results.getResults().size());
	}
	@Test
	public void testLastOne(){
		int limit = 3;
		int offest = 4;
		QueryResults<ConceptSummary> results = new QueryResults<ConceptSummary>(fullList, limit, offest);
		assertEquals(fullList.size(), results.getTotalNumberOfResults());
		assertNotNull(results.getResults());
		int expectedSize = 1;
		assertEquals(expectedSize, results.getResults().size());
		assertNotNull(results.getResults().get(0));
		assertEquals("4", results.getResults().get(0).getPreferredLabel());
	}

	@Ignore
	@Test
	public void testRoundTrip() throws JSONObjectAdapterException{
		// Create a query result with all types of data

//		List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
//		int count = 4;
//		for(int i=0; i<count; i++){
//			Map<String, Object> row = new HashMap<String, Object>();
//			row.put("string key", "String value:"+i);
//			row.put("long key", new Long(2*i));
//			row.put("double key", new Double(4*i+1.33));
//			row.put("int key", new Integer(i));
//			row.put("null key", null);
//			row.put("boolean key", (i % 2) >0);
////			row.put("key array", new String[]{"0ne", "two", "three"});
//			List<String> list = new ArrayList<String>();
//			list.add("a");
//			list.add("b");
//			list.add("c");
//			row.put("key List", list);
//			rows.add(row);
//		}
//		QueryResults result = new QueryResults(rows, 100);
//		// Make the round trip
//		JSONObjectAdapter adapter = new JSONObjectAdapterImpl();
//		result.writeToJSONObject(adapter);
//		QueryResults clone = new QueryResults();
//		clone.initializeFromJSONObject(adapter);
//		assertEquals(result, clone);
	}

}
