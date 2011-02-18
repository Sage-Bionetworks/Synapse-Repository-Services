package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

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
@ContextConfiguration(locations = { "classpath:repository-context.xml",
		"classpath:repository-servlet.xml" })
public class LayerControllerTest {

	private static final Logger log = Logger
			.getLogger(LayerControllerTest.class.getName());
	private Helpers helper = new Helpers();
	private DispatcherServlet servlet;
	private JSONObject dataset;

	private final String SAMPLE_LAYER = "{\"name\":\"DeLiver expression data\", \"type\":\"E\", "
			+ "\"description\": \"foo\", \"releaseNotes\":\"bar\"}";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		servlet = helper.setUp();

		dataset = helper.testCreateJsonEntity("/dataset",
				"{\"name\":\"DeLiver\"}");
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
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#sanityCheckChild(org.springframework.ui.ModelMap)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSanityCheck() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/dataset/123/layer/test");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		// The response should be: {"hello":"REST for Dataset Layers rocks"}
		assertEquals("REST for Dataset Layers rocks", results
				.getString("hello"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#createChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateLayer() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		// Check required properties
		assertEquals("DeLiver expression data", newLayer.getString("name"));

		assertExpectedLayerProperties(newLayer);

		// Get the dataset and make sure our Layer types preview is correct
		JSONObject updatedDataset = helper.testGetJsonEntity(dataset
				.getString("uri"));

		DatasetControllerTest.assertExpectedDatasetProperties(updatedDataset);

		// Get our newly created layer using the layer uri
		JSONObject results = helper.testGetJsonEntities(updatedDataset
				.getString("layer"), null, null, null, null);
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

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		// Get the layer
		JSONObject results = helper
				.testGetJsonEntity(newLayer.getString("uri"));

		assertEquals(newLayer.getString("id"), results.getString("id"));
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

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		// Get the layer
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));

