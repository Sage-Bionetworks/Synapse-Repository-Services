/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author deflaux
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:repository-context.xml",
		"classpath:repository-servlet.xml" })
public class QueryControllerTest {

	private static final Logger log = Logger
			.getLogger(LayerControllerTest.class.getName());
	private Helpers helper = new Helpers();
	private DispatcherServlet servlet;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		servlet = helper.setUp();

		// Load up a few datasets with annotations
		for (int i = 0; i < DatasetsControllerTest.SAMPLE_DATASET_NAMES.length; i++) {
			JSONObject newDataset = helper.testCreateJsonEntity("/dataset",
					"{\"name\":\""
							+ DatasetsControllerTest.SAMPLE_DATASET_NAMES[i]
							+ "\"}");

			// Add some canned annotations to our dataset
			helper.testEntityAnnotations(newDataset.getString("annotations"));
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQuery() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/query");
		request.addParameter("query", "select * from dataset");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject queryResult = new JSONObject(response.getContentAsString());
		assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES.length,
				queryResult.getInt("totalNumberOfResults"));
		JSONArray results = queryResult.getJSONArray("results");
		assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES.length,
				results.length());

		// Check that it is a list of maps
		for (int i = 0; i < DatasetsControllerTest.SAMPLE_DATASET_NAMES.length; i++) {
			JSONObject result = results.getJSONObject(i);
			assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES[i], result
					.getString("name"));
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSortQuery() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/query");
		request.addParameter("query",
				"select * from dataset order by \"name\" limit 10");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject queryResult = new JSONObject(response.getContentAsString());
		assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES.length,
				queryResult.getInt("totalNumberOfResults"));
		JSONArray results = queryResult.getJSONArray("results");
		assertEquals(10, results.length());

		List<String> sortedDatasetNames = Arrays
				.asList(DatasetsControllerTest.SAMPLE_DATASET_NAMES);
		Collections.sort(sortedDatasetNames);

		// Check that it is a list of maps
		for (int i = 0; i < 10; i++) {
			JSONObject result = results.getJSONObject(i);
			assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES[i], result
					.getString("name"));
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSortQueryDescending() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/query");
		request.addParameter("query",
				"select * from dataset order by \"name\" desc");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject queryResult = new JSONObject(response.getContentAsString());
		assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES.length,
				queryResult.getInt("totalNumberOfResults"));
		JSONArray results = queryResult.getJSONArray("results");
		assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES.length,
				results.length());

		List<String> sortedDatasetNames = Arrays
				.asList(DatasetsControllerTest.SAMPLE_DATASET_NAMES);
		Collections.sort(sortedDatasetNames);

		// Check that it is a list of maps
		for (int i = 0; i < DatasetsControllerTest.SAMPLE_DATASET_NAMES.length; i++) {
			JSONObject result = results.getJSONObject(i);
			assertEquals(
					DatasetsControllerTest.SAMPLE_DATASET_NAMES[DatasetsControllerTest.SAMPLE_DATASET_NAMES.length
							- 1 - i], result.getString("name"));
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWhereQuery() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/query");
		request.addParameter("query",
				"select * from dataset where name == \"Pediatric AML TARGET\"");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject queryResult = new JSONObject(response.getContentAsString());
		// TODO fix me, this should be 1
		assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES.length,
				queryResult.getInt("totalNumberOfResults"));
		JSONArray results = queryResult.getJSONArray("results");
		assertEquals(1, results.length());

		// Check that it is a list of one map
		JSONObject result = results.getJSONObject(0);
		assertEquals("Pediatric AML TARGET", result.getString("name"));
	}
	
	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTokenMgrError() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/query");
		request.addParameter("query",
				"select * from dataset where name == \"Pediatric AML TARGET");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatus());
		JSONObject error = new JSONObject(response.getContentAsString());
		assertEquals("TokenMgrError: Lexical error at line 1, column 58.  Encountered: <EOF> after : \"\\\"Pediatric AML TARGET\"", error.getString("reason"));
	}
}
