package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.controller.metadata.LocationMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Unit tests for the Layer CRUD operations exposed by the LayerController with
 * JSON request and response encoding.
 * <p>
 * 
 * Note that test logic and assertions common to operations for all DAO-backed
 * entities can be found in the Helpers class. What follows are test cases that
 * make use of that generic test logic with some assertions specific to layers.
 * <p>
 * 
 * TODO refactor me, this file is too long
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class LayerControllerTest {

	@Autowired
	private Helpers helper;
	private JSONObject project;
	private JSONObject dataset;

	/**
	 * Some properties for a layer to use for unit tests
	 */
	private final static String SAMPLE_LAYER_1 = "{\"name\":\"DeLiver expression data\", \"type\":\"E\", "
			+ "\"description\": \"foo\", \"releaseNotes\":\"bar\", \"parentId\":\"";
	
	/**
	 * Build a sample layer.
	 * @param parentId
	 * @return
	 */
	public static String getSampleLayer(String parentId){
		StringBuilder builder = new StringBuilder();
		builder.append(SAMPLE_LAYER_1);
		builder.append(parentId);
		builder.append("\"}");
		return builder.toString();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();

		project = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/project", DatasetControllerTest.SAMPLE_PROJECT);

		dataset = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/dataset", DatasetControllerTest.getSampleDataset(project.getString("id")));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	/*************************************************************************************************************************
	 * Happy case tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#createChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateLayer() throws Exception {

		// Sanity check - make sure we can find our dataset
		JSONObject allDatasets = helper.testGetJsonEntities(helper
				.getServletPrefix()
				+ "/dataset", null, null, null, null);
		assertEquals(1, allDatasets.getInt("totalNumberOfResults"));

		JSONObject newLayer = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/layer", LayerControllerTest.getSampleLayer(dataset.getString(NodeConstants.COL_ID)));
		assertExpectedLayerProperties(newLayer);
		// Check required properties
		assertEquals("DeLiver expression data", newLayer.getString("name"));

		// Sanity check - make sure we can _STILL_ find our dataset
		allDatasets = helper.testGetJsonEntities(helper.getServletPrefix()
				+ "/dataset", null, null, null, null);
		assertEquals(1, allDatasets.getInt("totalNumberOfResults"));

		// Get the dataset and make sure our Layer types preview is correct
		JSONObject updatedDataset = helper.testGetJsonEntity(dataset
				.getString("uri"));

		DatasetControllerTest.assertExpectedDatasetProperties(updatedDataset);

		// Get our newly created layer using the layer uri
		JSONObject results = helper.testGetJsonEntities(updatedDataset
				.getString("layers"), null, null, null, null);
		assertExpectedLayersProperties(results.getJSONArray("results"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#getChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetLayer() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/layer", LayerControllerTest.getSampleLayer(dataset.getString(NodeConstants.COL_ID)));

		// Get the layer
		Map<String,String> extraParams = new HashMap<String, String>();
		extraParams.put(ServiceConstants.METHOD_PARAM, RequestMethod.HEAD.name());
		JSONObject results = helper
				.testGetJsonEntity(newLayer.getString("uri"), extraParams);

		assertEquals(newLayer.getString(NodeConstants.COL_ID), results.getString(NodeConstants.COL_ID));
		assertEquals("DeLiver expression data", results.getString("name"));

		assertExpectedLayerProperties(results);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#updateChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateLayer() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/layer", LayerControllerTest.getSampleLayer(dataset.getString(NodeConstants.COL_ID)));
		// Get the layer
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));

		assertEquals(newLayer.getString(NodeConstants.COL_ID), layer.getString(NodeConstants.COL_ID));
		assertEquals("DeLiver expression data", layer.getString("name"));

		// Modify that layer
		layer.put("name", "DeLiver clinical data");
		layer.put("type", "C");
		JSONObject updatedLayer = helper.testUpdateJsonEntity(layer);
		assertExpectedLayerProperties(updatedLayer);

		// Check that the update response reflects the change
		assertEquals("DeLiver clinical data", updatedLayer.getString("name"));
		assertEquals("C", updatedLayer.getString("type"));

		// Now make sure the stored one reflects the change too
		JSONObject storedLayer = helper.testGetJsonEntity(newLayer
				.getString("uri"));
		assertEquals("DeLiver clinical data", storedLayer.getString("name"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#deleteChildEntity}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteLayer() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/layer", LayerControllerTest.getSampleLayer(dataset.getString(NodeConstants.COL_ID)));

		try {
			helper.testDeleteJsonEntity(newLayer.getString("uri"));
		} catch (Exception e) {
			throw new Exception("Exception deleting "+newLayer.getString("uri"), e);
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#getChildEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetLayers() throws Exception {

		helper
				.testCreateJsonEntity(helper.getServletPrefix()
						+"/layer",
						"{\"name\":\"DeLiver genetic data\", \"type\":\"G\", "
								+ " \"description\": \"foo\", \"releaseNotes\":\"bar\", \"parentId\":\""+dataset.getString(NodeConstants.COL_ID)+"\"}");
		helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/layer", LayerControllerTest.getSampleLayer(dataset.getString(NodeConstants.COL_ID)));
		helper
				.testCreateJsonEntity(helper.getServletPrefix()
						+"/layer",
						"{\"name\":\"DeLiver clinical data\", \"type\":\"C\", "
								+ " \"description\": \"foo\", \"releaseNotes\":\"bar\", \"parentId\":\""+dataset.getString(NodeConstants.COL_ID)+"\"}");

		JSONObject results = helper.testGetJsonEntities(helper
				.getServletPrefix()
				+ "/dataset/" + dataset.getString(NodeConstants.COL_ID) + "/layer", null, null,
				null, null);
		assertEquals(3, results.getInt("totalNumberOfResults"));
		assertEquals(3, results.getJSONArray("results").length());
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.PREVIOUS_PAGE_FIELD));
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.NEXT_PAGE_FIELD));

		assertExpectedLayersProperties(results.getJSONArray("results"));
	}

	/*****************************************************************************************************
	 * Bad parameters tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#createChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInvalidModelCreateLayer() throws Exception {

		JSONObject error = helper
				.testCreateJsonEntityShouldFail(helper.getServletPrefix()
						+"/layer",
						"{\"name\": \"DeLiver expression data\",  \"type\":\"C\", "
								+ "\"BOGUS\":\"this does not match our model object\"}",
						HttpStatus.BAD_REQUEST);

		// The response should be something like: {"reason":"Unrecognized field
		// \"BOGUS\"
		// (Class org.sagebionetworks.repo.model.Layer), not marked as
		// ignorable\n at
		// [Source:
		// org.springframework.mock.web.DelegatingServletInputStream@2501e081;
		// line: 1, column: 19]"}

		String reason = error.getString("reason");
		assertTrue(reason,reason.matches("(?s).*BOGUS.*"));
		assertTrue(reason, reason.matches("(?s).*is not defined in the schema.*"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#createChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore // if the name is null then the id will be used.
	@Test
	public void testMissingRequiredFieldCreateLayer() throws Exception {

		JSONObject error = helper
				.testCreateJsonEntityShouldFail(helper.getServletPrefix()
						+"/layer",
						"{\"version\": \"1.0.0\", \"description\": \"foo\", \"releaseNotes\":\"bar\", \"type\":\"C\", \"parentId\":\""+dataset.getString("id")+"\"}",
						HttpStatus.BAD_REQUEST);

		assertEquals("Node.name cannot be null", error
				.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#updateChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testMissingRequiredFieldUpdateLayer() throws Exception {

		// Create a layer
		JSONObject newLayer = helper
				.testCreateJsonEntity(helper.getServletPrefix()
						+"/layer",
						"{\"name\":\"MouseCross clinical data\", \"type\":\"C\", "
								+ " \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		// Get that layer
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));
		assertEquals(newLayer.getString(NodeConstants.COL_ID), layer.getString(NodeConstants.COL_ID));
		assertEquals("MouseCross clinical data", layer.getString("name"));

		// Modify that layer to make it invalid
		layer.remove("name");
		JSONObject error = helper.testUpdateJsonEntityShouldFail(layer,
				HttpStatus.BAD_REQUEST);

		assertEquals("'name' is a required property for Layer", error
				.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#updateChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateLayerConflict() throws Exception {
		// Create a layer
		JSONObject newLayer = helper
				.testCreateJsonEntity(helper.getServletPrefix()
						+"/layer",
						"{\"name\":\"MouseCross genetic data\", \"type\":\"C\", "
								+ " \"description\": \"foo\", \"releaseNotes\":\"bar\", \"parentId\":\"" +dataset.getString(NodeConstants.COL_ID)+
										"\"}");

		// Get that layer
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));
		assertEquals(newLayer.getString(NodeConstants.COL_ID), layer.getString(NodeConstants.COL_ID));
		assertEquals("MouseCross genetic data", layer.getString("name"));

		// Modify that layer
		layer.put("name", "MouseX genetic data");
		JSONObject updatedLayer = helper.testUpdateJsonEntity(layer);
		assertEquals("MouseX genetic data", updatedLayer.getString("name"));

		// Modify the layer we got earlier a second time
		layer.put("name", "CONFLICT MouseX genetic data");
		JSONObject error = helper.testUpdateJsonEntityShouldFail(layer,
				HttpStatus.PRECONDITION_FAILED);

		String reason = error.getString("reason");
		assertTrue(reason
				.matches("Node: .* was updated since you last fetched it, retrieve it again and reapply the update"));
	}

	/*****************************************************************************************************
	 * Not Found Tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#getChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetNonExistentLayer() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/layer", LayerControllerTest.getSampleLayer(dataset.getString(NodeConstants.COL_ID)));

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testGetJsonEntityShouldFail(results
				.getString("uri"), HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#updateChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateNonExistentLayer() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/layer", LayerControllerTest.getSampleLayer(dataset.getString(NodeConstants.COL_ID)));
		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testUpdateJsonEntityShouldFail(results,
				HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#deleteChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteNonExistentLayer() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/layer", LayerControllerTest.getSampleLayer(dataset.getString(NodeConstants.COL_ID)));

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testDeleteJsonEntityShouldFail(results
				.getString("uri"), HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/*****************************************************************************************************
	 * Layer-specific helpers
	 */

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedLayersProperties(JSONArray results)
			throws Exception {
		for (int i = 0; i < results.length(); i++) {
			JSONObject layer = results.getJSONObject(i);
			assertExpectedLayerProperties(layer);
		}
	}

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedLayerProperties(JSONObject results)
			throws Exception {
		// Check required properties
		assertTrue(results.has("name"));
		assertFalse("null".equals(results.getString("name")));
		assertTrue(results.has("type"));
		assertFalse("null".equals(results.getString("type")));

		// Check immutable system-defined properties
		assertTrue(results.has("annotations"));
		assertTrue(results.getString("annotations").endsWith("/annotations"));
		assertTrue(results.has("previews"));
		assertTrue(results.getString("previews").endsWith("/preview"));
		assertTrue(results.has("createdOn"));
		assertFalse("null".equals(results.getString("createdOn")));

		// Check that optional properties that receive default values
		assertTrue(results.has("version"));
		String value = results.getString("version");
		assertFalse("null".equals(value));

		// Check that other properties are present, even if their value is null
		String locations = results.getString("locations");
		assertNotNull(locations);
//		for (int i = 0; i < locations.length(); i++) {
//			String location = locations.getString(i);
//			assertTrue(location
//					.matches(".*/dataset/[^/]+/layer/[^/]+/(locations|.*Location)$"));
//		}
	}

}
