package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Note: In order to use this class you must have the following annotations on
 * your test to get the DispatcherServlet initialized:
 * 
 * @RunWith(SpringJUnit4ClassRunner.class)
 * @ContextConfiguration(locations = { "classpath:test-context.xml" })
 * 
 */
public class EntityServletTestHelperUtils {

	private static final Log log = LogFactory.getLog(ServletTestHelper.class);
	
	public enum HTTPMODE {
		GET, 
		POST, 
		PUT, 
		DELETE
	}
	
	/**
	 * Fills in a Mock HTTP request with the default headers (Accept and Content-Type), 
	 *   HTTP method, request URI, optional username parameter, and optional body 
	 * 
	 * @param username Optional, used to mock the result of a request passing through the AuthorizationFilter 
	 * @param entity Optional, object to serialize into the body of the request
	 */
	public static MockHttpServletRequest initRequest(HTTPMODE mode, String requestURI, String username, JSONEntity entity) 
			throws JSONObjectAdapterException, UnsupportedEncodingException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(mode.name());
		request.addHeader("Accept", "application/json");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setRequestURI(requestURI);
		if (username != null) {
			request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		}
		if (entity != null) {
			String body = EntityFactory.createJSONStringForEntity(entity);
			request.setContent(body.getBytes("UTF-8"));
		}
		return request;
	}
	
	/**
	 * Sends off a Mock HTTP request and check for errors
	 * 
	 * @param expected The expected HTTP status code
	 */
	public static MockHttpServletResponse dispatchRequest(HttpServlet dispatcherServlet, MockHttpServletRequest request, HttpStatus expected) 
			throws IOException, ServletException, DatastoreException, NotFoundException {
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != expected.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		return response;
	}
	
	/**
	 * Convert the status code into an exception
	 */
	public static void handleException(int status, String message) 
			throws NotFoundException, DatastoreException {
		if (HttpStatus.NOT_FOUND.value() == status) {
			throw new NotFoundException(message);
		}
		if (status > 499 && status < 600) {
			throw new DatastoreException(message);
		}
		if (status == 409) {
			throw new NameConflictException();
		}
		if (status == 403) {
			throw new UnauthorizedException(message);
		}
		if (status == 404) {
			throw new NotFoundException(message);
		}
		if (status > 399 && status < 500) {
			throw new IllegalArgumentException(message);
		}
		// Not sure what else it could be!
		throw new RuntimeException(message);
	}
	
	/**
	 * Extracts the JSON content of a HTTP response and parses it into an Entity object
	 */
	public static Entity readResponseEntity(MockHttpServletResponse response) 
			throws IOException, JSONObjectAdapterException {
		StringReader reader = new StringReader(response.getContentAsString());
		return JSONEntityHttpMessageConverter.readEntity(reader);
	}
	
	/**
	 * Extracts the JSON content of a HTTP response and parses it into an JSON adapter object
	 */
	public static JSONObjectAdapterImpl readResponseJSON(MockHttpServletResponse response) 
			throws JSONObjectAdapterException, IOException {
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		return new JSONObjectAdapterImpl(json);
	}
}

