package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.manager.TestUserDAO;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.util.UserProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Test helper class to consolidate some generic testing code.
 * <p>
 * 
 * It reads optional system properties to change this from a unit test to an
 * integration test INTEGRATION_TEST_ENDPOINT and SERVLET_PREFIX. To enable
 * this, run one of the following:
 * <ul>
 * <li>mvn test -DINTEGRATION_TEST_ENDPOINT=http://localhost:8080
 * -DSERVLET_PREFIX=/repo/v1
 * <li>mvn test
 * -DINTEGRATION_TEST_ENDPOINT=http://repositoryserviceb.elasticbeanstalk.com
 * -DSERVLET_PREFIX=/repo/v1
 * <li>mvn test -DINTEGRATION_TEST_ENDPOINT=http://localhost:8080
 * -DSERVLET_PREFIX=/auth/v1
 * <li>or pass these properties in your JUnit eclipse settings
 * </ul>
 * 
 * Also note that the unit tests do not use web.xml and therefore urls are not
 * prefixed with /repo/v1. For integration tests we do need the servlet prefix.
 * Call Helpers.getServletPrefix from your unit tests to get the url prefix, if
 * any.
 * 
 * @author deflaux
 */
public class Helpers {

	private static final Logger log = Logger.getLogger(Helpers.class.getName());
	private static final int JSON_INDENT = 2;
	private static final String DEFAULT_SERVLET_PREFIX = "/repo/v1";

	private HttpServlet servlet;
	private String servletPrefix;
	private String integrationTestEndpoint;
	private String userId;
	private UserInfo userInfo;
	private LinkedList<TestStateItem> testState;
	private Boolean isIntegrationTest = false;

	@Autowired
	public UserProvider testUserProvider;
	
	/**
	 * The JSON for a read only ACL
	 * @return
	 */
	public String getIdentifiedUserReadOnlyACL(){
		UserGroup group = testUserProvider.getIdentifiedUserGroup();
		return "{\"groupName\":\""+group.getName()+"\", \"accessType\":[\"READ\"]}";
	}

	/**
	 * The JSON for a Read/write ACL.
	 * @return
	 */
	public String getIdentifiedUserReadWriteACL(){
		UserGroup group = testUserProvider.getIdentifiedUserGroup();
		return "{\"groupName\":\""+group.getName()+"\", \"accessType\":[\"READ\", \"WRITE\"]}";
	}
	/**
	 * Default constructor reads optional system properties to change this from
	 * a unit test to an integration test INTEGRATION_TEST_ENDPOINT and
	 * SERVLET_PREFIX
	 */
	public Helpers() {
		integrationTestEndpoint = System
				.getProperty("INTEGRATION_TEST_ENDPOINT");
		servletPrefix = System.getProperty("SERVLET_PREFIX");
	}

	/**
	 * @return the URI prefix of the servlet being tested
	 */
	public String getServletPrefix() {
		return servletPrefix;
	}

	/**
	 * @return whether or not the current scenario is an integration test
	 */
	public Boolean isIntegrationTest() {
		return isIntegrationTest;
	}

	/**
	 * Setup up our mock servlet
	 * 
	 * @return the servlet
	 * @throws Exception
	 */
	public HttpServlet setUp() throws Exception {
		if (null != integrationTestEndpoint) {
			isIntegrationTest = true;
			servlet = new IntegrationTestMockServlet(integrationTestEndpoint);
			if (null == servletPrefix) {
				servletPrefix = DEFAULT_SERVLET_PREFIX;
			}
		} else {
			servletPrefix = "";
			// Create a Spring MVC DispatcherServlet so that we can test our URL
			// mapping, request format, response format, and response status
			// code.
			MockServletConfig servletConfig = new MockServletConfig(
					"repository");
			servletConfig.addInitParameter("contextConfigLocation",
					"classpath:test-context.xml");
			servlet = new DispatcherServlet();
			try {
				servlet.init(servletConfig);
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}
		}

		useAdminUser();
		testState = new LinkedList<TestStateItem>();

		return servlet;
	}

