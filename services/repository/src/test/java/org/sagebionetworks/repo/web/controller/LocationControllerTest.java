package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.StackConfiguration;
import org.sagebionetworks.repo.model.Location;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMethod;

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

	public static String SAMPLE_EXTERNAL_LOCATION = "{\"path\":\"http://tcga-data.nci.nih.gov/tcgafiles/ftp_auth/distro_ftpusers/anonymous/tumor/coad/cgcc/unc.edu/agilentg4502a_07_3/transcriptome/unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz\", "
			+ "\"type\":\"external\", \"md5sum\":\"33183779e53ce0cfc35f59cc2a762cbd\"}";

	@Autowired
	private Helpers helper;
	private JSONObject project;
	private JSONObject dataset;
	private JSONObject layer;
	private JSONObject datasetS3Location;
	private JSONObject layerS3Location;
	private JSONObject datasetExternalLocation;
	private JSONObject layerExternalLocation;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();
		
		// Datasets must have a project as a parent
		project = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/project", DatasetControllerTest.SAMPLE_PROJECT);

		dataset = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/dataset", DatasetControllerTest.getSampleDataset(project.getString("id")));

		layer = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/layer", LayerControllerTest.getSampleLayer(dataset
				.getString("id")));

		datasetS3Location = new JSONObject(SAMPLE_LOCATION).put(
				NodeConstants.COL_PARENT_ID, dataset.getString("id"));
		layerS3Location = new JSONObject(SAMPLE_LOCATION).put(
				NodeConstants.COL_PARENT_ID, layer.getString("id"));
		datasetExternalLocation = new JSONObject(SAMPLE_EXTERNAL_LOCATION).put(
				NodeConstants.COL_PARENT_ID, dataset.getString("id"));
		layerExternalLocation = new JSONObject(SAMPLE_EXTERNAL_LOCATION).put(
				NodeConstants.COL_PARENT_ID, layer.getString("id"));
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
		JSONObject createdLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", datasetS3Location.toString());

		// Check properties
		assertEquals("awss3", createdLocation.getString("type"));
		assertEquals("33183779e53ce0cfc35f59cc2a762cbd", createdLocation
				.getString("md5sum"));

		String s3key = "/"
			+ createdLocation.getString("id") + "/"
			+ createdLocation.getString("versionLabel") + "/"
			+ "unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.tar.gz";

		assertTrue(0 < createdLocation.getString("path").indexOf(s3key));
		assertTrue(createdLocation
				.getString("path")
				.matches(
						"^https://s3.amazonaws.com/"
								+ StackConfiguration.getS3Bucket()
								+ s3key
								+ "\\?.*Expires=\\d+&AWSAccessKeyId=\\w+&Signature=.+$"));

		assertExpectedLocationProperties(createdLocation);

		// Just change the md5sum to simulate an updated file
		JSONObject storedLocation = helper.testGetJsonEntity(createdLocation.getString("uri"));
		storedLocation.put("md5sum", "99983779e53ce0cfc35f59cc2a762999");
		JSONObject updatedLocation = helper.testUpdateJsonEntity(storedLocation);
		
		// Check the properties again
		assertEquals("awss3", updatedLocation.getString("type"));
		assertEquals("99983779e53ce0cfc35f59cc2a762999", updatedLocation
				.getString("md5sum"));
		assertTrue(0 < updatedLocation.getString("path").indexOf(s3key));
		assertTrue(updatedLocation
				.getString("path")
				.matches(
						"^https://s3.amazonaws.com/"
								+ StackConfiguration.getS3Bucket()
								+ s3key
								+ "\\?.*Expires=\\d+&AWSAccessKeyId=\\w+&Signature=[^/]+$"));
		assertExpectedLocationProperties(updatedLocation);

		// Ensure we have the correct number of locations under this dataset
		JSONObject storedDataset = helper.testGetJsonEntity(dataset.getString("uri"));
		assertNotNull(storedDataset);
		JSONObject datasetLocations = helper.testGetJsonEntities(storedDataset.getString("locations"));
		assertNotNull(datasetLocations);
		assertEquals(1, datasetLocations.getJSONArray("results").length());

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testGetLocation() throws Exception {
		JSONObject newExternalLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", layerExternalLocation.toString());

		JSONObject externalLocation = helper
				.testGetJsonEntity(newExternalLocation.getString("uri"));
		assertEquals(newExternalLocation.getString("id"), externalLocation
				.getString("id"));
		assertEquals("external", externalLocation.getString("type"));
		assertEquals("33183779e53ce0cfc35f59cc2a762cbd", externalLocation
				.getString("md5sum"));
		assertExpectedLocationProperties(externalLocation);

		JSONObject newS3Location = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", layerS3Location.toString());

		// GET method
		JSONObject s3Location = helper.testGetJsonEntity(newS3Location
				.getString("uri"));
		assertEquals(newS3Location.getString("id"), s3Location.getString("id"));
		assertEquals("awss3", s3Location.getString("type"));
		assertEquals("33183779e53ce0cfc35f59cc2a762cbd", s3Location
				.getString("md5sum"));
		// HEAD method
		Map<String,String> extraParams = new HashMap<String, String>();
		extraParams.put(ServiceConstants.METHOD_PARAM, RequestMethod.HEAD.name());
		JSONObject s3HeadLocation = helper.testGetJsonEntity(newS3Location
				.getString("uri"), extraParams);
		assertEquals(newS3Location.getString("id"), s3HeadLocation.getString("id"));
		assertEquals("awss3", s3HeadLocation.getString("type"));
		assertEquals("33183779e53ce0cfc35f59cc2a762cbd", s3HeadLocation
				.getString("md5sum"));
		
		assertExpectedLocationProperties(s3Location);

		JSONObject storedLayer = helper.testGetJsonEntity(layer
				.getString("uri"));
		assertNotNull(storedLayer);

		JSONObject paging = helper.testGetJsonEntities(layer
				.getString("locations"));
		assertNotNull(paging);
		assertEquals(2, paging.getLong("totalNumberOfResults"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testUpdateLocation() throws Exception {
		JSONObject newLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", datasetS3Location.toString());

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

		String s3key = "/"
			+ updatedLocation.getString("id") + "/"
			+ updatedLocation.getString("versionLabel") + "/"
			+ "unc.edu_COAD.AgilentG4502A_07_3.Level_2.2.0.0.zip";

		// Check that the update response reflects the change
		assertTrue(0 < updatedLocation.getString("path").indexOf(s3key));

		assertTrue(updatedLocation
				.getString("path")
				.matches(
						"^https://s3.amazonaws.com/"
						+ StackConfiguration.getS3Bucket()
						+ s3key
						+ "\\?.*Expires=\\d+&AWSAccessKeyId=\\w+&Signature=.+$"));

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
				+ "/location", layerS3Location.toString());

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
				+ "/location", datasetS3Location.toString());
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

		assertEquals("Entity type: location cannot have a parent of type: null", error.getString("reason"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testMissingParentIdUpdateLocation() throws Exception {
		// Create a location
		JSONObject newLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", layerS3Location.toString());

		// Get that location
		JSONObject location = helper.testGetJsonEntity(newLocation
				.getString("uri"));
		assertEquals(newLocation.getString("id"), location.getString("id"));
		assertEquals("awss3", location.getString("type"));

		// Modify that location to make it invalid
		location.remove(NodeConstants.COL_PARENT_ID);
		JSONObject error = helper.testUpdateJsonEntityShouldFail(location,
				HttpStatus.BAD_REQUEST);

		assertEquals("Entity type: location cannot have a parent of type: null", error.getString("reason"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testMissingRequiredFieldCreateLocation() throws Exception {

		JSONObject error = helper
				.testCreateJsonEntityShouldFail(
						helper.getServletPrefix() + "/location",
						"{\"parentId\":\""+layer.getString("id")+"\", \"type\":\"awss3\", \"md5sum\":\"33183779e53ce0cfc35f59cc2a762cbd\"}",
						HttpStatus.BAD_REQUEST);

		assertEquals("path cannot be null", error.getString("reason"));
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testInvalidTypeCreateLocation() throws Exception {

		JSONObject error = helper
				.testCreateJsonEntityShouldFail(
						helper.getServletPrefix() + "/location",
						"{\"parentId\":\""+layer.getString("id")+"\", \"type\":\"AFakeType\", \"path\":\"foo.txt\", \"md5sum\":\"33183779e53ce0cfc35f59cc2a762cbd\"}",
						HttpStatus.BAD_REQUEST);

		assertTrue(0 <= error.getString("reason").indexOf("'type' must be one of"));
	}
	
	/**
	 * @throws Exception
	 */
	@Test
	public void testMissingRequiredFieldUpdateLocation() throws Exception {
		// Create a location
		JSONObject newLocation = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/location", layerS3Location.toString());

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
				+ "/location", datasetS3Location.toString());
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
				+ "/location", layerS3Location.toString());

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
				+ "/location", datasetS3Location.toString());

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
				+ "/location", layerS3Location.toString());

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
	 * @param location
	 * @throws Exception
	 */
	public static void assertExpectedLocationProperties(JSONObject location) throws Exception {
		// Check required properties
		assertTrue(location.has("type"));
		assertFalse("null".equals(location.getString("type")));
		assertTrue(location.has("path"));
		assertFalse("null".equals(location.getString("path")));
		assertTrue(location.has("md5sum"));
		assertFalse("null".equals(location.getString("md5sum")));
		assertTrue(location.has("contentType"));
		assertFalse("null".equals(location.getString("contentType")));
		assertTrue(location.has(NodeConstants.COL_PARENT_ID));
		assertFalse("null".equals(location
				.getString(NodeConstants.COL_PARENT_ID)));

		if(location.getString("type").equals(
				Location.LocationTypeNames.awss3.toString())) {
			String s3keyPrefix = "/"
				+ location.getString("id");

			assertTrue(0 < location.getString("path").indexOf(s3keyPrefix));
			assertTrue(location
					.getString("path")
					.matches(
							"^https://s3.amazonaws.com/"
							+ StackConfiguration.getS3Bucket()
							+ s3keyPrefix
							+ "/.*\\?.*Expires=\\d+&AWSAccessKeyId=\\w+&Signature=.+$"));
		}

	}
}
