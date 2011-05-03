/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
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
public class ControllerAuthorizationTest {

	private static final String CURATOR1_USER_ID = "matt.furia@sagebase.org";
	private static final String CURATOR2_USER_ID = "solly.sieberts@sagebase.org";
	private static final String READONLY_USER_ID = "john.doe@gmail.com";

	@Autowired
	private Helpers helper;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		// TODO use an admin user in helpers to delete stuff for post-test
		// cleanup
		helper.setUserId(CURATOR1_USER_ID);
		helper.tearDown();
	}

	/**
	 * TODO get this test to pass
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testCreateAuthorization() throws Exception {

		if (helper.isIntegrationTest()) {
			// TODO once our remote service is properly bootstrapped with users
			// we'll be able to do integration tests that perform stuff
			// requiring authorization
			return;
		}

		UserDAO userDao = helper.getDaoFactory().getUserDAO(null);
		UserGroupDAO groupDao = helper.getDaoFactory().getUserGroupDAO(null);

		User user = new User();
		user.setUserId(READONLY_USER_ID);
		userDao.create(user);

		UserGroup curators = new UserGroup();
		curators.setName("Curators");
		groupDao.create(curators);

		User curator1 = new User();
		curator1.setUserId(CURATOR1_USER_ID);
		userDao.create(curator1);
		groupDao.addUser(curators, curator1);

		User curator2 = new User();
		curator2.setUserId(CURATOR2_USER_ID);
		userDao.create(curator2);
		groupDao.addUser(curators, curator2);

		// Create a dataset
		helper.setUserId(CURATOR1_USER_ID);
		JSONObject dataset = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/dataset", DatasetControllerTest.SAMPLE_DATASET);
		assertTrue(dataset.has("id"));

		// Only the creator can currently access it
		helper.setUserId(CURATOR1_USER_ID);
		helper.testGetJsonEntity(dataset.getString("uri"));
		helper.setUserId(CURATOR2_USER_ID);
		helper.testGetJsonEntityShouldFail(dataset.getString("uri"),
				HttpStatus.FORBIDDEN);
		helper.setUserId(READONLY_USER_ID);
		helper.testGetJsonEntityShouldFail(dataset.getString("uri"),
				HttpStatus.FORBIDDEN);

		// Creator users opens up the permissions
		UserGroupDAO datasetGroupDao = helper.getDaoFactory().getUserGroupDAO(
				CURATOR1_USER_ID);
		// for 'add resource' we actually have to pass in the DTO object, not just the ID
		Dataset ds = new Dataset(); ds.setId(dataset.getString("id"));
		datasetGroupDao.addResource(datasetGroupDao.getPublicGroup(), ds,
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[]{AuthorizationConstants.ACCESS_TYPE.READ}));
		datasetGroupDao.addResource(curators, ds,
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[]{AuthorizationConstants.ACCESS_TYPE.READ}));
		datasetGroupDao.addResource(curators, ds,
				Arrays.asList(new AuthorizationConstants.ACCESS_TYPE[]{AuthorizationConstants.ACCESS_TYPE.CHANGE}));

		// Read/Write user tries to get the dataset - should pass
		helper.setUserId(CURATOR2_USER_ID);
		JSONObject storedDataset = helper.testGetJsonEntity(dataset
				.getString("uri"));
		assertEquals(dataset.getString("name"), storedDataset.getString("name"));

		// Read-Only user tries to get the dataset - should pass
		helper.setUserId(READONLY_USER_ID);
		JSONObject storedDataset2 = helper.testGetJsonEntity(dataset
				.getString("uri"));
		assertEquals(dataset.getString("name"), storedDataset2
				.getString("name"));

		// Read/Write user tries to modify the dataset - should pass
		storedDataset.put("name", "a name updated by another curator");
		helper.setUserId(CURATOR2_USER_ID);
		JSONObject updatedDataset = helper.testUpdateJsonEntity(storedDataset);
		assertFalse(dataset.getString("name").equals(
				updatedDataset.getString("name")));

		// Read-Only user tries to modify the dataset - should fail
		updatedDataset
				.put(
						"name",
						"a name updated by an a read only user - this should fail with a 403 not a 412!");
		helper.setUserId(READONLY_USER_ID);
		JSONObject error = helper.testUpdateJsonEntityShouldFail(
				updatedDataset, HttpStatus.FORBIDDEN);
		assertEquals(
				"You are not authorized to access the requested resource and/or perform the requested activity",
				error.getString("reason"));

		// Read-Only user tries to delete the dataset - should fail
		helper.setUserId(READONLY_USER_ID);
		JSONObject deleteError = helper.testDeleteJsonEntityShouldFail(
				updatedDataset.getString("uri"), HttpStatus.FORBIDDEN);
		assertEquals(
				"You are not authorized to access the requested resource and/or perform the requested activity",
				deleteError.getString("reason"));

		// Read/Write user tries to delete the dataset - should pass
		helper.setUserId(CURATOR2_USER_ID);
		helper.testDeleteJsonEntity(dataset.getString("uri"));

		// Clean up from test by deleting users and groups
		userDao.delete(user.getId());
		userDao.delete(curator1.getId());
		userDao.delete(curator2.getId());
		groupDao.delete(curators.getId());
	}

	// TODO only some users should be able to create/update/delete resources

	// TODO get multiple datasets

}
