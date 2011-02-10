/**
 * 
 */
package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * @author deflaux
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:repository-context.xml",
		"classpath:repository-servlet.xml" })
public class QueryControllerTest {

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

		// Load up a few datasets with annotations
		for (int i = 0; i < DatasetsControllerTest.SAMPLE_DATASET_NAMES.length; i++) {
			JSONObject newDataset = helper.testCreateJsonEntity("/dataset",
					"{\"name\":\""
							+ DatasetsControllerTest.SAMPLE_DATASET_NAMES[i]
							+ "\"}");

			// Get our empty annotations container
			JSONObject annotations = helper.testGetJsonEntity(newDataset
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
			// comes back from the service, Jackson will always treat them as
			// double
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
			// When dates are returned by the service they are always serialized
			// as
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
			dateAnnotations.put("clinicalTrialStartDate",
					clinicalTrialStartDate);

			Long epochDates[] = { now.getTime(), aWhileBack.getMillis() };
			dateAnnotations.put("epochDates", epochDates);

			DateTime isoDates[] = { aWhileBack };
			dateAnnotations.put("isoDates", isoDates);

			helper.testUpdateJsonEntity(annotations);
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		helper.tearDown();
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query(java.lang.String, org.springframework.ui.ModelMap)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSelectStar() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/query");
		request.addParameter("query", "select * from dataset limit 30");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(DatasetsControllerTest.SAMPLE_DATASET_NAMES.length,
				results.length());
		// TODO add more tests here once we know what structure we want
	}

	/**
	 * Test method for
	 * {@link org.sagebionetworks.repo.web.controller.QueryController#query(java.lang.String, org.springframework.ui.ModelMap)}
	 * .
	 * 
	 * @throws Exception
	 */
	@Test
	public void testQuery() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/query");
		request.addParameter("query",
				"select * from dataset where name == \"Harvard Brain\"");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("we got 200 OK", 200, response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertEquals(1, results.length());
		// TODO add more tests here once we know what structure we want
	}
}
