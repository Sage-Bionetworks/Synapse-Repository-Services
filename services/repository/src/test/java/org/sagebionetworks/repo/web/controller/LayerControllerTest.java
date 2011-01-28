/**
 *
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Unit tests for the DatasetLayer CRUD operations exposed by the
 * DatasetController with JSON request and response encoding.
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
@ContextConfiguration(locations = { "classpath:repository-context.xml",
		"classpath:repository-servlet.xml" })
public class LayerControllerTest {

	private static final Logger log = Logger
			.getLogger(LayerControllerTest.class.getName());
	private Helpers helper = new Helpers();
	private DispatcherServlet servlet;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		servlet = helper.setUp();
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
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#sanityCheck(org.springframework.ui.ModelMap)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSanityCheck() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/dataset/123/layer/test");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		// The response should be: {"hello":"REST for Dataset Layers rocks"}
		assertEquals("REST for Dataset Layers rocks", results
				.getString("hello"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#createEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateLayer() throws Exception {
		JSONObject dataset = helper.testCreateJsonEntity("/dataset",
				"{\"name\":\"DeLiver\"}");

		// TODO use dataset layer URI
		JSONObject results = helper
				.testCreateJsonEntity(
						"/dataset/" + dataset.getString("id") + "/layer",
						"{\"name\":\"DeLiver expression data\", \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		// Check required properties
		assertEquals("DeLiver expression data", results.getString("name"));

		assertExpectedLayerProperties(results);
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
	public void testGetLayer() throws Exception {
		JSONObject dataset = helper.testCreateJsonEntity("/dataset",
				"{\"name\":\"DeLiver\"}");

		// TODO use dataset layer URI
		JSONObject newLayer = helper
				.testCreateJsonEntity(
						"/dataset/" + dataset.getString("id") + "/layer",
						"{\"name\":\"DeLiver expression data\", \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		// Get the layer
		JSONObject results = helper
				.testGetJsonEntity(newLayer.getString("uri"));

		assertEquals(newLayer.getString("id"), results.getString("id"));
		assertEquals("DeLiver expression data", results.getString("name"));

		assertExpectedLayerProperties(results);
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateLayer() throws Exception {
		JSONObject dataset = helper.testCreateJsonEntity("/dataset",
				"{\"name\":\"DeLiver\"}");

		// TODO use dataset layer URI
		JSONObject newLayer = helper
				.testCreateJsonEntity(
						"/dataset/" + dataset.getString("id") + "/layer",
						"{\"name\":\"DeLiver expression data\", \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		// Get the layer
		JSONObject layer = helper.testGetJsonEntity(newLayer.getString("uri"));

		assertEquals(newLayer.getString("id"), layer.getString("id"));
		assertEquals("DeLiver expression data", layer.getString("name"));

		// Modify that layer
		layer.put("name", "DeLiver clinical data");
		JSONObject updatedLayer = helper.testUpdateJsonEntity(layer);
		assertExpectedLayerProperties(updatedLayer);

		// Check that the update response reflects the change
		assertEquals("DeLiver clinical data", updatedLayer.getString("name"));

		// Now make sure the stored one reflects the change too
		JSONObject storedLayer = helper.testGetJsonEntity(newLayer
				.getString("uri"));
		assertEquals("DeLiver clinical data", storedLayer.getString("name"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#deleteEntity(java.lang.String)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteLayer() throws Exception {

		JSONObject dataset = helper.testCreateJsonEntity("/dataset",
				"{\"name\":\"DeLiver\"}");

		// TODO use dataset layer URI
		JSONObject newLayer = helper
				.testCreateJsonEntity(
						"/dataset/" + dataset.getString("id") + "/layer",
						"{\"name\":\"DeLiver expression data\", \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		helper.testDeleteJsonEntity(newLayer.getString("uri"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#getEntities}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testGetLayers() throws Exception {

		JSONObject dataset = helper.testCreateJsonEntity("/dataset",
				"{\"name\":\"DeLiver\"}");

		// TODO use dataset layer URI
		helper
				.testCreateJsonEntity(
						"/dataset/" + dataset.getString("id") + "/layer",
						"{\"name\":\"DeLiver genetic data\", \"description\": \"foo\", \"releaseNotes\":\"bar\"}");
		helper
				.testCreateJsonEntity(
						"/dataset/" + dataset.getString("id") + "/layer",
						"{\"name\":\"DeLiver expression data\", \"description\": \"foo\", \"releaseNotes\":\"bar\"}");
		helper
				.testCreateJsonEntity(
						"/dataset/" + dataset.getString("id") + "/layer",
						"{\"name\":\"DeLiver clinical data\", \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		JSONObject results = helper.testGetJsonEntities("/dataset/"
				+ dataset.getString("id") + "/layer", null, null, null, null);
		assertEquals(3, results.getInt("totalNumberOfResults"));
		assertEquals(3, results.getJSONArray("results").length());
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.PREVIOUS_PAGE_FIELD));
		assertFalse(results.getJSONObject("paging").has(
				PaginatedResults.NEXT_PAGE_FIELD));

		assertExpectedLayersProperties(results.getJSONArray("results"));
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.DatasetController#updateEntityAnnotations}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testUpdateDatasetAnnotations() throws Exception {

		JSONObject dataset = helper.testCreateJsonEntity("/dataset",
		"{\"name\":\"DeLiver\"}");

		// TODO use dataset layer URI
		JSONObject newLayer = helper
		.testCreateJsonEntity(
				"/dataset/" + dataset.getString("id") + "/layer",
		"{\"name\":\"DeLiver genetic data\", \"description\": \"foo\", \"releaseNotes\":\"bar\"}");

		// Get our empty annotations container
		JSONObject annotations = helper.testGetJsonEntity(newLayer
				.getString("annotations"));

		// Add some string annotations
		JSONObject stringAnnotations = annotations
				.getJSONObject("stringAnnotations");
		String tissues[] = { "liver", "brain" };
		stringAnnotations.put("tissues", tissues);
		String summary[] = { "this is a summary" };
		stringAnnotations.put("summary", summary);

		// Add some numeric annotations
		//
		// Note that we could send these numbers as floats but when the
		// serialized version
		// comes back from the service, Jackson will always treat them as double
		// See http://wiki.fasterxml.com/JacksonInFiveMinutes
		JSONObject floatAnnotations = annotations
				.getJSONObject("floatAnnotations");
		Double pValues[] = { new Double(0.987), new Double(0) };
		floatAnnotations.put("pValues", pValues);
		Double numSamples[] = { new Double(3000) };
		floatAnnotations.put("numSamples", numSamples);

		//
		// Add some date annotations
		//
		// When dates are serialized to be sent to the service can dates
		// expressed in epoch time (which is a Long)
		// or ISO-8601 (which is a string).
		//
		// When dates are returned by the service they are always serialized as
		// epoch time.
		// See
		// http://wiki.fasterxml.com/JacksonFAQDateHandling?highlight=(jackson)|(date)

		Date now = new Date();
		DateTime aWhileBack = new DateTime("2010-10-01");

		Long curationEvents[] = { now.getTime(), now.getTime(),
				aWhileBack.getMillis() };
		JSONObject dateAnnotations = annotations
				.getJSONObject("dateAnnotations");
		dateAnnotations.put("curationEvents", curationEvents);

		Long clinicalTrialStartDate[] = { now.getTime() };
		dateAnnotations.put("clinicalTrialStartDate", clinicalTrialStartDate);

		Long epochDates[] = { now.getTime(), aWhileBack.getMillis() };
		dateAnnotations.put("epochDates", epochDates);

		DateTime isoDates[] = { aWhileBack };
		dateAnnotations.put("isoDates", isoDates);
		Long isoDatesAsLong[] = { aWhileBack.getMillis() }; // for the assertion
		// below

		JSONObject results = helper.testUpdateJsonEntity(annotations);

		// Check the update response
		helper.assertJSONArrayEquals(summary, results.getJSONObject(
				"stringAnnotations").getJSONArray("summary"));
		helper.assertJSONArrayEquals(tissues, results.getJSONObject(
				"stringAnnotations").getJSONArray("tissues"));
		helper.assertJSONArrayEquals(pValues, results.getJSONObject(
				"floatAnnotations").getJSONArray("pValues"));
		helper.assertJSONArrayEquals(numSamples, results.getJSONObject(
				"floatAnnotations").getJSONArray("numSamples"));
		helper.assertJSONArrayEquals(curationEvents, results.getJSONObject(
				"dateAnnotations").getJSONArray("curationEvents"));
		helper.assertJSONArrayEquals(clinicalTrialStartDate, results
				.getJSONObject("dateAnnotations").getJSONArray(
						"clinicalTrialStartDate"));
		// These are sent serialized as Longs and come back serialized as Longs
		helper.assertJSONArrayEquals(epochDates, results.getJSONObject(
				"dateAnnotations").getJSONArray("epochDates"));
		// These are sent serialized as Strings and come back serialized as
		// Longs
		helper.assertJSONArrayEquals(isoDatesAsLong, results.getJSONObject(
				"dateAnnotations").getJSONArray("isoDates"));

		// Now check that we correctly persisted them for real
		JSONObject storedAnnotations = helper.testGetJsonEntity(newLayer
				.getString("annotations"));
		helper.assertJSONArrayEquals(summary, storedAnnotations.getJSONObject(
				"stringAnnotations").getJSONArray("summary"));
		helper.assertJSONArrayEquals(tissues, storedAnnotations.getJSONObject(
				"stringAnnotations").getJSONArray("tissues"));
		helper.assertJSONArrayEquals(pValues, storedAnnotations.getJSONObject(
				"floatAnnotations").getJSONArray("pValues"));
		helper.assertJSONArrayEquals(numSamples, storedAnnotations
				.getJSONObject("floatAnnotations").getJSONArray("numSamples"));
		helper.assertJSONArrayEquals(curationEvents, results.getJSONObject(
				"dateAnnotations").getJSONArray("curationEvents"));
		helper.assertJSONArrayEquals(clinicalTrialStartDate, results
				.getJSONObject("dateAnnotations").getJSONArray(
						"clinicalTrialStartDate"));
		// These are sent serialized as Longs and come back serialized as Longs
		helper.assertJSONArrayEquals(epochDates, results.getJSONObject(
				"dateAnnotations").getJSONArray("epochDates"));
		// These are sent serialized as Strings and come back serialized as
		// Longs
		helper.assertJSONArrayEquals(isoDatesAsLong, results.getJSONObject(
				"dateAnnotations").getJSONArray("isoDates"));
	}

	// /*****************************************************************************************************
	// * Bad parameters tests
	// */
	//
	// /**
	// * Test method for
	// * {@link
	// org.sagebionetworks.repo.web.controller.DatasetController#createEntity}
	// * .
	// *
	// * @throws Exception
	// */
	// @Test
	// public void testInvalidModelCreateDataset() throws Exception {
	//
	// JSONObject error = helper
	// .testCreateJsonEntityShouldFail(
	// "/dataset",
	// "{\"name\": \"DeLiver\", \"BOGUS\":\"this does not match our model object\"}",
	// HttpStatus.BAD_REQUEST);
	//
	// // The response should be something like: {"reason":"Unrecognized field
	// // \"BOGUS\"
	// // (Class org.sagebionetworks.repo.model.Dataset), not marked as
	// // ignorable\n at
	// // [Source:
	// // org.springframework.mock.web.DelegatingServletInputStream@2501e081;
	// // line: 1, column: 19]"}
	//
	// String reason = error.getString("reason");
	// assertTrue(reason.matches("(?s).*\"BOGUS\".*"));
	// assertTrue(reason.matches("(?s).*not marked as ignorable.*"));
	// }
	//
	// /**
	// * Test method for
	// * {@link
	// org.sagebionetworks.repo.web.controller.DatasetController#createEntity}
	// * .
	// *
	// * @throws Exception
	// */
	// @Test
	// public void testMissingRequiredFieldCreateDataset() throws Exception {
	//
	// JSONObject error = helper.testCreateJsonEntityShouldFail("/dataset",
	// "{\"version\": \"1.0.0\"}", HttpStatus.BAD_REQUEST);
	//
	// assertEquals("'name' is a required property for Dataset", error
	// .getString("reason"));
	// }
	//
	// /**
	// * Test method for
	// * {@link
	// org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}
	// * .
	// *
	// * @throws Exception
	// */
	// @Test
	// public void testMissingRequiredFieldUpdateDataset() throws Exception {
	// // Create a dataset
	// JSONObject newDataset = helper.testCreateJsonEntity("/dataset",
	// "{\"name\":\"MouseCross\"}");
	//
	// // Get that dataset
	// JSONObject dataset = helper.testGetJsonEntity(newDataset
	// .getString("uri"));
	// assertEquals(newDataset.getString("id"), dataset.getString("id"));
	// assertEquals("MouseCross", dataset.getString("name"));
	//
	// // Modify that dataset to make it invalid
	// dataset.remove("name");
	// JSONObject error = helper.testUpdateJsonEntityShouldFail(dataset,
	// HttpStatus.BAD_REQUEST);
	//
	// assertEquals("'name' is a required property for Dataset", error
	// .getString("reason"));
	// }
	//
	// /**
	// * Test method for
	// * {@link
	// org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}
	// * .
	// *
	// * @throws Exception
	// */
	// @Test
	// public void testUpdateDatasetConflict() throws Exception {
	// // Create a dataset
	// JSONObject newDataset = helper.testCreateJsonEntity("/dataset",
	// "{\"name\":\"MouseCross\"}");
	// // Get that dataset
	// JSONObject dataset = helper.testGetJsonEntity(newDataset
	// .getString("uri"));
	// assertEquals(newDataset.getString("id"), dataset.getString("id"));
	// assertEquals("MouseCross", dataset.getString("name"));
	// // Modify that dataset
	// dataset.put("name", "MouseX");
	// JSONObject updatedDataset = helper.testUpdateJsonEntity(dataset);
	// assertEquals("MouseX", updatedDataset.getString("name"));
	//
	// // Modify the dataset we got earlier a second time
	// dataset.put("name", "CONFLICT MouseX");
	// JSONObject error = helper.testUpdateJsonEntityShouldFail(dataset,
	// HttpStatus.PRECONDITION_FAILED);
	//
	// String reason = error.getString("reason");
	// assertTrue(reason
	// .matches("entity with id .* was updated since you last fetched it, retrieve it again and reapply the update"));
	// }
	//
	// /*****************************************************************************************************
	// * Not Found Tests
	// */
	//
	// /**
	// * Test method for
	// * {@link
	// org.sagebionetworks.repo.web.DAOControllerImp#getEntity(java.lang.String,
	// javax.servlet.http.HttpServletRequest)}
	// * .
	// *
	// * @throws Exception
	// */
	// @Test
	// public void testGetNonExistentDataset() throws Exception {
	// JSONObject results = helper.testCreateJsonEntity("/dataset",
	// "{\"name\":\"DeLiver\"}");
	//
	// helper.testDeleteJsonEntity(results.getString("uri"));
	//
	// JSONObject error = helper.testGetJsonEntityShouldFail(results
	// .getString("uri"), HttpStatus.NOT_FOUND);
	// assertEquals(
	// "The resource you are attempting to retrieve cannot be found",
	// error.getString("reason"));
	// }
	//
	// /**
	// * Test method for
	// * {@link
	// org.sagebionetworks.repo.web.controller.DatasetController#updateEntityAnnotations}
	// * .
	// *
	// * @throws Exception
	// */
	// @Test
	// public void testGetNonExistentDatasetAnnotations() throws Exception {
	//
	// // Load up a dataset
	// JSONObject newDataset = helper.testCreateJsonEntity("/dataset",
	// "{\"name\":\"MouseCross\"}");
	// // Get our empty annotations container
	// JSONObject annotations = helper.testGetJsonEntity(newDataset
	// .getString("annotations"));
	//
	// // Delete our dataset
	// helper.testDeleteJsonEntity(newDataset.getString("uri"));
	//
	// JSONObject error = helper.testGetJsonEntityShouldFail(annotations
	// .getString("uri"), HttpStatus.NOT_FOUND);
	// assertEquals(
	// "The resource you are attempting to retrieve cannot be found",
	// error.getString("reason"));
	//
	// }
	//
	// /**
	// * Test method for
	// * {@link
	// org.sagebionetworks.repo.web.controller.DatasetController#updateEntity}
	// * .
	// *
	// * @throws Exception
	// */
	// @Test
	// public void testUpdateNonExistentDataset() throws Exception {
	// JSONObject results = helper.testCreateJsonEntity("/dataset",
	// "{\"name\":\"DeLiver\"}");
	//
	// helper.testDeleteJsonEntity(results.getString("uri"));
	//
	// JSONObject error = helper.testUpdateJsonEntityShouldFail(results,
	// HttpStatus.NOT_FOUND);
	// assertEquals(
	// "The resource you are attempting to retrieve cannot be found",
	// error.getString("reason"));
	// }
	//
	// /**
	// * Test method for
	// * {@link
	// org.sagebionetworks.repo.web.controller.DatasetController#deleteEntity(java.lang.String)}
	// * .
	// *
	// * @throws Exception
	// */
	// @Test
	// public void testDeleteNonExistentDataset() throws Exception {
	// JSONObject results = helper.testCreateJsonEntity("/dataset",
	// "{\"name\":\"DeLiver\"}");
	//
	// helper.testDeleteJsonEntity(results.getString("uri"));
	//
	// JSONObject error = helper.testDeleteJsonEntityShouldFail(results
	// .getString("uri"), HttpStatus.NOT_FOUND);
	// assertEquals(
	// "The resource you are attempting to retrieve cannot be found",
	// error.getString("reason"));
	// }

	/*****************************************************************************************************
	 * Layer-specific helpers
	 */

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedLayersProperties(JSONArray results)
			throws Exception {
		for (int i = 0; i < results.length(); i++) {
			JSONObject layer = results.getJSONObject(i);
			assertExpectedLayerProperties(layer);
		}
	}

	/**
	 * @param results
	 * @throws Exception
	 */
	public static void assertExpectedLayerProperties(JSONObject results)
			throws Exception {
		// Check required properties
		assertTrue(results.has("name"));
		assertFalse("null".equals(results.getString("name")));

		// Check immutable system-defined properties
		assertTrue(results.has("annotations"));
		assertFalse("null".equals(results.getString("annotations")));
		assertTrue(results.has("creationDate"));
		assertFalse("null".equals(results.getString("creationDate")));

		// Check that optional properties that receive default values
		assertTrue(results.has("version"));

		assertFalse("null".equals(results.getString("version")));
	}
}