	/**
	 * Tear down our datastore, deleting all items
	 * 
	 * @throws Exception
	 */
	public void tearDown() throws Exception {
		for (TestStateItem item : testState) {
			try {
				item.delete();
			} catch (Exception e) {
				log.info(e.toString());
			}
		}
		// If we do not destroy the servlet we get connection leaks.
		if(servlet != null){
			servlet.destroy();
		}

	}

	public void useAdminUser() {
		assertNotNull(testUserProvider);
		userInfo = testUserProvider.getTestAdminUserInfo();
		UserInfo.validateUserInfo(userInfo);
		assertNotNull(userInfo);
		userId = userInfo.getUser().getUserId();
	}

	public void useTestUser() {
		assertNotNull(testUserProvider);
		userInfo = testUserProvider.getTestUserInfo();
		UserInfo.validateUserInfo(userInfo);
		assertNotNull(userInfo);
		userId = userInfo.getUser().getUserId();
		Collection<UserGroup> groups = userInfo.getGroups();
		for(UserGroup group : groups) {
			System.out.println(group);
		}
	}

	public String getUserId() {
		return userId;
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
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(jsonRequestContent.getBytes("UTF-8"));
		log.info("About to send: "
				+ new JSONObject(jsonRequestContent).toString(2));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("Reason: "
				+

				(response.getErrorMessage() == null ? response
						.getContentAsString() : response.getErrorMessage())

		, HttpStatus.CREATED.value(), response.getStatus());
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
		assertTrue(locationHeader, locationHeader.endsWith(requestUrl + "/"
				+ URLEncoder.encode(results.getString("id"), "UTF-8")));
		assertTrue(locationHeader.endsWith(results.getString("uri")));

		// Stash the url for this entity so that we can clean it up at the end
		// of our test
		testState.addFirst(new TestStateItem(userId, locationHeader));

		return results;
	}

