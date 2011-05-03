package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit tests for the Dataset Query operations exposed by the QueryController
 * with XML request and response encoding.
 * <p>
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class QueryControllerXMLTest {

	private static final Logger log = Logger
			.getLogger(QueryControllerXMLTest.class.getName());

	@Autowired
	private Helpers helper;
	private HttpServlet servlet = null;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		servlet = helper.setUp();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	/*****************************************************************************************
	 * Happy Case Tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQueryDatasets() throws Exception {
		// Load up a few datasets
		createDatasetHelper();
		createDatasetHelper();

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/xml");
		request.setRequestURI(helper.getServletPrefix() + "/query");
		request.addParameter("query", "select+*+from+dataset");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), response.getStatus());
		assertTrue(response
				.getContentAsString()
				.equals(
						"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><errorResponse><reason>Could not find acceptable representation</reason></errorResponse>"));

		/*
		 * TODO if we want to get this working, we need to modify the
		 * QueryResults object
		 * http://stackoverflow.com/questions/298733/java-util
		 * -list-is-an-interface-and-jaxb-cant-handle-interfaces
		 */

	}

	private String createDatasetHelper() throws Exception {
		JSONObject dataset = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/dataset", "{\"name\":\"dataset from a unit test\"}");
		return dataset.getString("id");
	}
}
