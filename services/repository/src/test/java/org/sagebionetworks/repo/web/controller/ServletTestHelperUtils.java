package org.sagebionetworks.repo.web.controller;

import java.io.StringReader;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.UnauthorizedException;
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

	public enum HTTPMODE {
		GET, POST, PUT, DELETE, HEAD
	}

	/**
	 * Adds the extra parameters and the Entity object to the HTTP request (via
	 * a EntityObjectMapper)
	 */
	public static void addExtraParams(MockHttpServletRequest request,
			Map<String, String> extraParams) throws Exception {
		if (null != extraParams) {
			for (Map.Entry<String, String> param : extraParams.entrySet()) {
				request.setParameter(param.getKey(), param.getValue());
			}
		}

	}
	
	private static final String REQUEST_ENCODING_CHARSET = "UTF-8";
	private static final String RESPONSE_ENCODING_CHARSET = "UTF-8";

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
	public static MockHttpServletRequest initRequest(HTTPMODE mode, String path,
			String requestURI, Long userId, String accessToken, JSONEntity entity)
			throws Exception {
		MockHttpServletRequest request = initRequestUnauthenticated(mode, path, requestURI, entity);
		if (userId != null) {
			request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId.toString());
		}
		if (accessToken != null) {
			request.addHeader(AuthorizationConstants.SYNAPSE_AUTHORIZATION_HEADER_NAME, "Bearer "+accessToken);
		}
		return request;
	}
		
	private static MockHttpServletRequest initRequestUnauthenticated(HTTPMODE mode, String path,
				String requestURI, JSONEntity entity)
				throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(mode.name());
		request.addHeader("Accept", "application/json; charset="+RESPONSE_ENCODING_CHARSET);
		request.addHeader("Accept-Encoding", RESPONSE_ENCODING_CHARSET);
		request.addHeader("Content-Type", "application/json; charset="+REQUEST_ENCODING_CHARSET);
		request.setRequestURI(path+requestURI);
		if (entity != null) {
			String body = EntityFactory.createJSONStringForEntity(entity);
			request.setContent(body.getBytes(REQUEST_ENCODING_CHARSET));
			log.debug("Request content: " + body);
		}
		return request;

	}
	
	public static MockHttpServletRequest initRequest(HTTPMODE mode,
			String requestURI, Long userId, String accessToken, JSONEntity entity)
			throws Exception {
		return initRequest(mode, "/repo/v1", requestURI, userId, accessToken, entity);
	}

	/**
	 * Sends off a Mock HTTP request and check for errors
	 * 
	 * @param expected
	 *            The expected HTTP status code (null to skip the check)
	 */
	public static MockHttpServletResponse dispatchRequest(
			HttpServlet dispatcherServlet, MockHttpServletRequest request,
			HttpStatus expected) throws Exception {
		if (dispatcherServlet == null) {
			throw new IllegalArgumentException("Servlet cannot be null");
		}

		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherServlet.service(request, response);
		if (expected != null && response.getStatus() != expected.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		return response;
	}

	/**
	 * Convert the status code into an exception
	 */
	public static void handleException(int status, String message)
			throws Exception {
		log.debug("HTTP status: " + status + ", Message: " + message);
		if (HttpStatus.NOT_FOUND.value() == status) {
			throw new NotFoundException(message);
		}
		if (status > 499 && status < 600) {
			throw new DatastoreException(message);
		}
		if (status == 409) {
			throw new NameConflictException(message);
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
		throw new RuntimeException(status + ":" + message);
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
	 * Read a JSONEntity from the response
	 * @param response
	 * @param clazz
	 * @return
	 * @throws Exception
	 */
	public static <T extends JSONEntity> T readResponse(MockHttpServletResponse response, Class<? extends T> clazz) throws Exception {
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		return EntityFactory.createEntityFromJSONString(json, clazz);
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
		return PaginatedResults.createFromJSONObjectAdapter(adapter, clazz);
	}

	/**
	 * Extracts the JSON content of a HTTP response and parses it into a set of
	 * variable paginated results
	 */
	public static <T extends JSONEntity> PaginatedResults<T> readResponseVariablePaginatedResults(
			MockHttpServletResponse response, Class<? extends T> clazz)
			throws Exception {
		JSONObjectAdapterImpl adapter = ServletTestHelperUtils
				.readResponseJSON(response);
		return PaginatedResults.createFromJSONObjectAdapter(adapter, clazz);
	}

	/**
	 * Simple helper for creating a URI for a WikiPage using its key
	 */
	public static String createWikiURI(WikiPageKey key) {
		return "/" + key.getOwnerObjectType().name().toLowerCase() + "/"
				+ key.getOwnerObjectId() + "/wiki/" + key.getWikiPageId();
	}
	
	/**
	 * Simple helper for creating a URI for a V2WikiPage using its key
	 */
	public static String createV2WikiURI(WikiPageKey key) {
		return "/" + key.getOwnerObjectType().name().toLowerCase() + "/"
				+ key.getOwnerObjectId() + "/wiki2/" + key.getWikiPageId();
	}

	/**
	 * Gets a redirect URL
	 */
	public static URL handleRedirectReponse(Boolean redirect,
			MockHttpServletResponse response) throws Exception {
		// Redirect response is different than non-redirect
		if (redirect == null || Boolean.TRUE.equals(redirect)) {
			if (response.getStatus() != HttpStatus.TEMPORARY_REDIRECT.value()) {
				throw new RuntimeException("Status : " + response.getStatus() + ", Content: " + response.getContentAsString());
			}
			// Get the redirect location
			return new URL(response.getRedirectedUrl());
		} else {
			// Redirect == false
			if (response.getStatus() != HttpStatus.OK.value()) {
				throw new RuntimeException("Status : " + response.getStatus() + ", Content: " + response.getContentAsString());
			}
			// Get the redirect location
			return new URL(response.getContentAsString());
		}
	}
}
