package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit tests for the UserGroup CRUD operations exposed by the UserGroupController
 * with JSON request and response encoding.
 * <p>
 * 
 * Note that test logic and assertions common to operations for all DAO-backed
 * entities can be found in the Helpers class. What follows are test cases that
 * make use of that generic test logic with some assertions specific to
 * userGroups.
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class UserGroupControllerTest {
	
	// TODO enable as integration test:
	// the services layer does not expose USER creation/deletion, therefore
	// setting up the user for testing is either done in the local container
	// (in the case of unit testing) or by creating/deleting the user in Crowd
	// followed by a call to the 'mirror' service.  At this time, only the
	// former is implemented.  During integration testing SOME OF the tests in this
	// suite are bypassed.  Though unit testing should be sufficient for exercising
	// the service's logic, rerunning as an integration test would be a nice
	// addition.
	private boolean isIntegrationTest() {
		String integrationTestEndpoint = System.getProperty("INTEGRATION_TEST_ENDPOINT");
		return integrationTestEndpoint!=null && integrationTestEndpoint.length()>0;
	}



	@Autowired
	private Helpers helper;

	@Autowired
	UserDAO userDAO;
	
	List<User> users = new ArrayList<User>(); // list of things to delete
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
		for (User user: users) {
			userDAO.delete(user.getId());
		}
		helper.tearDown();
	}

	/*************************************************************************************************************************
	 * Happy case tests
	 */

//	/**
//	 * Test method for
//	 * {@link org.sagebionetworks.repo.web.controller.UserGroupController#createEntity}
//	 * .
//	 * 
//	 * @throws Exception
//	 */
//	@Test
//	public void testCreateUserGroup() throws Exception {
//		JSONObject results = helper.testCreateJsonEntity(helper
//				.getServletPrefix()
//				+ "/usergroup", "{\"name\":\"FederationGroup\"}");
//
//		// Check required properties
//		assertEquals("FederationGroup", results.getString("name"));
//
//		assertExpectedUserGroupProperties(results);
//	}
//	
//	/**
//	 * Test adding a user to a group
//	 */
//	@Test
//	public void testAddandRemoveUser() throws Exception {
//		if (isIntegrationTest()) return; // can't create a User in another container
//
//		// create a group
//		JSONObject group = helper.testCreateJsonEntity(helper
//				.getServletPrefix()
//				+ "/usergroup", "{\"name\":\"FederationGroup\"}");
//
//		// Check required properties
//		assertEquals("FederationGroup", group.getString("name"));
//		
//		// create a user
//		User user = new User();
//		user.setUserId("testUserId");
//		userDAO.create(user);
//		String uid = user.getId();
//		this.users.add(user);
//		assertNotNull(uid);
//		
//		// add the user to the group
//		helper.testCreateNoResponse(helper.getServletPrefix()+
//				"/usergroup/"+group.getString("id")+"/users/"+uid, "{}");
//
//		// get the users in the group
//		JSONObject users = helper.testGetJsonEntities(helper
//				.getServletPrefix()
//				+ "/usergroup/"+group.getString("id")+"/users", null, null, null, null);
//		
//		// is the user there?
//		assertEquals(1, users.getInt("totalNumberOfResults"));
//		assertEquals(1, users.getJSONArray("results").length());
//		JSONObject jsonUser = users.getJSONArray("results").getJSONObject(0);
//		assertFalse(jsonUser.toString(), "null".equals(jsonUser.getString("userId")));
//		assertEquals(uid, jsonUser.getString("id"));
//
//		// remove the user
//		helper.testDeleteJsonEntity(helper.getServletPrefix()+
//				"/usergroup/"+group.getString("id")+"/users/"+uid, false);
//		
//		// get the users in the group
//		 users = helper.testGetJsonEntities(helper
//				.getServletPrefix()
//				+ "/usergroup/"+group.getString("id")+"/users", null, null, null, null);
//		 
//		// check that the user's gone
//		assertEquals(0, users.getInt("totalNumberOfResults"));
//		assertEquals(0, users.getJSONArray("results").length());
//	}
//	

	/**
	 * Test adding a resource to a group
	 */
	@Test
	@Ignore
	public void testAddandRemoveResource() throws Exception {
		if (isIntegrationTest()) return; // can't create a User in another container

		// create a group
		JSONObject group = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/usergroup", "{\"name\":\"FederationGroup\"}");

		// Check required properties
		assertEquals("FederationGroup", group.getString("name"));
		
		// create a resource.  TODO replace this with a node-based resource
		User user = new User();
		user.setUserId("testUserId");
		userDAO.create(user);
		this.users.add(user);
		String resourceId = user.getId();
		assertNotNull(resourceId);
		
		// add the resource to the group
		helper.testCreateNoResponse(helper.getServletPrefix()+
				"/usergroup/"+group.getString("id")+"/resources/"+resourceId, 
					"{\"accessType\":[\"READ\",\"SHARE\"]}");

		// get the access types for this resource
		JSONObject accessTypes = helper.testGetJsonObject(helper.getServletPrefix()
				+ "/usergroup/"+group.getString("id")+"/resources/"+resourceId);
		
		assertTrue(accessTypes.getString("accessType"), accessTypes.getString("accessType").indexOf("\"SHARE\"")>0);
		assertTrue(accessTypes.getString("accessType"), accessTypes.getString("accessType").indexOf("\"READ\"")>0);

		// remove the resource
		helper.testDeleteJsonEntity(helper.getServletPrefix()+
				"/usergroup/"+group.getString("id")+"/resources/"+resourceId, false);
		
		// get the access types for this resource, again
		accessTypes = helper.testGetJsonObject(helper.getServletPrefix()
				+ "/usergroup/"+group.getString("id")+"/resources/"+resourceId);
		
		// should return nothing
		assertEquals("[]", accessTypes.getString("accessType"));
	}
	

