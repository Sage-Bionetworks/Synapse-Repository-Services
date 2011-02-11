package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.servlet.DispatcherServlet;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

/**
 * Test helper class to consolidate some generic testing code
 * 
 * @author deflaux
 * 
 */
public class Helpers {

	private static final Logger log = Logger.getLogger(Helpers.class.getName());

	private static final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig());

	private DispatcherServlet servlet = null;

	private static final int JSON_INDENT = 2;

	/**
	 * Setup up our mock datastore and servlet
	 * 
	 * @return the servlet
	 * @throws ServletException
	 */
	public DispatcherServlet setUp() throws ServletException {
		datastoreHelper.setUp();

		// Create a Spring MVC DispatcherServlet so that we can test our URL
		// mapping, request format, response format, and response status code.
		MockServletConfig servletConfig = new MockServletConfig("repository");
		servletConfig
				.addInitParameter("contextConfigLocation",
						"classpath:repository-context.xml,classpath:repository-servlet.xml");
		servlet = new DispatcherServlet();
		servlet.init(servletConfig);

		return servlet;
	}

	/**
	 * Teardown our datastore, deleting all items
	 */
	public void tearDown() {
		datastoreHelper.tearDown();
	}

	/**
	 * Creation of JSON entities
	 * 
	 * @param requestUrl
	 * @param jsonRequestContent
	 * @return the entity created
	 * @throws Exception
	 */
	public JSONObject testCreateJsonEntity(String requestUrl,
			String jsonRequestContent) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(jsonRequestContent.getBytes("UTF-8"));
		log.info("About to send: "
				+ new JSONObject(jsonRequestContent).toString(2));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.CREATED.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		log.info(results.toString(JSON_INDENT));

		// Check default properties
		assertExpectedEntityProperties(results);

		// Check our response headers
		String etagHeader = (String) response
				.getHeader(ServiceConstants.ETAG_HEADER);
		assertNotNull(etagHeader);
		assertEquals(etagHeader, results.getString("etag"));
		String locationHeader = (String) response
				.getHeader(ServiceConstants.LOCATION_HEADER);
		assertEquals(locationHeader, requestUrl + "/"
				+ URLEncoder.encode(results.getString("id"), "UTF-8"));
		assertEquals(locationHeader, results.getString("uri"));

		return results;
	}

	/**
	 * @param requestUrl
	 * @return the jsonRequestContent object holding the entity
	 * @throws Exception
	 */
	public JSONObject testGetJsonEntity(String requestUrl) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		log.info(results.toString(JSON_INDENT));

		// Check default properties
		assertExpectedEntityProperties(results);

		// Check our response headers
		String etagHeader = (String) response
				.getHeader(ServiceConstants.ETAG_HEADER);
		assertNotNull(etagHeader);
		assertEquals(etagHeader, results.getString("etag"));

		return results;
	}

	/**
	 * @param jsonEntity
	 * @return the json object holding the updated entity
	 * @throws Exception
	 */
	public JSONObject testUpdateJsonEntity(JSONObject jsonEntity)
			throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.addHeader(ServiceConstants.ETAG_HEADER, jsonEntity
				.getString("etag"));
		request.setRequestURI(jsonEntity.getString("uri"));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(jsonEntity.toString().getBytes("UTF-8"));
		log.info("About to send: " + jsonEntity.toString(JSON_INDENT));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		log.info(results.toString(JSON_INDENT));

		// Check default properties
		assertExpectedEntityProperties(results);
		assertEquals(jsonEntity.getString("id"), results.getString("id"));
		assertEquals(jsonEntity.getString("uri"), results.getString("uri"));

		// Check our response headers
		String etagHeader = (String) response
				.getHeader(ServiceConstants.ETAG_HEADER);
		assertNotNull(etagHeader);
		assertEquals(etagHeader, results.getString("etag"));

		// Make sure we got an updated etag
		assertFalse(etagHeader.equals(jsonEntity.getString("etag")));

		return results;
	}

	/**
	 * @param requestUrl
	 * @throws Exception
	 */
	public void testDeleteJsonEntity(String requestUrl) throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
		assertEquals("", response.getContentAsString());

		testGetJsonEntityShouldFail(requestUrl, HttpStatus.NOT_FOUND);
	}

	/**
	 * @param requestUrl
	 * @param offset
	 * @param limit
	 * @param sort
	 * @param ascending
	 * @return the response Json entity
	 * @throws Exception
	 */
	public JSONObject testGetJsonEntities(String requestUrl, Integer offset,
			Integer limit, String sort, Boolean ascending) throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		if (null != offset) {
			request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM,
					offset.toString());
		}
		if (null != limit) {
			request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, limit
					.toString());
		}
		if (null != sort) {
			request.setParameter(ServiceConstants.SORT_BY_PARAM, sort);
			if (null != ascending) {
				request.setParameter(ServiceConstants.ASCENDING_PARAM,
						ascending.toString());
			}
		}
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());

		// Check that the response has the correct structure
		assertTrue(results.has("totalNumberOfResults"));
		assertTrue(results.has("results"));
		assertTrue(results.has("paging"));

		assertExpectedEntitiesProperties(results.getJSONArray("results"));

		return results;
	}

	/**
	 * @param requestUrl
	 * @param jsonRequestContent
	 * @param status
	 * @return the error entity
	 * @throws Exception
	 */
	public JSONObject testCreateJsonEntityShouldFail(String requestUrl,
			String jsonRequestContent, HttpStatus status) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(jsonRequestContent.getBytes("UTF-8"));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertFalse(HttpStatus.OK.equals(response.getStatus()));
		assertEquals(status.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertTrue(results.has("reason"));
		assertFalse("null".equals(results.getString("reason")));

		return results;
	}

	/**
	 * @param requestUrl
	 * @param status
	 * @return the error entity
	 * @throws Exception
	 */
	public JSONObject testGetJsonEntityShouldFail(String requestUrl,
			HttpStatus status) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertFalse(HttpStatus.OK.equals(response.getStatus()));
		assertEquals(status.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertTrue(results.has("reason"));
		assertFalse("null".equals(results.getString("reason")));

		return results;
	}

	/**
	 * @param jsonEntity
	 * @param status
	 * @return the error entity
	 * @throws Exception
	 */
	public JSONObject testUpdateJsonEntityShouldFail(JSONObject jsonEntity,
			HttpStatus status) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.addHeader(ServiceConstants.ETAG_HEADER, jsonEntity
				.getString("etag"));
		request.setRequestURI(jsonEntity.getString("uri"));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(jsonEntity.toString().getBytes("UTF-8"));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertFalse(HttpStatus.OK.equals(response.getStatus()));
		assertEquals(status.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertTrue(results.has("reason"));
		assertFalse("null".equals(results.getString("reason")));

		return results;
	}

	/**
	 * @param requestUrl
	 * @param status
	 * @return the error entity
	 * @throws Exception
	 */
	public JSONObject testDeleteJsonEntityShouldFail(String requestUrl,
			HttpStatus status) throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertFalse(HttpStatus.OK.equals(response.getStatus()));
		assertEquals(status.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertTrue(results.has("reason"));
		assertFalse("null".equals(results.getString("reason")));

		return results;
	}

	/**
	 * @param requestUrl
	 * @param offset
	 * @param limit
	 * @param sort
	 * @param ascending
	 * @param status
	 * @return the response Json entity
	 * @throws Exception
	 */
	public JSONObject testGetJsonEntitiesShouldFail(String requestUrl,
			Integer offset, Integer limit, String sort, Boolean ascending,
			HttpStatus status) throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		if (null != offset) {
			request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM,
					offset.toString());
		}
		if (null != limit) {
			request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, limit
					.toString());
		}
		if (null != sort) {
			request.setParameter(ServiceConstants.SORT_BY_PARAM, sort);
			if (null != ascending) {
				request.setParameter(ServiceConstants.ASCENDING_PARAM,
						ascending.toString());
			}
		}
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertFalse(HttpStatus.OK.equals(response.getStatus()));
		assertEquals(status.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertTrue(results.has("reason"));
		assertFalse("null".equals(results.getString("reason")));

		return results;
	}

	/**
	 * Add some canned annotations to our entity and persist them
	 *
	 * @param requestUrl 
	 * @throws Exception 
	 */
	public void testEntityAnnotations(String requestUrl) throws Exception {
		// Get our empty annotations container
		JSONObject annotations = testGetJsonEntity(requestUrl);

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
		JSONObject doubleAnnotations = annotations
				.getJSONObject("doubleAnnotations");
		Double pValues[] = { new Double(0.987), new Double(0) };
		doubleAnnotations.put("pValues", pValues);
		JSONObject longAnnotations = annotations
				.getJSONObject("longAnnotations");
		Integer numSamples[] = { 3000 };
		longAnnotations.put("numSamples", numSamples);

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
		
		JSONObject results = testUpdateJsonEntity(annotations);
		// Check the update response
		assertJSONArrayEquals(summary, results.getJSONObject(
				"stringAnnotations").getJSONArray("summary"));
		assertJSONArrayEquals(tissues, results.getJSONObject(
				"stringAnnotations").getJSONArray("tissues"));
		assertJSONArrayEquals(pValues, results.getJSONObject(
				"doubleAnnotations").getJSONArray("pValues"));
		assertJSONArrayEquals(numSamples, results.getJSONObject(
				"longAnnotations").getJSONArray("numSamples"));
		assertJSONArrayEquals(curationEvents, results.getJSONObject(
				"dateAnnotations").getJSONArray("curationEvents"));
		assertJSONArrayEquals(clinicalTrialStartDate, results
				.getJSONObject("dateAnnotations").getJSONArray(
						"clinicalTrialStartDate"));
		// These are sent serialized as Longs and come back serialized as Longs
		assertJSONArrayEquals(epochDates, results.getJSONObject(
				"dateAnnotations").getJSONArray("epochDates"));
		// These are sent serialized as Strings and come back serialized as
		// Longs
		assertJSONArrayEquals(isoDatesAsLong, results.getJSONObject(
				"dateAnnotations").getJSONArray("isoDates"));
		// Now check that we correctly persisted them for real
		JSONObject storedAnnotations = testGetJsonEntity(requestUrl);
		assertJSONArrayEquals(summary, storedAnnotations.getJSONObject(
				"stringAnnotations").getJSONArray("summary"));
		assertJSONArrayEquals(tissues, storedAnnotations.getJSONObject(
				"stringAnnotations").getJSONArray("tissues"));
		assertJSONArrayEquals(pValues, storedAnnotations.getJSONObject(
				"doubleAnnotations").getJSONArray("pValues"));
		assertJSONArrayEquals(numSamples, storedAnnotations
				.getJSONObject("longAnnotations").getJSONArray("numSamples"));
		assertJSONArrayEquals(curationEvents, results.getJSONObject(
				"dateAnnotations").getJSONArray("curationEvents"));
		assertJSONArrayEquals(clinicalTrialStartDate, results
				.getJSONObject("dateAnnotations").getJSONArray(
						"clinicalTrialStartDate"));
		// These are sent serialized as Longs and come back serialized as Longs
		assertJSONArrayEquals(epochDates, results.getJSONObject(
				"dateAnnotations").getJSONArray("epochDates"));
		// These are sent serialized as Strings and come back serialized as
		// Longs
		assertJSONArrayEquals(isoDatesAsLong, results.getJSONObject(
				"dateAnnotations").getJSONArray("isoDates"));
	}
	
	/**
	 * @param expected
	 * @param actual
	 * @throws Exception
	 */
	public void assertJSONArrayEquals(Object[] expected, JSONArray actual)
			throws Exception {
		assertEquals(expected.length, actual.length());

		Set<Object> s1 = new HashSet<Object>(Arrays.asList(expected));
		Set<Object> s2 = new HashSet<Object>();

		for (int i = 0; i < actual.length(); i++) {
			s2.add(actual.get(i));
		}

		// Symmetric set difference the set of elements contained in either
		// of two specified sets but not in both
		Set<Object> symmetricDiff = new HashSet<Object>(s1);
		symmetricDiff.addAll(s2);
		Set<Object> tmp = new HashSet<Object>(s1);
		tmp.retainAll(s2);
		symmetricDiff.removeAll(tmp);

		assertEquals(0, symmetricDiff.size());
	}

	/**
	 * @param results
	 * @throws Exception
	 */
	public void assertExpectedEntitiesProperties(JSONArray results)
			throws Exception {
		for (int i = 0; i < results.length(); i++) {
			JSONObject entity = results.getJSONObject(i);
			assertExpectedEntityProperties(entity);
		}
	}

	/**
	 * @param results
	 * @throws Exception
	 */
	public void assertExpectedEntityProperties(JSONObject results)
			throws Exception {

		// Check default properties
		assertTrue(results.has("id"));
		assertFalse("null".equals(results.getString("id")));
		assertTrue(results.has("uri"));
		assertFalse("null".equals(results.getString("uri")));
		assertTrue(results.has("etag"));
		assertFalse("null".equals(results.getString("etag")));
	}

}
