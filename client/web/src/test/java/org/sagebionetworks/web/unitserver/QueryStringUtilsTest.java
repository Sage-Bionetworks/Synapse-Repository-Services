package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sagebionetworks.web.server.servlet.QueryStringUtils;
import org.sagebionetworks.web.shared.SearchParameters;

public class QueryStringUtilsTest {
	
	@Test
	public void testRoundTrip(){
		// Create a simple query
		SearchParameters params = new SearchParameters();
		params.setFromType(SearchParameters.FromType.dataset.name());
		// Now create the query string for this object
		String queryString = QueryStringUtils.writeQueryString(params);
		assertNotNull(queryString);
		System.out.println(queryString);
		// Now make a copy from the string
		SearchParameters copy = QueryStringUtils.parseQueryString(queryString);
		assertNotNull(copy);
		// It should match the original
		assertEquals(params, copy);
	}
	
	@Test
	public void testRoundTripPaging(){
		// Create a simple query
		SearchParameters params = new SearchParameters();
		params.setLimit(20);
		params.setOffset(50);
		params.setFromType(SearchParameters.FromType.dataset.name());
		// Now create the query string for this object
		String queryString = QueryStringUtils.writeQueryString(params);
		assertNotNull(queryString);
		System.out.println(queryString);
		// Now make a copy from the string
		SearchParameters copy = QueryStringUtils.parseQueryString(queryString);
		assertNotNull(copy);
		// It should match the original
		assertEquals(params, copy);
	}
	
	@Test
	public void testRoundTripPagingAndSorting(){
		// Create a simple query
		SearchParameters params = new SearchParameters();
		params.setLimit(100);
		params.setOffset(31);
		params.setFromType(SearchParameters.FromType.dataset.name());
		params.setAscending(false);
		params.setSort("sortKey");
		// Now create the query string for this object
		String queryString = QueryStringUtils.writeQueryString(params);
		assertNotNull(queryString);
		System.out.println(queryString);
		// Now make a copy from the string
		SearchParameters copy = QueryStringUtils.parseQueryString(queryString);
		assertNotNull(copy);
		// It should match the original
		assertEquals(params, copy);
	}

}
