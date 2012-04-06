package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit tests for the Dataset CRUD operations exposed by the DatasetController
 * with JSON request and response encoding.
 * <p>
 * 
 * Note that test logic and assertions common to operations for all DAO-backed
 * entities can be found in the Helpers class. What follows are test cases that
 * make use of that generic test logic with some assertions specific to
 * datasets.
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class DatasetControllerTest {

	public static final String CREATION_DATE = "createdOn";

	@Autowired
	private Helpers helper;

	/**
	 * A few dataset properties for use in creating a new dataset object for
	 * unit tests
	 */
	public static final String SAMPLE_DATASET_1 = "{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"DeLiver\", \"parentId\":\"";
	
	private JSONObject project;
	
	/**
	 * Build a sample dataset.
	 * @param parentId
	 * @return
	 */
	public static String getSampleDataset(String parentId){
		StringBuilder builder = new StringBuilder();
		builder.append(SAMPLE_DATASET_1);
		builder.append(parentId);
		builder.append("\"}");
		return builder.toString();
	}
	
	public static final String SAMPLE_PROJECT = "{\"entityType\":\"org.sagebionetworks.repo.model.Project\", \"name\":\"RootProject\"}";

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();
		project = helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
					DatasetControllerTest.SAMPLE_PROJECT);
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
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testCreateDataset() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"DeLiver\" ,\"parentId\":\""
				+ project.getString("id") + "\" }");

		// Check required properties
		assertEquals("DeLiver", results.getString("name"));

		assertExpectedDatasetProperties(results);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testGetDataset() throws Exception {
		// Load up a few datasets
		helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"DeLiver\" ,\"parentId\":\""+project.getString("id")+"\"}");
		helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"Harvard Brain\" ,\"parentId\":\""+project.getString("id")+"\"}");
		JSONObject newDataset = helper.testCreateJsonEntity(helper.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"MouseCross\" ,\"parentId\":\""+project.getString("id")+"\"}");

		JSONObject results = helper.testGetJsonEntity(newDataset
				.getString("uri"));

		assertEquals(newDataset.getString("id"), results.getString("id"));
		assertEquals("MouseCross", results.getString("name"));

		assertExpectedDatasetProperties(results);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetAnnotationsController#updateEntityAnnotations}
	 * and
	 * {@link org.sagebionetworks.repo.web.controller.DatasetAnnotationsController#getEntityAnnotations}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testUpdateDatasetAnnotations() throws Exception {

		// Load up a few datasets
		JSONObject newDataset = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ UrlHelpers.ENTITY, "{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"MouseCross\" ,\"parentId\":\""+project.getString("id")+"\"}");

		// Get our empty annotations container
		helper.testEntityAnnotations(newDataset.getString("annotations"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testUpdateDataset() throws Exception {
		// Load up a few datasets
		helper.testCreateJsonEntity(helper.getServletPrefix()+ UrlHelpers.ENTITY, 
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"DeLiver\",\"parentId\":\""+project.getString("id")+"\"}");
		helper.testCreateJsonEntity(helper.getServletPrefix()+ UrlHelpers.ENTITY, 
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"Harvard Brain\",\"parentId\":\""+project.getString("id")+"\"}");
		JSONObject newDataset = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"MouseCross\",\"parentId\":\""+project.getString("id")+"\"}");

		// Get one dataset
		JSONObject dataset = helper.testGetJsonEntity(newDataset
				.getString("uri"));
		assertEquals(newDataset.getString("id"), dataset.getString("id"));
		assertEquals("MouseCross", dataset.getString("name"));

		// Modify that dataset
		dataset.put("name", "MouseX");
		JSONObject updatedDataset = helper.testUpdateJsonEntity(dataset);
		assertExpectedDatasetProperties(updatedDataset);

		// Check that the update response reflects the change
		assertEquals("MouseX", updatedDataset.getString("name"));

		// Now make sure the stored one reflects the change too
		JSONObject storedDataset = helper.testGetJsonEntity(newDataset
				.getString("uri"));
		assertEquals("MouseX", storedDataset.getString("name"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testChangeDatasetParentID() throws Exception {
		// Load up a few datasets
		//testCreateJsonEntity takes a requestURL, and JSON content
		//and it returns a JSONObject that represents the Node it created and strored in db
		helper.testCreateJsonEntity(helper.getServletPrefix()+ UrlHelpers.ENTITY, 
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"DeLiver\",\"parentId\":\""+project.getString("id")+"\"}");
		helper.testCreateJsonEntity(helper.getServletPrefix()+ UrlHelpers.ENTITY, 
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"Harvard Brain\",\"parentId\":\""+project.getString("id")+"\"}");
		
		//making a new JSONObject where the name is MouseCross, and save the return object
		JSONObject newDataset = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"MouseCross\",\"parentId\":\""+project.getString("id")+"\"}");
		
		//make a new project where name is ContainerForProject
		JSONObject newProject = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Project\", \"name\":\"ContainerForProject\",\"parentId\":\""+project.getString("id")+"\"}");

		//retrieve the "newDataset" entity from the db
		JSONObject dataset = helper.testGetJsonEntity(newDataset
				.getString("uri"));
		assertEquals(newDataset.getString("id"), dataset.getString("id"));
		assertEquals("MouseCross", dataset.getString("name"));
		
		//change the parent id of the "dataset" to that of the "newProject"
		dataset.put("parentId", newProject.get("id"));
		
		//send the modified "dataset" to be officially updated and saved in db
		JSONObject updatedDataset = helper.testUpdateJsonEntity(dataset);
		assertExpectedDatasetProperties(updatedDataset);
		
		//check that parentID value sent over was not null
		assertTrue(updatedDataset.get("parentId") != null);
		
		//check that update response reflects the change
		assertEquals(newProject.get("id"), updatedDataset.get("parentId"));
		
		//now make sure change is reflected back in stored object
		JSONObject storedDataset = helper.testGetJsonEntity(newDataset
				.getString("uri"));
		assertEquals(newProject.get("id"), storedDataset.get("parentId"));		
	}
	
	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testUpdateNewlyCreatedDataset() throws Exception {
		JSONObject newDataset = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"MouseCross\",\"parentId\":\""+project.getString("id")+"\"}");

		// Modify the newly created dataset
		newDataset.put("name", "MouseX");
		JSONObject updatedDataset = helper.testUpdateJsonEntity(newDataset);
		assertExpectedDatasetProperties(updatedDataset);

		// Check that the update response reflects the change
		assertEquals("MouseX", updatedDataset.getString("name"));

		// Now make sure the stored one reflects the change too
		JSONObject storedDataset = helper.testGetJsonEntity(newDataset
				.getString("uri"));
		assertEquals("MouseX", storedDataset.getString("name"));
	}
	
	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#deleteEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testDeleteDataset() throws Exception {
		// Load up a few datasets
		helper.testCreateJsonEntity(helper.getServletPrefix()+ UrlHelpers.ENTITY, 
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"DeLiver\",\"parentId\":\""+project.getString("id")+"\"}");
		helper.testCreateJsonEntity(helper.getServletPrefix()+ UrlHelpers.ENTITY, 
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"Harvard Brain\",\"parentId\":\""+project.getString("id")+"\"}");
		JSONObject newDataset = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"MouseCross\",\"parentId\":\""+project.getString("id")+"\"}");
		helper.testDeleteJsonEntity(newDataset.getString("uri"));
	}

	/*****************************************************************************************************
	 * Bad parameters tests
	 */

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testInvalidModelCreateDataset() throws Exception {

		JSONObject error = helper
				.testCreateJsonEntityShouldFail(
						helper.getServletPrefix()+ UrlHelpers.ENTITY, 
						"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\": \"DeLiver\", \"BOGUS\":\"this does not match our model object\",\"parentId\":\""+project.getString("id")+"\"}",
						HttpStatus.BAD_REQUEST);

		// The response should be something like: {"reason":"Unrecognized field
		// \"BOGUS\"
		// (Class org.sagebionetworks.repo.model.Dataset), not marked as
		// ignorable\n at
		// [Source:
		// org.springframework.mock.web.DelegatingServletInputStream@2501e081;
		// line: 1, column: 19]"}

		String reason = error.getString("reason");
		assertTrue(reason,reason.matches("(?s).*BOGUS.*"));
		assertTrue(reason, reason.matches("(?s).*is not defined in the schema.*"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore // when a name is null the id will be used as the name
	@Test
	public void testMissingRequiredFieldCreateDataset() throws Exception {

		JSONObject error = helper.testCreateJsonEntityShouldFail(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"version\": \"1.0.0\",\"parentId\":\""+project.getString("id")+"\"}",
				HttpStatus.BAD_REQUEST);

		assertEquals("Node.name cannot be null", error
				.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore // when a name is null the id will be used as the name
	@Test
	public void testMissingRequiredFieldUpdateDataset() throws Exception {
		// Create a dataset
		JSONObject newDataset = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"MouseCross\",\"parentId\":\""+project.getString("id")+"\"}");

		// Get that dataset
		JSONObject dataset = helper.testGetJsonEntity(newDataset
				.getString("uri"));
		assertEquals(newDataset.getString("id"), dataset.getString("id"));
		assertEquals("MouseCross", dataset.getString("name"));

		// Modify that dataset to make it invalid
		dataset.remove("name");
		JSONObject error = helper.testUpdateJsonEntityShouldFail(dataset,
				HttpStatus.BAD_REQUEST);

		assertEquals("Node.name cannot be null", error
				.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testUpdateDatasetConflict() throws Exception {
		// Create a dataset
		JSONObject newDataset = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"MouseCross\",\"parentId\":\""+project.getString("id")+"\"}");
		// Get that dataset
		JSONObject dataset = helper.testGetJsonEntity(newDataset
				.getString("uri"));
		assertEquals(newDataset.getString("id"), dataset.getString("id"));
		assertEquals("MouseCross", dataset.getString("name"));
		// Modify that dataset
		dataset.put("name", "MouseX");
		JSONObject updatedDataset = helper.testUpdateJsonEntity(dataset);
		assertEquals("MouseX", updatedDataset.getString("name"));

		// Modify the dataset we got earlier a second time
		dataset.put("name", "CONFLICT MouseX");
		JSONObject error = helper.testUpdateJsonEntityShouldFail(dataset,
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
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.EntityControllerImp#getEntity} .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testGetNonExistentDataset() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"DeLiver\",\"parentId\":\""+project.getString("id")+"\"}");

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testGetJsonEntityShouldFail(results
				.getString("uri"), HttpStatus.NOT_FOUND);
		// TODO: Confirm that new message is expected one
		assertEquals(
				"Cannot find a node with id: " + results.getString("id"),
				error.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetAnnotationsController#updateEntityAnnotations}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testGetNonExistentDatasetAnnotations() throws Exception {

		// Load up a dataset
		JSONObject newDataset = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"MouseCross\",\"parentId\":\""+project.getString("id")+"\"}");
		// Get our empty annotations container
		JSONObject annotations = helper.testGetJsonEntity(newDataset
				.getString("annotations"));

		// Delete our dataset
		helper.testDeleteJsonEntity(newDataset.getString("uri"));

		JSONObject error = helper.testGetJsonEntityShouldFail(annotations
				.getString("uri"), HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));

	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testUpdateNonExistentDataset() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"DeLiver\",\"parentId\":\""+project.getString("id")+"\"}");

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testUpdateJsonEntityShouldFail(results,
				HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#deleteEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testDeleteNonExistentDataset() throws Exception {
		JSONObject results = helper.testCreateJsonEntity(helper
				.getServletPrefix() + UrlHelpers.ENTITY,
				"{\"entityType\":\"org.sagebionetworks.repo.model.Study\", \"name\":\"DeLiver\",\"parentId\":\""+project.getString("id")+"\"}");

		helper.testDeleteJsonEntity(results.getString("uri"));

		JSONObject error = helper.testDeleteJsonEntityShouldFail(results
				.getString("uri"), HttpStatus.NOT_FOUND);
		assertEquals(
				"The resource you are attempting to access cannot be found",
				error.getString("reason"));
	}

	/*****************************************************************************************************
	 * Dataset-specific helpers
	 */

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedDatasetProperties(JSONObject results)
			throws Exception {
		// Check required properties
		assertTrue(results.has("name"));
		assertFalse("null".equals(results.getString("name")));

		// Check immutable system-defined properties
		assertTrue(results.has("annotations"));
		assertFalse("null".equals(results.getString("annotations")));
		assertTrue(results.has("layers"));
		assertFalse("null".equals(results.getString("layers")));

		assertTrue(results.has(CREATION_DATE));
		assertFalse("null".equals(results.getString(CREATION_DATE)));
		// Check that optional properties that receive default values
		assertTrue(results.has("versionNumber"));
		assertFalse("null".equals(results.getLong("versionNumber")));
	}
}
