package org.sagebionetworks.search.controller;

import static org.junit.Assert.assertEquals;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.conn.ConnectTimeoutException;
import org.junit.Test;
import org.sagebionetworks.repo.model.error.ErrorResponse;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author deflaux
 *
 */
public class BaseControllerTest {

	/**
	 * 
	 */
	@Test
	public void testCSExceptionScrubbing() {

		BaseController controller = new SearchController();
		HttpServletRequest request = new MockHttpServletRequest();
		ConnectTimeoutException ex = new ConnectTimeoutException(
				"Connect to search-prod-20120206-vigj35bjslyimyxftqh4mludxm.us-east-1.cloudsearch.amazonaws.com:80 timed out");
		// org.apache.http.conn.ConnectTimeoutException: Connect to
		// search-prod-20120206-vigj35bjslyimyxftqh4mludxm.us-east-1.cloudsearch.amazonaws.com:80
		// timed out
		ErrorResponse response = controller.handleException(ex, request);
		assertEquals("ConnectTimeoutException while connecting to the search index.", response.getReason());
	}
	
}
