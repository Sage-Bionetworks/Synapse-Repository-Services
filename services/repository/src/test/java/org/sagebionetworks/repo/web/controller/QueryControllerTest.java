/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author deflaux
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class QueryControllerTest {

	@Autowired
	private Helpers helper;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();

		// Load up a few datasets with annotations
		for (int i = 0; i < DatasetsControllerTest.SAMPLE_DATASET_NAMES.length; i++) {
			String status = (0 == (i % 2)) ? "pending" : "complete";
			JSONObject newDataset = helper.testCreateJsonEntity(helper
					.getServletPrefix()
					+ "/dataset", "{\"name\":\""
					+ DatasetsControllerTest.SAMPLE_DATASET_NAMES[i]
					+ "\", \"status\":\"" + status + "\"}");

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
		assertExpectedQueryResultProperties("dataset", queryResult);

		assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES.length,
				queryResult.getInt("totalNumberOfResults"));
		JSONArray results = queryResult.getJSONArray("results");
		assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES.length,
				results.length());

		// Check that it is a list of maps
		for (int i = 0; i < DatasetsControllerTest.SAMPLE_DATASET_NAMES.length; i++) {
			JSONObject result = results.getJSONObject(i);
			assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES[i], result
					.getString("dataset.name"));
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
		assertExpectedQueryResultProperties("dataset", queryResult);

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
					.getString("dataset.name"));
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

		// This test case also has the "dataset." prefix on the column name
		JSONObject queryResult = helper
				.testQuery("select * from dataset order by dataset.\"name\" desc");
		assertExpectedQueryResultProperties("dataset", queryResult);

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
							- 1 - i], result.getString("dataset.name"));
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
				.testQuery("select * from dataset where dataset.name == \"Pediatric AML TARGET\"");
		assertExpectedQueryResultProperties("dataset", queryResult);

		assertEquals(1, queryResult.getInt("totalNumberOfResults"));
		JSONArray results = queryResult.getJSONArray("results");
		assertEquals(1, results.length());

		// Check that it is a list of one map
		JSONObject result = results.getJSONObject(0);
		assertEquals("Pediatric AML TARGET", result.getString("dataset.name"));
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

		// This test case also checks that we are URL decoding the queries
		JSONObject datasetResults = helper
				.testQuery("select+*+from+dataset+where+name+%3d%3d+%22Pediatric+AML+TARGET%22");
		assertExpectedQueryResultProperties("dataset", datasetResults);

		String datasetId = datasetResults.getJSONArray("results")
				.getJSONObject(0).getString("dataset.id");

		JSONObject queryResult = helper
				.testQuery("select * from layer where dataset.parentId == \""
						+ datasetId + "\"");
		assertExpectedQueryResultProperties("layer", queryResult);

		assertEquals(numLayersExpected, queryResult
				.getInt("totalNumberOfResults"));
		JSONArray results = queryResult.getJSONArray("results");
		assertEquals(numLayersExpected, results.length());

		// Check that it is a list of maps
		for (int i = 0; i < numLayersExpected; i++) {
			JSONObject layer = results.getJSONObject(i);
			assertTrue(layer.has("layer.type"));
			assertFalse("null".equals(layer.getString("layer.type")));
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSingleQuoteStringLiteral() throws Exception {
		JSONObject queryResult = helper
				.testQuery("select * from dataset where dataset.name == 'Pediatric AML TARGET'");
		assertExpectedQueryResultProperties("dataset", queryResult);
		assertEquals(1, queryResult.getInt("totalNumberOfResults"));
		JSONArray results = queryResult.getJSONArray("results");
		assertEquals(1, results.length());
		// Check that it is a list of one map
		JSONObject result = results.getJSONObject(0);
		assertEquals("Pediatric AML TARGET", result.getString("dataset.name"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query} .
	 * 
	 * @throws Exception
	 */
	@Ignore // This is no longer true
	@Test
	public void testLayerQueryMissingDatasetId() throws Exception {
		JSONObject error = helper.testQueryShouldFail(
				"select * from layer where name == \"bar\"",
				HttpStatus.BAD_REQUEST);
		assertEquals(
				"java.lang.IllegalArgumentException: Layer queries must include a 'WHERE dataset.id == <the id>' clause",
				error.getString("reason"));
		error = helper.testQueryShouldFail("select * from layer",
				HttpStatus.BAD_REQUEST);
		assertEquals(
				"java.lang.IllegalArgumentException: Layer queries must include a 'WHERE dataset.id == <the id>' clause",
				error.getString("reason"));
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

	/*****************************************************************************************************
	 * Query API-specific helpers
	 */

	/**
	 * @param tableName
	 * @param queryResult
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static void assertExpectedQueryResultProperties(String tableName,
			JSONObject queryResult) throws Exception {

		JSONArray results = queryResult.getJSONArray("results");
		for (int i = 0; i < results.length(); i++) {
			JSONObject result = results.getJSONObject(i);
			Iterator<String> iter = result.keys();
			while (iter.hasNext()) {
				String key = iter.next();
				assertTrue(key.startsWith(tableName + "."));
			}
		}
	}
}
