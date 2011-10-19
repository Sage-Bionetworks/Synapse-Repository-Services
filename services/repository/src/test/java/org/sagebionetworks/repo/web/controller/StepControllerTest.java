package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.model.NodeConstants;
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
	private JSONObject datasetLocation;
	private JSONObject layer;
	private JSONObject layerLocation;

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
		datasetLocation = new JSONObject(LocationControllerTest.SAMPLE_LOCATION)
				.put(NodeConstants.COL_PARENT_ID, dataset.getString("id"));
		datasetLocation = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/location", datasetLocation.toString());

		layer = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/layer", LayerControllerTest.getSampleLayer(dataset
				.getString("id")));
		layerLocation = new JSONObject(LocationControllerTest.SAMPLE_LOCATION)
				.put(NodeConstants.COL_PARENT_ID, layer.getString("id"));
		layerLocation = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/location", layerLocation.toString());
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	/*************************************************************************************************************************
	 * Happy case tests, most are covered by the DefaultController tests and do
	 * not need to be repeated here
	 */

	/**
	 * @throws Exception
	 */
	@Test
	public void testCreateStep() throws Exception {
		JSONObject step = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/step", "{\"input\":[{\"targetId\":\""
				+ layer.getString("id") + "\"}], \"environmentDescriptors\":["
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
		assertEquals(4, storedStep.getJSONArray("environmentDescriptors").length());
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