//	/**
//	 * Test method for
//	 * {@link org.sagebionetworks.repo.web.controller.UserGroupController#getEntity}
//	 * .
//	 * 
//	 * @throws Exception
//	 */
//	@Test
//	public void testGetUserGroup() throws Exception {
//		// Load up a few userGroups
//		helper.testCreateJsonEntity(helper.getServletPrefix() + "/usergroup",
//				"{\"name\":\"FederationGroup\"}");
//		helper.testCreateJsonEntity(helper.getServletPrefix() + "/usergroup",
//				"{\"name\":\"AgingGroup\"}");
//		JSONObject newUserGroup = helper.testCreateJsonEntity(helper
//				.getServletPrefix()
//				+ "/usergroup", "{\"name\":\"SageGroup\"}");
//
//		JSONObject results = helper.testGetJsonEntity(newUserGroup
//				.getString("uri"));
//
//		assertEquals(newUserGroup.getString("id"), results.getString("id"));
//		assertEquals("SageGroup", results.getString("name"));
//
//		assertExpectedUserGroupProperties(results);
//	}
//
//	/**
//	 * Test retrieving multiple groups
//	 * 
//	 */
//	@Test
//	public void testGetUserGroupS() throws Exception {
//		
//		// see how many user groups there are initially 
//		// (Groups like Public and Admin may already exist.)
//		JSONObject results = helper.testGetJsonEntities(helper
//				.getServletPrefix()
//				+ "/usergroup", null, null, null, null);
//
//		int numGroups = results.getInt("totalNumberOfResults");
//
//		// Load up a few userGroups
//		helper.testCreateJsonEntity(helper.getServletPrefix() + "/usergroup",
//				"{\"name\":\"FederationGroup\"}");
//		helper.testCreateJsonEntity(helper.getServletPrefix() + "/usergroup",
//				"{\"name\":\"AgingGroup\"}");
//		
//		numGroups += 2;
//		
//		results = helper.testGetJsonEntities(helper
//				.getServletPrefix()
//				+ "/usergroup", null, null, null, null);
//		assertEquals(results.toString(), numGroups, results.getInt("totalNumberOfResults"));
//		assertEquals(results.toString(), numGroups, results.getJSONArray("results").length());
//
//		assertExpectedUserGroupsProperties(results.getJSONArray("results"));
//	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.UserGroupController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateUserGroup() throws Exception {
		// Load up a few usergroups
		
		// TODO:  Note, can't create via web service
