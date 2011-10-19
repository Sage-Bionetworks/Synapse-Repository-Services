package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit tests for the Location CRUD operations on Step entities exposed by the
 * GenericController with JSON request and response encoding.
 * <p>
 * 
 * Note that test logic and assertions common to operations for all DAO-backed
 * entities can be found in the Helpers class. What follows are test cases that
 * make use of that generic test logic with some assertions specific to steps.
 * 
 * @author deflaux
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:test-context.xml" })
public class StepControllerTest {

	@Autowired
	private Helpers helper;
	private JSONObject project;
	private JSONObject dataset;
	private JSONObject layer;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();
		helper.useTestUser();

		project = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/project", DatasetControllerTest.SAMPLE_PROJECT);

		dataset = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/dataset", DatasetControllerTest.getSampleDataset(project
				.getString("id")));

		layer = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/layer", LayerControllerTest.getSampleLayer(dataset
				.getString("id")));
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
	public void testCreateStep() throws Exception {
		JSONObject step = helper
				.testCreateJsonEntity(
						helper.getServletPrefix() + "/step",
						"{\"input\":[{\"targetId\":\""
								+ layer.getString("id")
								+ "\"}], \"environmentDescriptors\":["
								+ "{\"type\":\"OS\",\"name\":\"x86_64-apple-darwin9.8.0/x86_64\",\"quantifier\":\"64-bit\"},"
								+ "{\"type\":\"application\",\"name\":\"R\",\"quantifier\":\"2.13.0\"},"
								+ "{\"type\":\"rPackage\",\"name\":\"synapseClient\",\"quantifier\":\"0.8-0\"},"
								+ "{\"type\":\"rPackage\",\"name\":\"Biobase\",\"quantifier\":\"2.12.2\"}"
								+ "]}");
		assertExpectedStepProperties(step);
		assertEquals(layer.getString("id"), step.getJSONArray("input")
				.getJSONObject(0).getString("targetId"));
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, (Long) step
				.getJSONArray("input").getJSONObject(0).getLong(
						"targetVersionNumber"));

		JSONObject storedStep = helper.testGetJsonEntity(step.getString("uri"));
		assertExpectedStepProperties(storedStep);
		assertEquals(layer.getString("id"), storedStep.getJSONArray("input")
				.getJSONObject(0).getString("targetId"));
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, (Long) storedStep
				.getJSONArray("input").getJSONObject(0).getLong(
						"targetVersionNumber"));
		assertEquals(4, storedStep.getJSONArray("environmentDescriptors")
				.length());
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testProvenanceSideEffects() throws Exception {
		JSONObject step = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/step", "{}");
		assertExpectedStepProperties(step);

		// Our extra parameter used to indicate the provenance record to update
		Map<String, String> extraParams = new HashMap<String, String>();
		extraParams.put(ServiceConstants.STEP_TO_UPDATE_PARAM, step
				.getString("id"));

		// Get a layer, side effect should add it to input references
		helper.testGetJsonEntity(layer.getString("uri"), extraParams);

		// Create a new layer, side effect should be to add it to output
		// references
		JSONObject outputLayer = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/layer", "{\"parentId\":\"" + dataset.getString("id")
				+ "\", \"type\":\"M\"}", extraParams);

		// TODO update a layer, version a layer, etc ...

		// Make sure those layers are not referred to by our step
		JSONObject storedStep = helper.testGetJsonEntity(step.getString("uri"));
		assertExpectedStepProperties(storedStep);
		assertEquals(layer.getString("id"), storedStep.getJSONArray("input")
				.getJSONObject(0).getString("targetId"));
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, (Long) storedStep
				.getJSONArray("input").getJSONObject(0).getLong(
						"targetVersionNumber"));
		assertEquals(outputLayer.getString("id"), storedStep.getJSONArray(
				"output").getJSONObject(0).getString("targetId"));
		assertEquals(NodeConstants.DEFAULT_VERSION_NUMBER, (Long) storedStep
				.getJSONArray("input").getJSONObject(0).getLong(
						"targetVersionNumber"));

	}

	/*****************************************************************************************************
	 * Step-specific helpers
	 */

	/**
	 * @param step
	 * @throws Exception
	 */
	public void assertExpectedStepProperties(JSONObject step) throws Exception {
		// Check required properties
		assertTrue(step.has("creationDate"));
		assertFalse("null".equals(step.getString("creationDate")));
		assertTrue(step.has("createdBy"));
		assertEquals(helper.getUserId(), step.getString("createdBy"));
		assertTrue(step.has("startDate"));
		assertFalse("null".equals(step.getString("startDate")));
		assertTrue(step.has("endDate"));
		assertTrue(step.has("description"));
		assertTrue(step.has("commandLine"));
		assertTrue(step.has("code"));
		assertTrue(step.has("input"));
		assertTrue(step.has("output"));
		assertTrue(step.has("environmentDescriptors"));
	}
}
