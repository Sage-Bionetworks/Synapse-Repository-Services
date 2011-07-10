package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
public class EulaControllerTest {

	/**
	 * Sample eula for use in unit tests, note that the parentId property is
	 * missing and necessary to be a valid location
	 */
	public static String SAMPLE_EULA = "{\"name\":\"TCGA Redistribution Use Agreement\", "
			+ "\"agreement\":\"The recipient acknowledges that the data herein is provided by TCGA and not SageBionetworks and must abide by ...\"}";

	@Autowired
	private Helpers helper;
	private JSONObject dataset;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		helper.setUp();

		dataset = helper.testCreateJsonEntity(helper.getServletPrefix()
				+ "/dataset", DatasetControllerTest.SAMPLE_DATASET);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	/*************************************************************************************************************************
	 * Happy case tests, most are covered by the DefaultController tests and do not need to be repeated here
	 */

	/**
	 * @throws Exception
	 */
	@Ignore
	@Test
	public void testCreateEula() throws Exception {
		JSONObject eula = helper.testCreateJsonEntity(helper
				.getServletPrefix()
				+ "/eula", SAMPLE_EULA);

		// Check properties
		assertEquals("The recipient acknowledges that the data herein is provided by TCGA and not SageBionetworks and must abide by ...", eula.getString("agreement"));
		assertEquals("TCGA Redistribution Use Agreement", eula.getString("name"));

		assertExpectedEulaProperties(eula);

		JSONObject storedEula = helper.testGetJsonEntity(eula
				.getString("uri"));
		assertNotNull(storedEula);
		assertEquals(eula.getString("agreement"), storedEula.getString("agreement"));
		assertEquals(eula.getString("name"), storedEula.getString("name"));

	}

	/*****************************************************************************************************
	 * Eula-specific helpers
	 */

	/**
	 * @param eula
	 * @throws Exception
	 */
	public static void assertExpectedEulaProperties(JSONObject eula) throws Exception {
		// Check required properties
		assertTrue(eula.has("name"));
		assertFalse("null".equals(eula.getString("name")));
		assertTrue(eula.has("agreement"));
		assertFalse("null".equals(eula.getString("agreement")));
	}
}