//		helper.testCreateJsonEntity(helper.getServletPrefix() + "/usergroup",
//				"{\"name\":\"FederationGroup\"}");
//		helper.testCreateJsonEntity(helper.getServletPrefix() + "/usergroup",
//				"{\"name\":\"AgingGroup\"}");
//		JSONObject newUserGroup = helper.testCreateJsonEntity(helper
//				.getServletPrefix()
//				+ "/usergroup", "{\"name\":\"SageGroup\"}");
//
//		// Get one usergroup
//		JSONObject userGroup = helper.testGetJsonEntity(newUserGroup
//				.getString("uri"));
//		assertEquals(newUserGroup.getString("id"), userGroup.getString("id"));
//		assertEquals("SageGroup", userGroup.getString("name"));
//
//		// Modify that userGroup
//		userGroup.put("name", "MouseX");
//		JSONObject updatedUserGroup = helper.testUpdateJsonEntity(userGroup);
//		assertExpectedUserGroupProperties(updatedUserGroup);
//
//		// Check that the update response reflects the change
//		assertEquals("MouseX", updatedUserGroup.getString("name"));
//
//		// Now make sure the stored one reflects the change too
//		JSONObject storedUserGroup = helper.testGetJsonEntity(newUserGroup
//				.getString("uri"));
//		assertEquals("MouseX", storedUserGroup.getString("name"));
	}

//	/**
//	 * Test method for
//	 * {@link org.sagebionetworks.repo.web.controller.UserGroupController#deleteEntity}
//	 * .
//	 * 
//	 * @throws Exception
//	 */
//	@Test
//	public void testDeleteUserGroup() throws Exception {
//		// Load up a few usergroups
//		helper.testCreateJsonEntity(helper.getServletPrefix() + "/usergroup",
//				"{\"name\":\"FederationGroup\"}");
//		helper.testCreateJsonEntity(helper.getServletPrefix() + "/usergroup",
//				"{\"name\":\"AgingGroup\"}");
//		JSONObject newUserGroup = helper.testCreateJsonEntity(helper
//				.getServletPrefix()
//				+ "/usergroup", "{\"name\":\"SageGroup\"}");
//		helper.testDeleteJsonEntity(newUserGroup.getString("uri"));
//	}

//	/*****************************************************************************************************
//	 * Bad parameters tests
//	 */
//
//	/**
//	 * Test method for
//	 * {@link org.sagebionetworks.repo.web.controller.UserGroupController#createEntity}
//	 * .
//	 * 
//	 * @throws Exception
//	 */
//	@Test
//	public void testInvalidModelCreateUserGroup() throws Exception {
//
//		JSONObject error = helper
//				.testCreateJsonEntityShouldFail(
//						helper.getServletPrefix() + "/usergroup",
//						"{\"name\": \"FederationGroup\", \"BOGUS\":\"this does not match our model object\"}",
//						HttpStatus.BAD_REQUEST);
//
//		String reason = error.getString("reason");
//		assertTrue(reason.matches("(?s).*\"BOGUS\".*"));
//		assertTrue(reason.matches("(?s).*not marked as ignorable.*"));
//	}
//
//	/**
//	 * Test method for
//	 * {@link org.sagebionetworks.repo.web.controller.UserGroupController#createEntity}
//	 * .
//	 * 
//	 * @throws Exception
//	 */
//	@Test
//	public void testMissingRequiredFieldCreateUserGroup() throws Exception {
//
//		JSONObject error = helper.testCreateJsonEntityShouldFail(helper
//				.getServletPrefix()
//				+ "/usergroup", "{}",
//				HttpStatus.BAD_REQUEST);
//
//		assertEquals("'name' is a required property for UserGroup", error
//				.getString("reason"));
//	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.UserGroupController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMissingRequiredFieldUpdateUserGroup() throws Exception {
		// Create a userGroup
		
		// TODO:  Note, can't create group via web service