	/**
	 * Creation of JSON entities, when there is no response expected
	 * 
	 * @param requestUrl
	 * @param jsonRequestContent
	 * @throws Exception
	 */
	public void testCreateNoResponse(String requestUrl,
			String jsonRequestContent) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(jsonRequestContent.getBytes("UTF-8"));
		log.info("About to send: "
				+ new JSONObject(jsonRequestContent).toString(2));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.CREATED.value(), response.getStatus());
	}

	/**
	 * @param requestUrl
	 * @return the jsonRequestContent object holding the entity
	 * @throws Exception
	 */
	public JSONObject testGetJsonEntity(String requestUrl) throws Exception {
		return testGetJsonEntity(requestUrl, null);
	}

	/**
	 * @param requestUrl
	 * @return the jsonRequestContent object holding the entity
	 * @throws Exception
	 */
	public JSONObject testGetJsonEntity(String requestUrl, Map<String, String> extraParams) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		if (null != extraParams) {
			for(Map.Entry<String, String> param : extraParams.entrySet()) {
				request.setParameter(param.getKey(), param.getValue());
			}
		}
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("helper-servletprefix=" + getServletPrefix()
				+ ", requestUrl=" + requestUrl, HttpStatus.OK.value(), response
				.getStatus());
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
	 * A call that returns Paginated results.
	 * 
	 * @param requestUrl
	 * @return
	 * @throws Exception
	 */
	public JSONObject testGetJsonEntities(String requestUrl) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals("helper-servletprefix=" + getServletPrefix()
				+ ", requestUrl=" + requestUrl, HttpStatus.OK.value(), response
				.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		log.info(results.toString(JSON_INDENT));

		// Check default properties
		assertExpectedEntitiesProperties(results);
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
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
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
	 * This method calls a URI with the PUT method, without any payload
	 * 
	 * @param uri
	 * @throws Exception
	 */
	public void testUpdateNoPayload(String uri) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(uri);
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.CREATED.value(), response.getStatus());
		return;
	}

	/**
	 * @param requestUrl
	 * @throws Exception
	 */
	public void testDeleteJsonEntity(String requestUrl) throws Exception {
		testDeleteJsonEntity(requestUrl, true);
	}

	/**
	 * @param requestUrl
	 * @param tryGet
	 * @throws Exception
	 */
	public void testDeleteJsonEntity(String requestUrl, boolean tryGet)
			throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(requestUrl + " -> " + response.getContentAsString(),
				HttpStatus.NO_CONTENT.value(), response.getStatus());
		assertEquals("", response.getContentAsString());

		if (tryGet)
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
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
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
		assertEquals("Reason: " + response.getErrorMessage(), HttpStatus.OK
				.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());

		// Check that the response has the correct structure
		assertTrue(results.has("totalNumberOfResults"));
		assertTrue(results.has("results"));
		assertTrue(results.has("paging"));

		assertExpectedEntitiesProperties(results.getJSONArray("results"));

		return results;
	}

	/**
	 * Some responses from the service are successful but do not return
	 * "entities" that we can directly do CRUD upon.
	 * 
	 * @param requestUrl
	 * @return the jsonRequestContent object
	 * @throws Exception
	 */
	public JSONObject testGetJsonObject(String requestUrl) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(requestUrl, HttpStatus.OK.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		log.info(results.toString(JSON_INDENT));

		return results;
	}

	/**
	 * @param query
	 * @return the query result
	 * @throws Exception
	 */
	public JSONObject testQuery(String query) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(servletPrefix + "/query");
		request.addParameter("query", query);
		request.addParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		log.info("About to query: " + query);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject queryResult = new JSONObject(response.getContentAsString());
		log.info(queryResult.toString(JSON_INDENT));
		assertTrue(queryResult.has("totalNumberOfResults"));
		assertTrue(queryResult.has("results"));
		return queryResult;
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
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
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
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
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
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
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
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
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
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
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
	 * @param query
	 * @param status
	 * @return the error response
	 * @throws Exception
	 */
	public JSONObject testQueryShouldFail(String query, HttpStatus status)
			throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(servletPrefix + "/query");
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		request.addParameter("query", query);
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(status.value(), response.getStatus());
		JSONObject error = new JSONObject(response.getContentAsString());
		assertTrue(error.has("reason"));
		assertFalse("null".equals(error.getString("reason")));
		return error;
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

		// Start with a date with no milliseconds as they will be lost
		// going to MySQL
		Date now = new Date(1305050350000L);
		DateTime aWhileBack = new DateTime("2010-10-01");

		Long curationEvents[] = { now.getTime(), now.getTime() + 100000,
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
		assertJSONArrayEquals(clinicalTrialStartDate, results.getJSONObject(
				"dateAnnotations").getJSONArray("clinicalTrialStartDate"));
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
		assertJSONArrayEquals(numSamples, storedAnnotations.getJSONObject(
				"longAnnotations").getJSONArray("numSamples"));
		assertJSONArrayEquals(curationEvents, results.getJSONObject(
				"dateAnnotations").getJSONArray("curationEvents"));
		assertJSONArrayEquals(clinicalTrialStartDate, results.getJSONObject(
				"dateAnnotations").getJSONArray("clinicalTrialStartDate"));
		// These are sent serialized as Longs and come back serialized as Longs
		assertJSONArrayEquals(epochDates, results.getJSONObject(
				"dateAnnotations").getJSONArray("epochDates"));
		// These are sent serialized as Strings and come back serialized as
		// Longs
		assertJSONArrayEquals(isoDatesAsLong, results.getJSONObject(
				"dateAnnotations").getJSONArray("isoDates"));

		// Try to overwrite using an obsolete version of the annotations and it
		// should fail
		JSONObject preconditionError = testUpdateJsonEntityShouldFail(
				annotations, HttpStatus.PRECONDITION_FAILED);
		assertTrue(preconditionError
				.getString("reason")
				.endsWith(
						"was updated since you last fetched it, retrieve it again and reapply the update"));

		// Whitespace in annotation names is invalid
		storedAnnotations.getJSONObject("stringAnnotations").put(
				"tissue types", tissues);
		JSONObject badRequestError = testUpdateJsonEntityShouldFail(
				storedAnnotations, HttpStatus.BAD_REQUEST);
		assertTrue(badRequestError.getString("reason").startsWith(
				"Invalid annotation name"));
	}

	/**
	 * Give identified individuals read-only access to the entity
	 * @param entity
	 * @throws Exception 
	 * @throws JSONException 
	 */
	public void addPublicReadOnlyAclToEntity(JSONObject entity) throws JSONException, Exception {
		JSONObject entityAcl = testGetJsonEntity(entity
				.getString("accessControlList"));
		entityAcl.getJSONArray("resourceAccess").put(
				new JSONObject(getIdentifiedUserReadOnlyACL()));
		
		// TODO uncomment this line and delete the stuff that follows when PLFM-321 is fixed
		//testUpdateJsonEntity(entityAcl);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.addHeader(ServiceConstants.ETAG_HEADER, entityAcl
				.getString("etag"));
		request.setRequestURI(entityAcl.getString("uri"));
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(entityAcl.toString().getBytes("UTF-8"));
		log.info("About to send: " + entityAcl.toString(JSON_INDENT));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		log.info(results.toString(JSON_INDENT));

		// Check default properties
		// PLFM-321 assertExpectedEntityProperties(results);
		assertEquals(entityAcl.getString("id"), results.getString("id"));
		// PLFM-321 assertEquals(entityAcl.getString("uri"), results.getString("uri"));

		// Check our response headers
		String etagHeader = (String) response
				.getHeader(ServiceConstants.ETAG_HEADER);
		assertNotNull(etagHeader);
		assertEquals(etagHeader, results.getString("etag"));

		// Make sure we got an updated etag
		assertFalse(etagHeader.equals(entityAcl.getString("etag")));
	}

	/**
	 * Give identified individuals read-only access to the entity
	 * @param entity
	 * @throws Exception 
	 * @throws JSONException 
	 */
	public void addPublicReadWriteAclToEntity(JSONObject entity) throws JSONException, Exception {
		JSONObject entityAcl = testGetJsonEntity(entity
				.getString("accessControlList"));
		entityAcl.getJSONArray("resourceAccess").put(
				new JSONObject(getIdentifiedUserReadWriteACL()));
		
		// TODO uncomment this line and delete the stuff that follows when PLFM-321 is fixed
		//testUpdateJsonEntity(entityAcl);
		
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.addHeader(ServiceConstants.ETAG_HEADER, entityAcl
				.getString("etag"));
		request.setRequestURI(entityAcl.getString("uri"));
		if (null != userId)
			request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(entityAcl.toString().getBytes("UTF-8"));
		log.info("About to send: " + entityAcl.toString(JSON_INDENT));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		log.info(results.toString(JSON_INDENT));

		// Check default properties
		// PLFM-321 assertExpectedEntityProperties(results);
		assertEquals(entityAcl.getString("id"), results.getString("id"));
		// PLFM-321 assertEquals(entityAcl.getString("uri"), results.getString("uri"));

		// Check our response headers
		String etagHeader = (String) response
				.getHeader(ServiceConstants.ETAG_HEADER);
		assertNotNull(etagHeader);
		assertEquals(etagHeader, results.getString("etag"));

		// Make sure we got an updated etag
		assertFalse(etagHeader.equals(entityAcl.getString("etag")));
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

	/**
	 * @param results
	 * @throws Exception
	 */
	public void assertExpectedEntitiesProperties(JSONObject results)
			throws Exception {
		// Check default properties
		assertTrue(results.has("paging"));
		assertTrue(results.has("results"));
		assertTrue(results.has("totalNumberOfResults"));
		assertFalse("null".equals(results.getString("totalNumberOfResults")));
	}

	private class TestStateItem {
		public String userId;
		public String requestUrl;

		public TestStateItem(String userId, String requestUrl) {
			this.userId = userId;
			this.requestUrl = requestUrl;
		}

		public void delete() throws Exception {
			MockHttpServletRequest request = new MockHttpServletRequest();
			MockHttpServletResponse response = new MockHttpServletResponse();
			request.setMethod("DELETE");
			request.addHeader("Accept", "application/json");
			request.setRequestURI(requestUrl);
			if (null != userId)
				request.setParameter(AuthUtilConstants.USER_ID_PARAM, userId);
			servlet.service(request, response);
		}
	}
}
