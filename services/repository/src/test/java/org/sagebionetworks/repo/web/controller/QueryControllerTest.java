/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

			// Add a layer
			helper.testCreateJsonEntity(newDataset.getString("layer"),
					LayerControllerTest.SAMPLE_LAYER);
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
		JSONObject queryResult = helper.testQuery("select * from dataset");
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
		JSONObject queryResult = helper
				.testQuery("select * from dataset order by \"name\" limit 10");
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
		JSONObject queryResult = helper
				.testQuery("select * from dataset order by \"name\" desc");
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

		JSONObject queryResult = helper
				.testQuery("select * from dataset where name == \"Pediatric AML TARGET\"");
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
	public void testLayerQuery() throws Exception {
		int numLayersExpected = 1;

		JSONObject datasetResults = helper
				.testQuery("select * from dataset where name == \"Pediatric AML TARGET\"");
		String datasetId = datasetResults.getJSONArray("results")
				.getJSONObject(0).getString("id");

		JSONObject queryResult = helper
				.testQuery("select * from layer where dataset.id == \""
						+ datasetId + "\"");
		assertEquals(numLayersExpected, queryResult
				.getInt("totalNumberOfResults"));
		JSONArray results = queryResult.getJSONArray("results");
		assertEquals(numLayersExpected, results.length());

		// Check that it is a list of maps
		for (int i = 0; i < numLayersExpected; i++) {
			JSONObject layer = results.getJSONObject(i);
			assertTrue(layer.has("type"));
			assertFalse("null".equals(layer.getString("type")));
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLayerQueryMissingDatasetId() throws Exception {
		JSONObject error = helper.testQueryShouldFail(
				"select * from layer where foo == \"bar\"",
				HttpStatus.BAD_REQUEST);
		assertEquals("Layer queries must include a 'WHERE dataset.id == <the id>' clause", error.getString("reason"));
		error = helper.testQueryShouldFail("select * from layer",
				HttpStatus.BAD_REQUEST);
		assertEquals("Layer queries must include a 'WHERE dataset.id == <the id>' clause", error.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testTokenMgrError() throws Exception {
		JSONObject error = helper.testQueryShouldFail(
				"select * from dataset where name == \"Pediatric AML TARGET",
				HttpStatus.BAD_REQUEST);
		assertEquals(
				"TokenMgrError: Lexical error at line 1, column 58.  Encountered: <EOF> after : \"\\\"Pediatric AML TARGET\"",
				error.getString("reason"));
	}

}
