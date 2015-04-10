package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;

import java.sql.BatchUpdateException;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.sagebionetworks.repo.model.ErrorResponse;
import org.sagebionetworks.utils.HttpClientHelperException;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author deflaux
 *
 */
public class BaseControllerTest {


	@Test
	public void testDeadlockError(){
		EntityController controller = new EntityController();
		HttpServletRequest request = new MockHttpServletRequest();
		ErrorResponse response = controller.handleTransientDataAccessExceptions(new DeadlockLoserDataAccessException("Message",
				new BatchUpdateException()), request);
		assertEquals(BaseController.SERVICE_TEMPORARILY_UNAVAIABLE_PLEASE_TRY_AGAIN_LATER, response.getReason());
	}
	
	@Test
	public void testTransientError() {
		EntityController controller = new EntityController();
		HttpServletRequest request = new MockHttpServletRequest();
		ErrorResponse response = controller.handleTransientDataAccessExceptions(new TransientDataAccessException("Message",
				new BatchUpdateException()) {
			private static final long serialVersionUID = 1L;
		}, request);
		assertEquals(BaseController.SERVICE_TEMPORARILY_UNAVAIABLE_PLEASE_TRY_AGAIN_LATER, response.getReason());
	}

	/**
	 * 
	 */
	@Test
	public void testCSExceptionScrubbing() {

		SearchController controller = new SearchController();
		HttpServletRequest request = new MockHttpServletRequest();
		HttpClientHelperException ex = new HttpClientHelperException(
				"Connect to search-prod-20120206-vigj35bjslyimyxftqh4mludxm.us-east-1.cloudsearch.amazonaws.com:80 timed out");
		// org.apache.http.conn.ConnectTimeoutException: Connect to
		// search-prod-20120206-vigj35bjslyimyxftqh4mludxm.us-east-1.cloudsearch.amazonaws.com:80
		// timed out
		ErrorResponse response = controller.handleException(ex, request, true);
		assertEquals("search failed, try again", response.getReason());
	}

}
