package org.sagebionetworks.auth;

// Shamelessly copied from org.sagebionetworks.repo.web.controller.Helpers

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Test helper class to consolidate some generic testing code
 * 
 * @author deflaux
 * 
 */
public class Helpers {

	private static final Logger log = Logger.getLogger(Helpers.class.getName());

	private DispatcherServlet servlet = null;

	private static final int JSON_INDENT = 2;

	/**
	 * Setup up our mock datastore and servlet
	 * 
	 * @return the servlet
	 * @throws ServletException
	 */
	public DispatcherServlet setUp() throws ServletException {
		// Create a Spring MVC DispatcherServlet so that we can test our URL
		// mapping, request format, response format, and response status code.
		MockServletConfig servletConfig = new MockServletConfig("authentication");
		servletConfig
				.addInitParameter("contextConfigLocation",
						"classpath:authentication-context.xml,classpath:authentication-servlet.xml");
		servlet = new DispatcherServlet();
		servlet.init(servletConfig);

		return servlet;
	}

	/**
	 * Teardown our datastore, deleting all items
	 */
	public void tearDown() {
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
//		System.out.println("Helpers.testCreateJsonEntity: "+jsonRequestContent);
		return testCreateJsonEntity(requestUrl, jsonRequestContent, HttpStatus.CREATED);
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
				String jsonRequestContent, HttpStatus expectedStatus) throws Exception {		
			
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
		log.info("Results: " + response.getStatus()+ " " + response.getContentAsString());
		assertEquals(expectedStatus.value(), response.getStatus());
		JSONObject results = null;
		String s = response.getContentAsString();
		if (s!=null && s.length()>0) {
			results = new JSONObject(s);
		}
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

		return results;
	}

	/**
	 * @param jsonEntity
	 * @return the json object holding the updated entity
	 * @throws Exception
	 */
	public JSONObject testUpdateJsonEntity(String requestUrl, String stringEntity)
			throws Exception {
		JSONObject jsonEntity = new JSONObject(stringEntity);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(jsonEntity.toString().getBytes("UTF-8"));
		log.info("About to send: " + jsonEntity.toString(JSON_INDENT));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject results = null;
		String s = response.getContentAsString();
		if (s!=null && s.length()>0) {
			results = new JSONObject(s);
		}

		return results;
	}

	/**
	 * @param jsonEntity
	 * @return the json object holding the updated entity
	 * @throws Exception
	 */
	public JSONObject testUpdateJsonEntity(String requestUrl, String stringEntity, HttpStatus status)
			throws Exception {
		JSONObject jsonEntity = new JSONObject(stringEntity);
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(jsonEntity.toString().getBytes("UTF-8"));
		log.info("About to send: " + jsonEntity.toString(JSON_INDENT));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(status.value(), response.getStatus());
		JSONObject results = null;
		String s = response.getContentAsString();
		if (s!=null && s.length()>0) {
			results = new JSONObject(s);
		}

		return results;
	}

	/**
	 * @param requestUrl
	 * @throws Exception
	 */
	public void testDeleteJsonEntity(String requestUrl,
			String jsonRequestContent) throws Exception {

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(requestUrl);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(jsonRequestContent.getBytes("UTF-8"));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.NO_CONTENT.value(), response.getStatus());
		assertEquals("", response.getContentAsString());

		//testGetJsonEntityShouldFail(requestUrl, HttpStatus.NOT_FOUND);
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

		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertEquals(HttpStatus.OK.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());


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
	 * @param jsonRequestContent
	 * @param status
	 * @return the error entity
	 * @throws Exception
	 */
	public JSONObject testUpdateJsonEntityShouldFail(String requestUrl,
			String jsonRequestContent,
			HttpStatus status) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setRequestURI(requestUrl);
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");

		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setContent(jsonRequestContent.getBytes("UTF-8"));
		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertFalse(HttpStatus.OK.equals(response.getStatus()));
		assertEquals(status.value(), response.getStatus());
		JSONObject results = null;
		try {
			results = new JSONObject(response.getContentAsString());
			assertTrue(results.has("reason"));
			assertFalse("null".equals(results.getString("reason")));
		} catch (Exception e) {
			throw new RuntimeException(">"+response.getContentAsString()+"<", e);
		}

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

		servlet.service(request, response);
		log.info("Results: " + response.getContentAsString());
		assertFalse(HttpStatus.OK.equals(response.getStatus()));
		assertEquals(status.value(), response.getStatus());
		JSONObject results = new JSONObject(response.getContentAsString());
		assertTrue(results.has("reason"));
		assertFalse("null".equals(results.getString("reason")));

		return results;
	}


}