//		JSONObject newUserGroup = helper.testCreateJsonEntity(helper
//				.getServletPrefix()
//				+ "/usergroup", "{\"name\":\"MyGroup\"}");
//
//		// Get that userGroup
//		JSONObject userGroup = helper.testGetJsonEntity(newUserGroup
//				.getString("uri"));
//		assertEquals(newUserGroup.getString("id"), userGroup.getString("id"));
//		assertEquals("MyGroup", userGroup.getString("name"));
//
//		// Modify that userGroup to make it invalid
//		userGroup.remove("name");
//		JSONObject error = helper.testUpdateJsonEntityShouldFail(userGroup,
//				HttpStatus.BAD_REQUEST);
//
//		assertEquals("'name' is a required property for UserGroup", error
//				.getString("reason"));
	}

//	/*****************************************************************************************************
//	 * Not Found Tests
//	 */
//
//	/**
//	 * Test method for
//	 * {@link org.sagebionetworks.repo.web.EntityControllerImp#getEntity} .
//	 * 
//	 * @throws Exception
//	 */
//	@Test
//	public void testGetNonExistentUserGroup() throws Exception {
//		JSONObject results = helper.testCreateJsonEntity(helper
//				.getServletPrefix()
//				+ "/usergroup", "{\"name\":\"FederationGroup\"}");
//
//		helper.testDeleteJsonEntity(results.getString("uri"));
//
//		JSONObject error = helper.testGetJsonEntityShouldFail(results
//				.getString("uri"), HttpStatus.NOT_FOUND);
//		assertEquals(
//				"The resource you are attempting to access cannot be found",
//				error.getString("reason"));
//	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.UserGroupController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateNonExistentUserGroup() throws Exception {
		
		// TODO: note: Can't create user-group via web service
//		JSONObject results = helper.testCreateJsonEntity(helper
//				.getServletPrefix()
//				+ "/usergroup", "{\"name\":\"FederationGroup\"}");
//
//		helper.testDeleteJsonEntity(results.getString("uri"));
//
//		JSONObject error = helper.testUpdateJsonEntityShouldFail(results,
//				HttpStatus.NOT_FOUND);
//		assertEquals(
//				"The resource you are attempting to access cannot be found",
//				error.getString("reason"));
	}

//	/**
//	 * Test method for
//	 * {@link org.sagebionetworks.repo.web.controller.UserGroupController#deleteEntity}
//	 * .
//	 * 
//	 * @throws Exception
//	 */
//	@Test
//	public void testDeleteNonExistentUserGroup() throws Exception {
//		JSONObject results = helper.testCreateJsonEntity(helper
//				.getServletPrefix()
//				+ "/usergroup", "{\"name\":\"FederationGroup\"}");
//
//		helper.testDeleteJsonEntity(results.getString("uri"));
//
//		JSONObject error = helper.testDeleteJsonEntityShouldFail(results
//				.getString("uri"), HttpStatus.NOT_FOUND);
//		assertEquals(
//				"The resource you are attempting to access cannot be found",
//				error.getString("reason"));
//	}

	/*****************************************************************************************************
	 * UserGroup-specific helpers
	 */

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedUserGroupProperties(JSONObject results)
			throws Exception {
		// Check required properties
		assertTrue(results.has("name"));
		assertFalse("null".equals(results.getString("name")));

		// Check immutable system-defined properties

		assertTrue(results.has("creationDate"));
		assertFalse("null".equals(results.getString("creationDate")));
	}
	
	public static void assertExpectedUserGroupsProperties(JSONArray results) throws Exception {
		for (int i = 0; i < results.length(); i++) {
			JSONObject ug = results.getJSONObject(i);
			assertExpectedUserGroupProperties(ug);
		}
	}

}
