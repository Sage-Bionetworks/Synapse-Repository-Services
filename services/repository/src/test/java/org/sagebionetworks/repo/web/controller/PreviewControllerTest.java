package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class PreviewControllerTest {

	@Autowired
	private Helpers helper;
	private JSONObject dataset;
	private JSONObject project;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		helper.setUp();
		// Datasets must have a project as a parent
		project = helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
				DatasetControllerTest.SAMPLE_PROJECT);

		dataset = helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
				DatasetControllerTest.getSampleDataset(project.getString("id")));
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
	 * {@link org.sagebionetworks.repo.web.controller.LayerPreviewController#updateDependentEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateLayerPreviewAsMap() throws Exception {

		String tabDelimitedSnippet = "Patient_ID	AGE_(YRS)	GENDER	self_reported_ethnicity	inferred_population	WEIGHT_(KG)\n"
				+ "2220047	28	Male	W		81.6466266\n"
				+ "2220074	13	Male	W	Cauc	79.83225712\n"
				+ "2220061	67	Male	W	Cauc	80.73944186\n"
				+ "2220035	7	Female	W		24.94758035\n"
				+ "2220071	57	Female	W	Cauc	64.86370891\n";

		JSONObject newLayer = helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
				LayerControllerTest.getSampleLayer(dataset.getString("id")));
		
		// Create an empty preview for this layer.
		String prviewString = "{\"entityType\":\"org.sagebionetworks.repo.model.Preview\", \"parentId\":\""+newLayer.getString("id")+"\"}";
		JSONObject layerPreview = helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
				prviewString);
		
		assertEquals(newLayer.getString("id"), layerPreview.getString(NodeConstants.COL_PARENT_ID));
		assertTrue(layerPreview.isNull("previewString"));

		// Modify that layer
		layerPreview.put("previewString", tabDelimitedSnippet);

		JSONObject updatedLayerPreview = helper
				.testUpdateJsonEntity(layerPreview);

		// Check that the update response reflects the change
		assertEquals(tabDelimitedSnippet, updatedLayerPreview.getString("previewString"));

		// Now make sure the stored one reflects the change too
		// TODO do we want to leave a breadcrumb for this uri?
		JSONObject layerPreviewMap = helper.testGetJsonObject(newLayer.getString("previews"));
		assertNotNull(layerPreviewMap.getInt("totalNumberOfResults"));
		assertNotNull(layerPreviewMap.getJSONArray("results"));
		assertEquals(1, layerPreviewMap.getJSONArray("results").length());
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerPreviewController#updateDependentEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateLayerPreview() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
				LayerControllerTest.getSampleLayer(dataset.getString("id")));

		// Create an empty preview for this layer.
		String prviewString = "{\"entityType\":\"org.sagebionetworks.repo.model.Preview\", \"parentId\":\""+newLayer.getString("id")+"\"}";
		JSONObject layerPreview = helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
				prviewString);

		assertEquals(newLayer.getString("id"), layerPreview.getString(NodeConstants.COL_PARENT_ID));
		assertTrue(layerPreview.isNull("previewString"));

		// Modify that layer
		layerPreview.put("previewString", "this is an updated preview of a layer");
		JSONObject updatedLayerPreview = helper
				.testUpdateJsonEntity(layerPreview);

		// Check that the update response reflects the change
		assertEquals("this is an updated preview of a layer",
				updatedLayerPreview.getString("previewString"));

		// Now make sure the stored one reflects the change too
		JSONObject storedLayerPreview = helper.testGetJsonEntity(layerPreview.getString("uri"));
		assertEquals("this is an updated preview of a layer",
				storedLayerPreview.getString("previewString"));
	}

	/*****************************************************************************************************
	 * Layer-specific helpers
	 */

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedLayerLocationsProperties(JSONObject results)
			throws Exception {

		// Check that other properties are present, even if their value is null
		JSONArray locations = results.getJSONArray("locations");
		assertNotNull(locations);
		for (int i = 0; i < locations.length(); i++) {
			JSONObject location = locations.getJSONObject(i);
			assertExpectedLayerLocationProperties(location);
		}
	}

	public static void assertExpectedLayerLocationProperties(
			JSONObject location) throws JSONException {
		assertFalse("null".equals(location.getString("type")));
		assertFalse("null".equals(location.getString("path")));
	}
	
	

}
