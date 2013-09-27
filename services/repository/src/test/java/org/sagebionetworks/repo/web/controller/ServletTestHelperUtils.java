package org.sagebionetworks.repo.web.controller;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.VariableContentPaginatedResults;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.schema.adapter.JSONEntity;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Collection of helper methods for the servlet test helpers
 */
public class ServletTestHelperUtils {

	private static final Log log = LogFactory.getLog(ServletTestHelper.class);

	private static final EntityObjectMapper objectMapper = new EntityObjectMapper();

	public enum HTTPMODE {
		GET, POST, PUT, DELETE, HEAD
	}

	/**
	 * Adds the extra parameters and the Entity object to the HTTP request (via
	 * a EntityObjectMapper)
	 */
	public static void addExtraParams(MockHttpServletRequest request,
			Object entity, Map<String, String> extraParams) throws Exception {
		if (null != extraParams) {
			for (Map.Entry<String, String> param : extraParams.entrySet()) {
				request.setParameter(param.getKey(), param.getValue());
			}
		}
		if (entity != null) {
			StringWriter out = new StringWriter();
			objectMapper.writeValue(out, entity);
			String body = out.toString();
			request.setContent(body.getBytes("UTF-8"));
		}

	}

	/**
	 * Fills in a Mock HTTP request with the default headers (Accept and
	 * Content-Type), HTTP method, request URI, optional username parameter, and
	 * optional body
	 * 
	 * @param username
	 *            Optional, used to mock the result of a request passing through
	 *            the AuthorizationFilter
	 * @param entity
	 *            Optional, object to serialize into the body of the request
	 */
	public static MockHttpServletRequest initRequest(HTTPMODE mode,
			String requestURI, String username, JSONEntity entity)
			throws Exception {
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
	 * @param expected
	 *            The expected HTTP status code
	 */
	public static MockHttpServletResponse dispatchRequest(
			HttpServlet dispatcherServlet, MockHttpServletRequest request,
			HttpStatus expected) throws Exception {
		if (dispatcherServlet == null) {
			throw new IllegalArgumentException("Servlet cannot be null");
		}

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
	private static void handleException(int status, String message)
			throws Exception {
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
	 * Extracts the JSON content of a HTTP response and parses it into an Entity
	 * object
	 */
	public static Entity readResponseEntity(MockHttpServletResponse response)
			throws Exception {
		StringReader reader = new StringReader(response.getContentAsString());
		return JSONEntityHttpMessageConverter.readEntity(reader);
	}

	/**
	 * Extracts the JSON content of a HTTP response and parses it into an JSON
	 * adapter object
	 */
	public static JSONObjectAdapterImpl readResponseJSON(
			MockHttpServletResponse response) throws Exception {
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		return new JSONObjectAdapterImpl(json);
	}

	/**
	 * Extracts the JSON content of a HTTP response and parses it into a set of
	 * paginated results
	 */
	public static <T extends JSONEntity> PaginatedResults<T> readResponsePaginatedResults(
			MockHttpServletResponse response, Class<? extends T> clazz)
			throws Exception {
		JSONObjectAdapterImpl adapter = ServletTestHelperUtils
				.readResponseJSON(response);
		PaginatedResults<T> result = new PaginatedResults<T>(clazz);
		result.initializeFromJSONObject(adapter);
		return result;
	}

	/**
	 * Extracts the JSON content of a HTTP response and parses it into a set of
	 * variable paginated results
	 */
	public static <T extends JSONEntity> VariableContentPaginatedResults<T> readResponseVariablePaginatedResults(
			MockHttpServletResponse response, Class<? extends T> clazz) throws Exception {
		JSONObjectAdapterImpl adapter = ServletTestHelperUtils
				.readResponseJSON(response);
		VariableContentPaginatedResults<T> pr = new VariableContentPaginatedResults<T>();
		pr.initializeFromJSONObject(adapter);
		return pr;
	}

	/**
	 * Simple helper for creating a URI for a WikiPage using its key
	 */
	public static String createWikiURI(WikiPageKey key) {
		return "/" + key.getOwnerObjectType().name().toLowerCase() + "/"
				+ key.getOwnerObjectId() + "/wiki/" + key.getWikiPageId();
	}

	/**
	 * Gets a redirect URL
	 */
	public static URL handleRedirectReponse(Boolean redirect,
			MockHttpServletResponse response) throws Exception {
		// Redirect response is different than non-redirect
		if (redirect == null || Boolean.TRUE.equals(redirect)) {
			if (response.getStatus() != HttpStatus.TEMPORARY_REDIRECT.value()) {
				throw new ServletTestHelperException(response);
			}
			// Get the redirect location
			return new URL(response.getRedirectedUrl());
		} else {
			// Redirect == false
			if (response.getStatus() != HttpStatus.OK.value()) {
				throw new ServletTestHelperException(response);
			}
			// Get the redirect location
			return new URL(response.getContentAsString());
		}
	}
}
