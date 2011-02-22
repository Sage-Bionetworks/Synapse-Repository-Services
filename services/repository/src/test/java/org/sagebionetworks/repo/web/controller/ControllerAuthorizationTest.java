/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupDAO;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author deflaux
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:repository-context.xml",
		"classpath:repository-servlet.xml" })
public class ControllerAuthorizationTest {

	private static final String CURATOR1_USER_ID = "matt.furia@sagebase.org";
	private static final String CURATOR2_USER_ID = "solly.sieberts@sagebase.org";
	private static final String READONLY_USER_ID = "john.doe@gmail.com";

	private Helpers helper = new Helpers();

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
		helper.tearDown();
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testCreateAuthorization() throws Exception {

		// TODO @Autowired, no GAE references allowed in this class
		final DAOFactory DAO_FACTORY = new GAEJDODAOFactoryImpl();
		UserDAO userDao = DAO_FACTORY.getUserDAO(null);
		UserGroupDAO groupDao = DAO_FACTORY.getUserGroupDAO(null);

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
		JSONObject dataset = helper.testCreateJsonEntity("/dataset",
				DatasetControllerTest.SAMPLE_DATASET);
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
		UserGroupDAO datasetGroupDao = DAO_FACTORY
				.getUserGroupDAO(CURATOR1_USER_ID);
		datasetGroupDao.addResource(datasetGroupDao.getPublicGroup(), dataset
				.getString("id"), AuthorizationConstants.READ_ACCESS);
		datasetGroupDao.addResource(curators, dataset.getString("id"),
				AuthorizationConstants.READ_ACCESS);
		datasetGroupDao.addResource(curators, dataset.getString("id"),
				AuthorizationConstants.CHANGE_ACCESS);

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

	}

	// TODO only some users should be able to create/update/delete resources

	// TODO get multiple datasets

}