		assertEquals(newLayer.getString("id"), layer.getString("id"));
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

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		helper.testDeleteJsonEntity(newLayer.getString("uri"));
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
				.testCreateJsonEntity(
						dataset.getString("layer"),
						"{\"name\":\"DeLiver genetic data\", \"type\":\"G\", "
								+ " \"description\": \"foo\", \"releaseNotes\":\"bar\"}");
		helper.testCreateJsonEntity(dataset.getString("layer"), SAMPLE_LAYER);
		helper
				.testCreateJsonEntity(
						dataset.getString("layer"),
						"{\"name\":\"DeLiver clinical data\", \"type\":\"C\", "
								+ " \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		JSONObject results = helper.testGetJsonEntities("/dataset/"
				+ dataset.getString("id") + "/layer", null, null, null, null);
		assertEquals(3, results.getInt("totalNumberOfResults"));
		assertEquals(3, results.getJSONArray("results").length());
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.PREVIOUS_PAGE_FIELD));
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.NEXT_PAGE_FIELD));

		assertExpectedLayersProperties(results.getJSONArray("results"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#updateChildEntityAnnotations}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateLayerAnnotations() throws Exception {
		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		helper.testEntityAnnotations(newLayer.getString("annotations"));
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

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		// Get the layer
		JSONObject layerPreview = helper.testGetJsonEntity(newLayer
				.getString("preview"));

		assertEquals(newLayer.getString("id"), layerPreview.getString("id"));
		assertEquals("", layerPreview.getString("preview"));

		// Modify that layer
		layerPreview.put("preview", "this is an updated preview of a layer");
		JSONObject updatedLayerPreview = helper
				.testUpdateJsonEntity(layerPreview);

		// Check that the update response reflects the change
		assertEquals("this is an updated preview of a layer",
				updatedLayerPreview.getString("preview"));

		// Now make sure the stored one reflects the change too
		JSONObject storedLayerPreview = helper.testGetJsonEntity(newLayer
				.getString("preview"));
		assertEquals("this is an updated preview of a layer",
				storedLayerPreview.getString("preview"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerLocationsController#updateDependentEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateLayerLocations() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		// Get the Locations
		JSONObject layerLocations = helper.testGetJsonEntity(newLayer
				.getJSONArray("locations").getString(0));
		assertExpectedLayerLocationsProperties(layerLocations);
		assertEquals(newLayer.getString("id"), layerLocations.getString("id"));
		JSONArray locations = layerLocations.getJSONArray("locations");
		assertEquals(0, locations.length());

		// Modify the locations
		locations
				.put(new JSONObject(
						"{\"type\":\"awss3\",\"path\":\"human_liver_cohort/expression/expression.txt\"}"));
		locations.put(new JSONObject(
				"{\"type\":\"awsebs\", \"path\":\"snap-29d33a42 (US West)\"}"));
		locations
				.put(new JSONObject(
						"{\"type\":\"sage\", \"path\":\"smb://fremont/C$/external-data/DAT_001__TCGA_Glioblastoma/Mar2010/tcga_glioblastoma_data.tar.gz\"}"));
		JSONObject updatedLayerLocations = helper
				.testUpdateJsonEntity(layerLocations);
		assertExpectedLayerLocationsProperties(updatedLayerLocations);

		// Check that the update response reflects the change
		assertEquals(3, updatedLayerLocations.getJSONArray("locations")
				.length());

		// Now make sure the stored one reflects the change too
		JSONObject storedLayerLocations = helper.testGetJsonEntity(newLayer
				.getJSONArray("locations").getString(0));
		assertEquals(3, storedLayerLocations.getJSONArray("locations").length());
		assertExpectedLayerLocationsProperties(storedLayerLocations);
		
		// As a sanity check, make sure we can walk from one end to the other
		JSONObject saneDataset = helper.testGetJsonEntity(dataset.getString("uri"));
		JSONObject saneLayers = helper.testGetJsonEntities(saneDataset.getString("layer"), null, null, null, null);
		JSONObject saneLayer = helper.testGetJsonEntity(saneLayers.getJSONArray("results").getJSONObject(0).getString("uri"));
		helper.testGetJsonObject(saneLayer.getJSONArray("locations").getString(0));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerLocationsController#getS3Location}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetS3Location() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		// Get the Locations
		JSONObject layerLocations = helper.testGetJsonEntity(newLayer
				.getJSONArray("locations").getString(0));

		assertEquals(newLayer.getString("id"), layerLocations.getString("id"));
		JSONArray locations = layerLocations.getJSONArray("locations");
		assertEquals(0, locations.length());

		// Modify the locations
		locations
				.put(new JSONObject(
						"{\"type\":\"awss3\",\"path\":\"human_liver_cohort/expression/expression.txt\"}"));
		locations.put(new JSONObject(
				"{\"type\":\"awsebs\", \"path\":\"snap-29d33a42 (US West)\"}"));
		locations
				.put(new JSONObject(
						"{\"type\":\"sage\", \"path\":\"smb://fremont/C$/external-data/DAT_001__TCGA_Glioblastoma/Mar2010/tcga_glioblastoma_data.tar.gz\"}"));
		helper.testUpdateJsonEntity(layerLocations);

		// Get the layer
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));
		assertExpectedLayerProperties(layer);

		// Get the location
		JSONObject location = null;
		for (int i = 0; i < layer.getJSONArray("locations").length(); i++) {
			String locationUri = layer.getJSONArray("locations").getString(i);
			if (locationUri.endsWith(UrlHelpers.S3_LOCATIONSUFFIX)) {
				location = helper.testGetJsonObject(locationUri);
				break;
			}
		}

		assertTrue(location
				.getString("path")
				.matches(
						"^https://data01.sagebase.org.s3.amazonaws.com/[^?]+\\?Expires=\\d+&AWSAccessKeyId=\\w+&Signature=.+$"));

	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerLocationsController#getEbsLocation}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetEBSLocation() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		// Get the Locations
		JSONObject layerLocations = helper.testGetJsonEntity(newLayer
				.getJSONArray("locations").getString(0));

		assertEquals(newLayer.getString("id"), layerLocations.getString("id"));
		JSONArray locations = layerLocations.getJSONArray("locations");
		assertEquals(0, locations.length());

		// Modify the locations
		locations
				.put(new JSONObject(
						"{\"type\":\"awss3\",\"path\":\"human_liver_cohort/expression/expression.txt\"}"));
		locations.put(new JSONObject(
				"{\"type\":\"awsebs\", \"path\":\"snap-29d33a42 (US West)\"}"));
		locations
				.put(new JSONObject(
						"{\"type\":\"sage\", \"path\":\"smb://fremont/C$/external-data/DAT_001__TCGA_Glioblastoma/Mar2010/tcga_glioblastoma_data.tar.gz\"}"));
		helper.testUpdateJsonEntity(layerLocations);

		// Get the layer
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));
		assertExpectedLayerProperties(layer);

		// Get the location
		JSONObject location = null;
		for (int i = 0; i < layer.getJSONArray("locations").length(); i++) {
			String locationUri = layer.getJSONArray("locations").getString(i);
			if (locationUri.endsWith(UrlHelpers.EBS_LOCATIONSUFFIX)) {
				location = helper.testGetJsonObject(locationUri);
				break;
			}
		}

		assertEquals("snap-29d33a42 (US West)", location.getString("path"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerLocationsController#getSageLocation}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetSageLocation() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		// Get the Locations
		JSONObject layerLocations = helper.testGetJsonEntity(newLayer
				.getJSONArray("locations").getString(0));

		assertEquals(newLayer.getString("id"), layerLocations.getString("id"));
		JSONArray locations = layerLocations.getJSONArray("locations");
		assertEquals(0, locations.length());

		// Modify the locations
		locations
				.put(new JSONObject(
						"{\"type\":\"awss3\",\"path\":\"human_liver_cohort/expression/expression.txt\"}"));
		locations.put(new JSONObject(
				"{\"type\":\"awsebs\", \"path\":\"snap-29d33a42 (US West)\"}"));
		locations
				.put(new JSONObject(
						"{\"type\":\"sage\", \"path\":\"smb://fremont/C$/external-data/DAT_001__TCGA_Glioblastoma/Mar2010/tcga_glioblastoma_data.tar.gz\"}"));
		helper.testUpdateJsonEntity(layerLocations);

		// Get the layer
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));
		assertExpectedLayerProperties(layer);

		// Get the location
		JSONObject location = null;
		for (int i = 0; i < layer.getJSONArray("locations").length(); i++) {
			String locationUri = layer.getJSONArray("locations").getString(i);
			if (locationUri.endsWith(UrlHelpers.SAGE_LOCATIONSUFFIX)) {
				location = helper.testGetJsonObject(locationUri);
				break;
			}
		}

		assertEquals(
				"smb://fremont/C$/external-data/DAT_001__TCGA_Glioblastoma/Mar2010/tcga_glioblastoma_data.tar.gz",
				location.getString("path"));

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
				.testCreateJsonEntityShouldFail(
						dataset.getString("layer"),
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
		assertTrue(reason.matches("(?s).*\"BOGUS\".*"));
		assertTrue(reason.matches("(?s).*not marked as ignorable.*"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#createChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInvalidLocationModelCreateLayer() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		// Get the Locations
		JSONObject layerLocations = helper.testGetJsonEntity(newLayer
				.getJSONArray("locations").getString(0));
		JSONArray locations = layerLocations.getJSONArray("locations");

		// Modify the locations
		locations
				.put(new JSONObject(
						"{\"type\":\"awss3\",\"path\":\"human_liver_cohort/expression/expression.txt\"}"));
		locations.put(new JSONObject(
				"{\"type\":\"awsebs\", \"path\":\"snap-29d33a42 (US West)\"}"));
		locations
				.put(new JSONObject(
						"{\"type\":\"ThisShouldFail\", \"path\":\"smb://fremont/C$/external-data/DAT_001__TCGA_Glioblastoma/Mar2010/tcga_glioblastoma_data.tar.gz\"}"));
		JSONObject error = helper.testUpdateJsonEntityShouldFail(
				layerLocations, HttpStatus.BAD_REQUEST);

		String reason = error.getString("reason");
		assertEquals("'type' must be one of: awss3 awsebs sage", reason);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#createChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMissingRequiredFieldCreateLayer() throws Exception {

		JSONObject error = helper
				.testCreateJsonEntityShouldFail(
						dataset.getString("layer"),
						"{\"version\": \"1.0.0\", \"description\": \"foo\", \"releaseNotes\":\"bar\"}",
						HttpStatus.BAD_REQUEST);

		assertEquals("'name' is a required property for InputDataLayer", error
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
	public void testMissingRequiredFieldUpdateLayer() throws Exception {

		// Create a layer
		JSONObject newLayer = helper
				.testCreateJsonEntity(
						dataset.getString("layer"),
						"{\"name\":\"MouseCross clinical data\", \"type\":\"C\", "
								+ " \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		// Get that layer
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));
		assertEquals(newLayer.getString("id"), layer.getString("id"));
		assertEquals("MouseCross clinical data", layer.getString("name"));

		// Modify that layer to make it invalid
		layer.remove("name");
		JSONObject error = helper.testUpdateJsonEntityShouldFail(layer,
				HttpStatus.BAD_REQUEST);

		assertEquals("'name' is a required property for InputDataLayer", error
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
				.testCreateJsonEntity(
						dataset.getString("layer"),
						"{\"name\":\"MouseCross genetic data\", \"type\":\"C\", "
								+ " \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		// Get that layer
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));
		assertEquals(newLayer.getString("id"), layer.getString("id"));
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
				.matches("entity with id .* was updated since you last fetched it, retrieve it again and reapply the update"));
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
		JSONObject results = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testGetJsonEntityShouldFail(results
				.getString("uri"), HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerController#updateChildEntityAnnotations}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetNonExistentLayerAnnotations() throws Exception {

		// Load up a layer
		JSONObject newLayer = helper
				.testCreateJsonEntity(
						"/dataset/" + dataset.getString("id") + "/layer",
						"{\"name\":\"MouseCross\", \"type\":\"C\", "
								+ " \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		// Get our empty annotations container
		JSONObject annotations = helper.testGetJsonEntity(newLayer
				.getString("annotations"));

		// Delete our layer
		helper.testDeleteJsonEntity(newLayer.getString("uri"));

		JSONObject error = helper.testGetJsonEntityShouldFail(annotations
				.getString("uri"), HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));

	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerLocationsController#getS3Location}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetNonExistentLayerLocation() throws Exception {
		// Load up a layer
		JSONObject newLayer = helper
				.testCreateJsonEntity(
						dataset.getString("layer"),
						"{\"name\":\"DeLiver expression data\", \"type\":\"C\", "
								+ "\"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		JSONObject error = helper.testGetJsonEntityShouldFail(newLayer
				.getString("uri")
				+ "/awsEBSLocation", HttpStatus.NOT_FOUND);
		assertTrue(error.getString("reason").startsWith(
				"No AWS EBS location exists for layer"));

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
		JSONObject results = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

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
		JSONObject results = helper.testCreateJsonEntity(dataset
				.getString("layer"), SAMPLE_LAYER);

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
		assertTrue(results.has("preview"));
		assertTrue(results.getString("preview").endsWith("/preview"));
		assertTrue(results.has("creationDate"));
		assertFalse("null".equals(results.getString("creationDate")));

		// Check that optional properties that receive default values
		assertTrue(results.has("version"));
		assertFalse("null".equals(results.getString("version")));

		// Check that other properties are present, even if their value is null
		JSONArray locations = results.getJSONArray("locations");
		assertNotNull(locations);
		for (int i = 0; i < locations.length(); i++) {
			String location = locations.getString(i);
			assertTrue(location
					.matches("/dataset/[^/]+/layer/[^/]+/(locations|.*Location)$"));
		}
	}

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
			assertFalse("null".equals(location.getString("type")));
			assertFalse("null".equals(location.getString("path")));
		}
	}

}
