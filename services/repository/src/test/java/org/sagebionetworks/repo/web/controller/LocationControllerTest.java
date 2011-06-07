package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit tests for the Location CRUD operations exposed by the LocationController
 * with JSON request and response encoding.
 * <p>
 * 
 * Note that test logic and assertions common to operations for all DAO-backed
 * entities can be found in the Helpers class. What follows are test cases that
 * make use of that generic test logic with some assertions specific to
 * locations.
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class LocationControllerTest {

	/**
	 * Sample location for use in unit tests, note that the parentId property is
	 * missing and necessary to be a valid location
	 */
	public static String SAMPLE_LOCATION = "{\"path\":\"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz\", "
			+ "\"type\":\"awss3\", \"md5sum\":\"33183779e53ce0cfc35f59cc2a762cbd\"}";

	@Autowired
	private Helpers helper;
	private JSONObject dataset;
	private JSONObject layer;
	private JSONObject datasetLocation;
	private JSONObject layerLocation;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();

		dataset = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/dataset", DatasetControllerTest.SAMPLE_DATASET);

		layer = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/layer", LayerControllerTest.getSampleLayer(dataset.getString("id")));

		datasetLocation = new JSONObject(SAMPLE_LOCATION).put("parentId",
				dataset.getString("id"));
		layerLocation = new JSONObject(SAMPLE_LOCATION).put("parentId", layer
				.getString("id"));
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
	 * @throws Exception
	 */
	@Test
	public void testCreateLocation() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", datasetLocation.toString());

		// Check properties
		assertEquals("awss3", results.getString("type"));
		assertEquals("33183779e53ce0cfc35f59cc2a762cbd", results
				.getString("md5sum"));

		String s3key = "/" + results.getString("parentId") + "/"
				+ results.getString("id") + "/"
				// TODO + results.getString("version") + "/"
				+ "unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz";

		assertTrue(0 < results.getString("path").indexOf(s3key));
		assertTrue(results
				.getString("path")
				.matches(
						"^https://s3.amazonaws.com/data01.sagebase.org"
								+ s3key
								+ "\\?.*Expires=\\d+&AWSAccessKeyId=\\w+&Signature=.+$"));

		assertExpectedLocationProperties(results);

		JSONObject storedDataset = helper.testGetJsonEntity(dataset
				.getString("uri"));
		assertNotNull(storedDataset);

		// TODO fix this part of the test to just ensure that the dataset has
		// the correct number of location children
		// JSONObject datasetLocations = helper.testGetJsonEntity("/dataset/"
		// + dataset.getString("id") + "/locations");
		// assertNotNull(datasetLocations);
		// assertEquals(1, datasetLocations.getJSONArray("locations").length());

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testGetLocation() throws Exception {
		JSONObject newLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", layerLocation.toString());

		JSONObject results = helper.testGetJsonEntity(newLocation
				.getString("uri"));

		assertEquals(newLocation.getString("id"), results.getString("id"));
		assertEquals("awss3", results.getString("type"));
		assertEquals("33183779e53ce0cfc35f59cc2a762cbd", results
				.getString("md5sum"));

		String s3key = "/" + results.getString("parentId") + "/"
				+ results.getString("id") + "/"
				// TODO + results.getString("version") + "/"
				+ "unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz";

		assertTrue(0 < results.getString("path").indexOf(s3key));
		// https://s3.amazonaws.com/data01.sagebase.org/1/3/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz?Expires=1307507371&AWSAccessKeyId=thisIsAFakeAWSAccessId&Signature=op01bZcni6bPw1sEmpjci75PIoE%3D
		assertTrue(results.getString("path").matches(
				"^https://s3.amazonaws.com/data01.sagebase.org" + s3key
						+ "\\?.*Expires=\\d+&AWSAccessKeyId=\\w+&Signature=.+$"));

		assertExpectedLocationProperties(results);

		JSONObject storedLayer = helper.testGetJsonEntity(layer
				.getString("uri"));
		assertNotNull(storedLayer);

		// TODO fix this part of the test to just ensure that the layer has the
		// correct number of location children
		JSONObject paging = helper.testGetJsonEntities(layer.getString("locations"));
		assertNotNull(paging);
		assertEquals(1, paging.getLong("totalNumberOfResults"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testUpdateLocation() throws Exception {
		JSONObject newLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", datasetLocation.toString());

		// Get one location
		JSONObject location = helper.testGetJsonEntity(newLocation
				.getString("uri"));
		assertEquals(newLocation.getString("id"), location.getString("id"));
		assertTrue(0 < location.getString("path").indexOf(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz"));

		// Modify that location
		location.put("path",
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.zip");
		JSONObject updatedLocation = helper.testUpdateJsonEntity(location);
		assertExpectedLocationProperties(updatedLocation);

		// Check that the update response reflects the change
		assertTrue(0 < updatedLocation.getString("path").indexOf(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.zip"));

		// Now make sure the stored one reflects the change too
		JSONObject storedLocation = helper.testGetJsonEntity(newLocation
				.getString("uri"));
		assertTrue(0 < storedLocation.getString("path").indexOf(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.zip"));

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testUpdateNewlyCreatedLocation() throws Exception {
		JSONObject newLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", layerLocation.toString());

		// Modify the newly created location
		newLocation.put("path",
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.zip");
		JSONObject updatedLocation = helper.testUpdateJsonEntity(newLocation);
		assertExpectedLocationProperties(updatedLocation);

		// Check that the update response reflects the change
		assertTrue(0 < updatedLocation.getString("path").indexOf(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.zip"));

		// Now make sure the stored one reflects the change too
		JSONObject storedLocation = helper.testGetJsonEntity(newLocation
				.getString("uri"));
		assertTrue(0 < storedLocation.getString("path").indexOf(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.zip"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDeleteLocation() throws Exception {
		JSONObject newLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", datasetLocation.toString());
		helper.testDeleteJsonEntity(newLocation.getString("uri"));
	}

	/*****************************************************************************************************
	 * Bad parameters tests
	 */

	/**
	 * @throws Exception
	 */
	@Test
	public void testInvalidModelCreateLocation() throws Exception {

		JSONObject error = helper
				.testCreateJsonEntityShouldFail(
						helper.getServletPrefix() + "/location",
						"{\"path\": \"123/456/foo.txt\", \"BOGUS\":\"this does not match our model object\"}",
						HttpStatus.BAD_REQUEST);

		// The response should be something like: {"reason":"Unrecognized field
		// \"BOGUS\"
		// (Class org.sagebionetworks.repo.model.Location), not marked as
		// ignorable\n at
		// [Source:
		// org.springframework.mock.web.DelegatingServletInputStream@2501e081;
		// line: 1, column: 19]"}

		String reason = error.getString("reason");
		assertTrue(reason.matches("(?s).*\"BOGUS\".*"));
		assertTrue(reason.matches("(?s).*not marked as ignorable.*"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testMissingParentIdCreateLocation() throws Exception {

		JSONObject error = helper.testCreateJsonEntityShouldFail(helper
				.getServletPrefix()
				+ "/location", SAMPLE_LOCATION, HttpStatus.BAD_REQUEST);

		assertEquals("parentId cannot be null", error.getString("reason"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testMissingParentIdUpdateLocation() throws Exception {
		// Create a location
		JSONObject newLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", layerLocation.toString());

		// Get that location
		JSONObject location = helper.testGetJsonEntity(newLocation
				.getString("uri"));
		assertEquals(newLocation.getString("id"), location.getString("id"));
		assertEquals("awss3", location.getString("type"));

		// Modify that location to make it invalid
		location.remove("parentId");
		JSONObject error = helper.testUpdateJsonEntityShouldFail(location,
				HttpStatus.BAD_REQUEST);

		assertEquals("parentId cannot be null", error.getString("reason"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testMissingRequiredFieldCreateLocation() throws Exception {

		JSONObject error = helper
				.testCreateJsonEntityShouldFail(
						helper.getServletPrefix() + "/location",
						"{\"parentId\":\"0\", \"type\":\"awss3\", \"md5sum\":\"33183779e53ce0cfc35f59cc2a762cbd\"}",
						HttpStatus.BAD_REQUEST);

		assertEquals("path cannot be null", error.getString("reason"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testMissingRequiredFieldUpdateLocation() throws Exception {
		// Create a location
		JSONObject newLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", layerLocation.toString());

		// Get that location
		JSONObject location = helper.testGetJsonEntity(newLocation
				.getString("uri"));
		assertEquals(newLocation.getString("id"), location.getString("id"));
		assertEquals("awss3", location.getString("type"));

		// Modify that location to make it invalid
		location.remove("type");
		JSONObject error = helper.testUpdateJsonEntityShouldFail(location,
				HttpStatus.BAD_REQUEST);

		assertEquals("type cannot be null", error.getString("reason"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testUpdateLocationConflict() throws Exception {
		// Create a location
		JSONObject newLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", datasetLocation.toString());
		// Get that location
		JSONObject location = helper.testGetJsonEntity(newLocation
				.getString("uri"));
		assertEquals(newLocation.getString("id"), location.getString("id"));
		assertTrue(0 < location.getString("path").indexOf(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz"));

		// Modify that location
		location.put("path",
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.zip");
		JSONObject updatedLocation = helper.testUpdateJsonEntity(location);
		assertTrue(0 < updatedLocation.getString("path").indexOf(
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.zip"));

		// Modify the location we got earlier a second time
		location.put("path",
				"unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.bz2");
		JSONObject error = helper.testUpdateJsonEntityShouldFail(location,
				HttpStatus.PRECONDITION_FAILED);

		String reason = error.getString("reason");
		System.out.println(reason);
		assertTrue(reason
				.matches("Node: .* was updated since you last fetched it, retrieve it again and reapply the update"));
	}

	/*****************************************************************************************************
	 * Not Found Tests
	 */

	/**
	 * @throws Exception
	 */
	@Test
	public void testGetNonExistentLocation() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", layerLocation.toString());

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testGetJsonEntityShouldFail(results
				.getString("uri"), HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testUpdateNonExistentLocation() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", datasetLocation.toString());

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testUpdateJsonEntityShouldFail(results,
				HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testDeleteNonExistentLocation() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", layerLocation.toString());

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testDeleteJsonEntityShouldFail(results
				.getString("uri"), HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/*****************************************************************************************************
	 * Location-specific helpers
	 */

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedLocationProperties(JSONObject results)
			throws Exception {
		// Check required properties
		assertTrue(results.has("type"));
		assertFalse("null".equals(results.getString("type")));
		assertTrue(results.has("path"));
		assertFalse("null".equals(results.getString("path")));
		assertTrue(results.has("md5sum"));
		assertFalse("null".equals(results.getString("md5sum")));
		assertTrue(results.has("parentId"));
		assertFalse("null".equals(results.getString("parentId")));
	}
}
