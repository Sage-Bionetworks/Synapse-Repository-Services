package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.manager.TestGroupMembershipDAO;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.manager.UserGroupManager;
import org.sagebionetworks.repo.model.GroupMembershipDAO;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
public class LayerLocationsControllerTest {

	/**
	 * A user for use in unit tests
	 */
	public static final String UNIT_TEST_READ_ONLY_USER_ID = "unit.test@sagebase.org";

	@Autowired
	private Helpers helper;
	private JSONObject dataset;
	private String readOnlyUserId;
	
	private User user;
	
	@Autowired
	private UserGroupManager userGroupManager;
	
	private UserDAO userDao;
	
	/**
	 * A user for use in integration tests
	 */
	public static final String INTEGRATION_TEST_READ_ONLY_USER_ID = "integration.test@sagebase.org";
	/**
	 * 
	 */
	public static final String FAKE_ACCESS_ID = "thisIsAFakeAWSAccessId";
	/**
	 * 
	 */
	public static final String FAKE_SECRET_KEY = "thisIsAFakeAWSSecretKey";

	


	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		helper.setUp();

		if (helper.isIntegrationTest()) {
			readOnlyUserId = INTEGRATION_TEST_READ_ONLY_USER_ID;
		} else {
			readOnlyUserId = UNIT_TEST_READ_ONLY_USER_ID;
			
			this.userDao=new TestUserDAO();
			userGroupManager.setUserDAO(userDao);
			GroupMembershipDAO groupMembershipDAO = new TestGroupMembershipDAO();
			userGroupManager.setGroupMembershipDAO(groupMembershipDAO);

			// TODO talk to Bruce to determine the right way to bootstrap users

			// Make a user and his credentials
//			userDao = helper.getDaoFactory().getUserDAO(null);
			user = new User();
			user.setUserId(readOnlyUserId);
			userDao.create(user);
//			UserCredentialsDAO credsDao = helper.getDaoFactory()
//					.getUserCredentialsDAO(readOnlyUserId);
//			UserCredentials storedCre/ds;
//			storedCreds = credsDao.get(readOnlyUserId);
			user.setIamAccessId(FAKE_ACCESS_ID);
			user.setIamSecretKey(FAKE_SECRET_KEY);
			userDao.update(user);
//			credsDao.update(storedCreds);
		}

