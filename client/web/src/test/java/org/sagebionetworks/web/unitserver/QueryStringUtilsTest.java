package org.sagebionetworks.web.unitserver;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;
import org.sagebionetworks.web.server.servlet.QueryStringUtils;
import org.sagebionetworks.web.shared.SearchParameters;
import org.sagebionetworks.web.shared.WhereCondition;
import org.sagebionetworks.web.shared.WhereCondition.Operator;

public class QueryStringUtilsTest {
	
	private static String ROOT_URL = "http://localhost:9998";
	
	@Test
	public void testRoundTrip() throws URISyntaxException{
		// Create a simple query
		SearchParameters params = new SearchParameters();
		params.setFromType(SearchParameters.FromType.dataset.name());
		// Now create the query string for this object
		URI uri = QueryStringUtils.writeQueryUri(ROOT_URL, params);
		assertNotNull(uri);
		System.out.println(uri);
		// Now make a copy from the string
		SearchParameters copy = QueryStringUtils.parseQueryString(uri.toString());
		assertNotNull(copy);
		// It should match the original
		assertEquals(params, copy);
	}
	
	@Test
	public void testRoundTripPaging() throws URISyntaxException{
		// Create a simple query
		SearchParameters params = new SearchParameters();
		params.setLimit(20);
		params.setOffset(50);
		params.setFromType(SearchParameters.FromType.dataset.name());
		// Now create the query string for this object
		URI uri = QueryStringUtils.writeQueryUri(ROOT_URL, params);
		assertNotNull(uri);
		System.out.println(uri);
		// Now make a copy from the string
		SearchParameters copy = QueryStringUtils.parseQueryString(uri.toString());
		assertNotNull(copy);
		// It should match the original
		assertEquals(params, copy);
	}
	
	@Test
	public void testRoundTripPagingAndSorting() throws URISyntaxException{
		// Create a simple query
		SearchParameters params = new SearchParameters();
		params.setLimit(100);
		params.setOffset(31);
		params.setFromType(SearchParameters.FromType.dataset.name());
		params.setAscending(false);
		params.setSort("sortKey");
		// Now create the query string for this object
		URI uri = QueryStringUtils.writeQueryUri(ROOT_URL, params);
		assertNotNull(uri);
		System.out.println(uri);
		// Now make a copy from the string
		SearchParameters copy = QueryStringUtils.parseQueryString(uri.toString());
		assertNotNull(copy);
		// It should match the original
		assertEquals(params, copy);
	}
	
	@Test
	public void testRoundTripZeroOffest() throws URISyntaxException{
		// Create a simple query
		SearchParameters params = new SearchParameters();
		params.setLimit(100);
		params.setOffset(0);
		params.setFromType(SearchParameters.FromType.dataset.name());
		params.setAscending(true);
		params.setSort("sortKey");
		// Now create the query string for this object
		URI uri = QueryStringUtils.writeQueryUri(ROOT_URL, params);
		assertNotNull(uri);
		System.out.println(uri);
		// Now make a copy from the string
		SearchParameters copy = QueryStringUtils.parseQueryString(uri.toString());
		assertNotNull(copy);
		// The offset should have been changed to 1
		assertEquals(1, copy.getOffset());
	}
	
	@Test
	public void testRoundTripWhere() throws URISyntaxException{
		// Create a simple query
		SearchParameters params = new SearchParameters();
		params.setLimit(100);
		params.setOffset(0);
		params.setFromType(SearchParameters.FromType.dataset.name());
		params.setAscending(true);
		params.setSort("sortKey");
		WhereCondition where = new WhereCondition("someId", Operator.EQUALS, "123");
		params.setWhere(where);
		// Now create the query string for this object
		URI uri = QueryStringUtils.writeQueryUri(ROOT_URL, params);
		assertNotNull(uri);
		System.out.println(uri);
		// Now make a copy from the string
		SearchParameters copy = QueryStringUtils.parseQueryString(uri.toString());
		assertNotNull(copy);
		// The offset should have been changed to 1
		assertEquals(1, copy.getOffset());
	}


}
