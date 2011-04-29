package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.model.jdo.JDODAOFactoryImpl;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit tests for the User CRUD operations exposed by the UserController
 * with JSON request and response encoding.
 * <p>
 * 
 * Note that test logic and assertions common to operations for all DAO-backed
 * entities can be found in the Helpers class. What follows are test cases that
 * make use of that generic test logic with some assertions specific to
 * users.
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserControllerTest {

	@Autowired
	private Helpers helper;

	UserDAO userDAO =null;
	List<User> users = new ArrayList<User>(); // list of things to delete
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();
		userDAO = (new JDODAOFactoryImpl()).getUserDAO(null);
		makeUser();
	}
	
	private void makeUser() throws Exception {
		// create a user
		User user = new User();
		user.setUserId("testUserId");
		user.setIamAccessId("foo");
		user.setIamSecretKey("bar");
		userDAO.create(user);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/user");
		user.setUri(UrlHelpers.makeEntityUri(user, request));
		user.setEtag(UrlHelpers.makeEntityEtag(user));
		String uid = user.getId();
		this.users.add(user);
		assertNotNull(uid);
		assertNotNull(user.getUri());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		for (User user: users) {
			userDAO.delete(user.getId());
		}
		helper.tearDown();
	}

	/*************************************************************************************************************************
	 * Happy case tests
	 */

	

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.UserController#getEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetUser() throws Exception {

		User user = users.get(0);
		JSONObject results = helper.testGetJsonEntity(user.getUri());

		assertEquals(user.getId(), results.getString("id"));
		assertEquals(results.toString(), user.getIamAccessId(), results.getString("iamAccessId"));
		assertEquals(results.toString(), user.getIamSecretKey(), results.getString("iamSecretKey"));

		assertExpectedUserProperties(results);
	}

	/**
	 * Test retrieving multiple groups
	 * 
	 */
	@Test
	public void testGetUserS() throws Exception {
		
		// see how many user groups there are initially 
		// (Groups like Public and Admin may already exist.)
		JSONObject results = helper.testGetJsonEntities(helper
				.getServletPrefix()
				+ "/user", null, null, null, null);
		
		assertTrue(results.toString(), results.getInt("totalNumberOfResults")>0);

		assertExpectedUsersProperties(results.getJSONArray("results"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.UserController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateUser() throws Exception {

		User user = users.get(0);
		JSONObject results = helper.testGetJsonEntity(user.getUri());
		assertEquals(user.getId(), results.getString("id"));
		assertEquals(user.getUserId(), results.getString("userId"));

		// Modify that user
		String origUserId = results.getString("userId");
		results.put("userId", "MouseX");
		JSONObject updatedUser = helper.testUpdateJsonEntityShouldFail(results, HttpStatus.BAD_REQUEST);

		results.put("userId", origUserId);
		results.put("iamAccessId", "bas");
		updatedUser = helper.testUpdateJsonEntity(results);

		// Check that the update response reflects the change
		assertEquals("bas", updatedUser.getString("iamAccessId"));

		// Now make sure the stored one reflects the change too
		JSONObject storedUser = helper.testGetJsonEntity(user.getUri());
		assertEquals("bas", storedUser.getString("iamAccessId"));
	}


	/*****************************************************************************************************
	 * Bad parameters tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.UserController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMissingRequiredFieldUpdateUser() throws Exception {
		User user = users.get(0);

		// Get that user
		JSONObject result = helper.testGetJsonEntity(user.getUri());
		assertEquals(user.getId(), result.getString("id"));
		assertEquals(user.getUserId(), result.getString("userId"));

		// Modify that user to make it invalid
		result.remove("userId");
		JSONObject error = helper.testUpdateJsonEntityShouldFail(result,
				HttpStatus.BAD_REQUEST);

		assertEquals("'userId' is a required property for User", error
				.getString("reason"));
	}

	/*****************************************************************************************************
	 * Not Found Tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.EntityControllerImp#getEntity} .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetNonExistentUser() throws Exception {
		User user = users.get(0);
		JSONObject results = helper.testGetJsonEntity(user.getUri());
		JSONObject error = helper.testGetJsonEntityShouldFail(results
				.getString("uri")+"0", HttpStatus.NOT_FOUND);
		assertNotNull(error.getString("reason"));
	}

	/*****************************************************************************************************
	 * User-specific helpers
	 */

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedUserProperties(JSONObject results)
			throws Exception {
		// Check required properties
		assertTrue(results.has("userId"));
		assertFalse("null".equals(results.getString("userId")));
		assertTrue(results.has("iamAccessId"));
		assertFalse("null".equals(results.getString("iamAccessId")));
		assertTrue(results.has("iamSecretKey"));
		assertFalse("null".equals(results.getString("iamSecretKey")));

		// Check immutable system-defined properties

		assertTrue(results.has("creationDate"));
		assertFalse("null".equals(results.getString("creationDate")));
	}
	
	public static void assertExpectedUsersProperties(JSONArray results) throws Exception {
		for (int i = 0; i < results.length(); i++) {
			JSONObject ug = results.getJSONObject(i);
			assertExpectedUserProperties(ug);
		}
	}

}
