package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.ServiceConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Unit tests for the Code CRUD operations exposed by the CodeController with
 * JSON request and response encoding.
 * <p>
 * 
 * Note that test logic and assertions common to operations for all DAO-backed
 * entities can be found in the Helpers class. What follows are test cases that
 * make use of that generic test logic with some assertions specific to code.
 * <p>
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class CodeControllerTest {

	@Autowired
	private Helpers helper;
	private JSONObject project;
//	private JSONObject dataset;

	/**
	 * Some properties for a code to use for unit tests
	 */
	private final static String SAMPLE_CODE_1 = "{\"name\":\"Bayesian Network\",  "
			+ "\"description\": \"foo\", \"parentId\":\"";
	
	/**
	 * Build a sample code.
	 * @param parentId
	 * @return
	 */
	public static String getSampleCode(String parentId){
		StringBuilder builder = new StringBuilder();
		builder.append(SAMPLE_CODE_1);
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

//		dataset = helper.testCreateJsonEntity(helper.getServletPrefix()
//				+ "/dataset", DatasetControllerTest.getSampleDataset(project.getString("id")));
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
	 * {@link org.sagebionetworks.repo.web.controller.CodeController#createChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateCode() throws Exception {

		// Sanity check - make sure we can find our project
		JSONObject allProjects = helper.testGetJsonEntities(helper
				.getServletPrefix()
				+ "/project", null, null, null, null);
		assertTrue(allProjects.getInt("totalNumberOfResults")>0);

		JSONObject newCode = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/code", CodeControllerTest.getSampleCode(project.getString(NodeConstants.COL_ID)));
		assertExpectedCodeProperties(newCode);
		// Check required properties
		assertEquals("Bayesian Network", newCode.getString("name"));

		// Sanity check - make sure we can _STILL_ find our project
		allProjects = helper.testGetJsonEntities(helper.getServletPrefix()
				+ "/project", null, null, null, null);
		assertTrue(allProjects.getInt("totalNumberOfResults")>0);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.CodeController#getChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetCode() throws Exception {

		JSONObject newCode = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/code", CodeControllerTest.getSampleCode(project.getString(NodeConstants.COL_ID)));

		// Get the code
		Map<String,String> extraParams = new HashMap<String, String>();
		extraParams.put(ServiceConstants.METHOD_PARAM, RequestMethod.HEAD.name());
		JSONObject results = helper
				.testGetJsonEntity(newCode.getString("uri"), extraParams);

		assertEquals(newCode.getString(NodeConstants.COL_ID), results.getString(NodeConstants.COL_ID));
		assertEquals("Bayesian Network", results.getString("name"));

		assertExpectedCodeProperties(results);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.CodeController#updateChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateCode() throws Exception {

		JSONObject newCode = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/code", CodeControllerTest.getSampleCode(project.getString(NodeConstants.COL_ID)));
		// Get the code
		JSONObject code = helper.testGetJsonEntity(newCode.getString("uri"));

		assertEquals(newCode.getString(NodeConstants.COL_ID), code.getString(NodeConstants.COL_ID));
		assertEquals("Bayesian Network", code.getString("name"));

		// Modify that code
		code.put("name", "Coexpression Network");
		JSONObject updatedCode = helper.testUpdateJsonEntity(code);
		assertExpectedCodeProperties(updatedCode);

		// Check that the update response reflects the change
		assertEquals("Coexpression Network", updatedCode.getString("name"));

		// Now make sure the stored one reflects the change too
		JSONObject storedCode = helper.testGetJsonEntity(newCode
				.getString("uri"));
		assertEquals("Coexpression Network", storedCode.getString("name"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.CodeController#deleteChildEntity}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteCode() throws Exception {

		JSONObject newCode = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/code", CodeControllerTest.getSampleCode(project.getString(NodeConstants.COL_ID)));

		try {
			helper.testDeleteJsonEntity(newCode.getString("uri"));
		} catch (Exception e) {
			throw new Exception("Exception deleting "+newCode.getString("uri"), e);
		}
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.CodeController#getChildEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetCodes() throws Exception {

		helper
				.testCreateJsonEntity(helper.getServletPrefix()
						+"/code",
						"{\"name\":\"Bayesian Netowrk\", "
								+ " \"description\": \"foo\", \"parentId\":\""+project.getString(NodeConstants.COL_ID)+"\"}");
		helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/code", CodeControllerTest.getSampleCode(project.getString(NodeConstants.COL_ID)));
		helper
				.testCreateJsonEntity(helper.getServletPrefix()
						+"/code",
						"{\"name\":\"Coexpession Network\",  "
								+ " \"description\": \"foo\", \"parentId\":\""+project.getString(NodeConstants.COL_ID)+"\"}");

		JSONObject results = helper.testGetJsonEntities(helper
				.getServletPrefix()
				+ "/project/" + project.getString(NodeConstants.COL_ID) + "/code", null, null,
				null, null);
		assertEquals(3, results.getInt("totalNumberOfResults"));
		assertEquals(3, results.getJSONArray("results").length());
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.PREVIOUS_PAGE_FIELD));
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.NEXT_PAGE_FIELD));

		assertExpectedCodesProperties(results.getJSONArray("results"));
	}

	/*****************************************************************************************************
	 * Bad parameters tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.CodeController#createChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInvalidModelCreateCode() throws Exception {

		JSONObject error = helper
				.testCreateJsonEntityShouldFail(helper.getServletPrefix()
						+"/code",
						"{\"name\": \"Bayesian Network\", "
								+ "\"BOGUS\":\"this does not match our model object\"}",
						HttpStatus.BAD_REQUEST);

		// The response should be something like: {"reason":"Unrecognized field
		// \"BOGUS\"
		// (Class org.sagebionetworks.repo.model.Code), not marked as
		// ignorable\n at
		// [Source:
		// org.springframework.mock.web.DelegatingServletInputStream@2501e081;
		// line: 1, column: 19]"}

		String reason = error.getString("reason");
		assertTrue(reason, reason.matches("(?s).*BOGUS.*"));
		assertTrue(reason, reason.matches("(?s).*schema.*"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.CodeController#updateChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateCodeConflict() throws Exception {
		// Create a code
		JSONObject newCode = helper
				.testCreateJsonEntity(helper.getServletPrefix()
						+"/code",
						"{\"name\":\"MouseCross genetic data\", "
								+ " \"description\": \"foo\",  \"parentId\":\"" +project.getString(NodeConstants.COL_ID)+
										"\"}");

		// Get that code
		JSONObject code = helper.testGetJsonEntity(newCode.getString("uri"));
		assertEquals(newCode.getString(NodeConstants.COL_ID), code.getString(NodeConstants.COL_ID));
		assertEquals("MouseCross genetic data", code.getString("name"));

		// Modify that code
		code.put("name", "MouseX genetic data");
		JSONObject updatedCode = helper.testUpdateJsonEntity(code);
		assertEquals("MouseX genetic data", updatedCode.getString("name"));

		// Modify the code we got earlier a second time
		code.put("name", "CONFLICT MouseX genetic data");
		JSONObject error = helper.testUpdateJsonEntityShouldFail(code,
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
	 * {@link org.sagebionetworks.repo.web.controller.CodeController#getChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetNonExistentCode() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/code", CodeControllerTest.getSampleCode(project.getString(NodeConstants.COL_ID)));

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testGetJsonEntityShouldFail(results
				.getString("uri"), HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.CodeController#updateChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateNonExistentCode() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/code", CodeControllerTest.getSampleCode(project.getString(NodeConstants.COL_ID)));
		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testUpdateJsonEntityShouldFail(results,
				HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.CodeController#deleteChildEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteNonExistentCode() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix()+ "/code", CodeControllerTest.getSampleCode(project.getString(NodeConstants.COL_ID)));

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testDeleteJsonEntityShouldFail(results
				.getString("uri"), HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/*****************************************************************************************************
	 * Code-specific helpers
	 */

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedCodesProperties(JSONArray results)
			throws Exception {
		for (int i = 0; i < results.length(); i++) {
			JSONObject code = results.getJSONObject(i);
			assertExpectedCodeProperties(code);
		}
	}

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedCodeProperties(JSONObject results)
			throws Exception {
		// Check required properties
		assertTrue(results.has("name"));
		assertFalse("null".equals(results.getString("name")));

		// Check immutable system-defined properties
		assertTrue(results.has("annotations"));
		assertTrue(results.getString("annotations").endsWith("/annotations"));
		assertTrue(results.has("createdOn"));
		assertFalse("null".equals(results.getString("createdOn")));

//		// Check that optional properties that receive default values
		assertTrue(results.has("versionNumber"));
		Long value = results.getLong("versionNumber");
		assertFalse("null".equals(value));
	}

}