		dataset = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/dataset", DatasetControllerTest.SAMPLE_DATASET);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		if (!helper.isIntegrationTest()) {
			userDao.delete(user.getId());
		}

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

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), LayerControllerTest.SAMPLE_LAYER);

		// Get the layer
		JSONObject layerPreview = helper.testGetJsonEntity(newLayer
				.getString("preview"));

		assertEquals(newLayer.getString("id"), layerPreview.getString("id"));
		assertEquals("null", layerPreview.getString("preview"));

		// Modify that layer
		layerPreview.put("preview", tabDelimitedSnippet);

		JSONObject updatedLayerPreview = helper
				.testUpdateJsonEntity(layerPreview);

		// Check that the update response reflects the change
		assertEquals(tabDelimitedSnippet, updatedLayerPreview
				.getString("preview"));

		// Now make sure the stored one reflects the change too
		// TODO do we want to leave a breadcrumb for this uri?
		JSONObject layerPreviewMap = helper.testGetJsonObject(newLayer
				.getString("uri")
				+ UrlHelpers.PREVIEW_MAP);
		assertNotNull(layerPreviewMap.getInt("totalNumberOfResults"));
		assertNotNull(layerPreviewMap.getJSONArray("results"));
		assertEquals(5, layerPreviewMap.getJSONArray("results").length());
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
				.getString("layer"), LayerControllerTest.SAMPLE_LAYER);

		// Get the layer
		JSONObject layerPreview = helper.testGetJsonEntity(newLayer
				.getString("preview"));

		assertEquals(newLayer.getString("id"), layerPreview.getString("id"));
		assertEquals("null", layerPreview.getString("preview"));

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
				.getString("layer"), LayerControllerTest.SAMPLE_LAYER);

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
				"{\"type\":\"awsebs\", \"path\":\"snap-29d33a42 (US West)\", \"md5sum\":\"b4c1e441ecb754271e0dee5020fd38e4\"}"));
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
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.LayerLocationsController#updateDependentEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testLayerLocationsSanityCheck() throws Exception {

		testUpdateLayerLocations();

		// As a sanity check, make sure we can walk from one end to the other
		JSONObject saneDataset = helper.testGetJsonEntity(dataset
				.getString("uri"));
		JSONObject saneLayers = helper.testGetJsonEntities(saneDataset
				.getString("layer"), null, null, null, null);
		JSONObject saneLayer = helper.testGetJsonEntity(saneLayers
				.getJSONArray("results").getJSONObject(0).getString("uri"));
		helper.setUserId(readOnlyUserId);
		
		// make sure our fake userDAO has the user set up
		if (!helper.isIntegrationTest()) {
			assertEquals(readOnlyUserId, userDao.getUser(readOnlyUserId).getUserId());
		}
		
		for (int i = 0; i < saneLayer.getJSONArray("locations").length(); i++) {
			String locationUri = saneLayer.getJSONArray("locations").getString(
					i);
			if (locationUri.endsWith(UrlHelpers.S3_LOCATION)
					&& helper.isIntegrationTest()) {
				// skip this integration test

				// TODO get this to work as an integration test, that means the
				// remote service was properly bootstrapped with
				// READ_ONLY_USER_ID
			} else {
				helper.testGetJsonObject(locationUri);
			}
		}
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
				.getString("layer"), LayerControllerTest.SAMPLE_LAYER);

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
		helper.setUserId(readOnlyUserId);
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));
		LayerControllerTest.assertExpectedLayerProperties(layer);

		// Get the location
		JSONObject location = null;
		for (int i = 0; i < layer.getJSONArray("locations").length(); i++) {
			String locationUri = layer.getJSONArray("locations").getString(i);
			if (locationUri.endsWith(UrlHelpers.S3_LOCATION)) {
				// TODO get this to work as an integration test, that means the
				// remote service was properly bootstrapped with
				// READ_ONLY_USER_ID
				if (helper.isIntegrationTest()) {
					return;
				}
				location = helper.testGetJsonObject(locationUri);
				break;
			}
		}

		assertTrue(location
				.getString("path")
				.matches(
						"^https://s3.amazonaws.com/data01.sagebase.org/[^?]+\\?Expires=\\d+&AWSAccessKeyId=\\w+&Signature=.+$"));

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
				.getString("layer"), LayerControllerTest.SAMPLE_LAYER);

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
		LayerControllerTest.assertExpectedLayerProperties(layer);

		// Get the location
		JSONObject location = null;
		for (int i = 0; i < layer.getJSONArray("locations").length(); i++) {
			String locationUri = layer.getJSONArray("locations").getString(i);
			if (locationUri.endsWith(UrlHelpers.EBS_LOCATION)) {
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
				.getString("layer"), LayerControllerTest.SAMPLE_LAYER);

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
		LayerControllerTest.assertExpectedLayerProperties(layer);

		// Get the location
		JSONObject location = null;
		for (int i = 0; i < layer.getJSONArray("locations").length(); i++) {
			String locationUri = layer.getJSONArray("locations").getString(i);
			if (locationUri.endsWith(UrlHelpers.SAGE_LOCATION)) {
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
	public void testInvalidLocationModelCreateLayer() throws Exception {

		JSONObject newLayer = helper.testCreateJsonEntity(dataset
				.getString("layer"), LayerControllerTest.SAMPLE_LAYER);

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
		assertTrue(reason
				.startsWith("'type' must be one of: awss3 awsebs sage"));
	}

	/*****************************************************************************************************
	 * Not Found Tests
	 */

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
			assertFalse("null".equals(location.getString("type")));
			assertFalse("null".equals(location.getString("path")));
		}
	}

}
