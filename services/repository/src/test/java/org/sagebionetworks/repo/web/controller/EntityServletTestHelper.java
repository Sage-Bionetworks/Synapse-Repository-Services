package org.sagebionetworks.repo.web.controller;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.repo.ServiceConstants;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.JSONObjectAdapterException;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Note: In order to use this class you must have the following annotations on
 * your test to get the DispatcherServlet initialized:
 * 
 * @RunWith(SpringJUnit4ClassRunner.class)
 * @ContextConfiguration(locations = { "classpath:test-context.xml" }, loader
 *                                 =MockWebApplicationContextLoader.class)
 * @MockWebApplication
 * 
 *                     For more information on @Autowired DispatcherServlet see
 *                     http
 *                     ://tedyoung.me/2011/02/14/spring-mvc-integration-testing
 *                     -controllers/
 * @author jmhill
 * 
 */
public class EntityServletTestHelper {

	private static final Log log = LogFactory.getLog(ServletTestHelper.class);

	
	private static HttpServlet dispatcherServlet = null;

	/**
	 * Setup the servlet, default test user, and entity list for test cleanup.
	 * 
	 * Create a Spring MVC DispatcherServlet so that we can test our URL
	 * mapping, request format, response format, and response status code.
	 * 
	 * @throws Exception
	 */
	public EntityServletTestHelper() throws Exception {
		if(null == dispatcherServlet) {
			MockServletConfig servletConfig = new MockServletConfig("repository");
			servletConfig.addInitParameter("contextConfigLocation",
			"classpath:test-context.xml");
			dispatcherServlet = new DispatcherServlet();
			dispatcherServlet.init(servletConfig);
		}
	}
	/**
	 * Create an entity without an entity type.
	 * 
	 * @param entity
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws IOException
	 * @throws ServletException
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 * @throws Exception
	 */
	public Entity createEntity(Entity entity, String username)
			throws JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		String body = EntityFactory.createJSONStringForEntity(entity);
		request.setContent(body.getBytes("UTF-8"));
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		return JSONEntityHttpMessageConverter.readEntity(reader);
	}

	/**
	 * Delete an entity without knowning the type.
	 * 
	 * @param dispatchServlet
	 * @param id
	 * @param userId
	 * @param extraParams
	 * @throws ServletException
	 * @throws IOException
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public void deleteEntity(String id, String userId) throws ServletException,
			IOException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY + "/" + id);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
	}

	/**
	 * Get an entity using only the ID.
	 * @param id
	 * @param testUser1
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Entity getEntity(String id, String username) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+id);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		return JSONEntityHttpMessageConverter.readEntity(reader);
	}

	/**
	 * Update an entity.
	 * @param toUpdate
	 * @param username
	 * @return
	 * @throws JSONObjectAdapterException
	 * @throws ServletException
	 * @throws IOException
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Entity updateEntity(Entity toUpdate, String username) throws JSONObjectAdapterException, IOException, NotFoundException, DatastoreException, ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+toUpdate.getId());
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.addHeader(ServiceConstants.ETAG_HEADER, toUpdate.getEtag());
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		String body = EntityFactory.createJSONStringForEntity(toUpdate);
		request.setContent(body.getBytes("UTF-8"));
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		return JSONEntityHttpMessageConverter.readEntity(reader);
	}
	
	/**
	 * Convert the status code into an exception
	 * @param status
	 * @param message
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	private void handleException(int status, String message) throws NotFoundException, DatastoreException{
		if(HttpStatus.NOT_FOUND.value() == status){
			throw new NotFoundException(message);
		}
		if(status > 499 && status < 600){
			throw new DatastoreException(message);
		}
		if(status > 399 && status < 500){
			throw new IllegalArgumentException(message);
		}
		// Not sure what else it could be!
		throw new RuntimeException(message);
	}

	/**
	 * Get the annotations for an entity.
	 * @param id
	 * @param username
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws JSONObjectAdapterException
	 */
	public Annotations getEntityAnnotaions(String id, String username) throws ServletException, IOException, NotFoundException, DatastoreException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+id+UrlHelpers.ANNOTATIONS);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), Annotations.class);
	}

	/**
	 * Update the annotaions of an entity
	 * @param annos
	 * @param testUser1
	 * @return
	 * @throws JSONObjectAdapterException 
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public Annotations updateAnnotations(Annotations annos, String username) throws JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+annos.getId()+UrlHelpers.ANNOTATIONS);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.addHeader(ServiceConstants.ETAG_HEADER, annos.getEtag());
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		StringWriter out = new StringWriter();
		String body = EntityFactory.createJSONStringForEntity(annos);
		request.setContent(body.getBytes("UTF-8"));
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), Annotations.class);
	}

	/**
	 * Get the user's permissions for an entity.
	 * @param id
	 * @param username
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws JSONObjectAdapterException
	 */
	public UserEntityPermissions getUserEntityPermissions(String id, String username) throws ServletException, IOException, NotFoundException, DatastoreException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+id+UrlHelpers.PERMISSIONS);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), UserEntityPermissions.class);
	}
	
	/**
	 * Get the user's permissions for an entity.
	 * @param id
	 * @param username
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws JSONObjectAdapterException
	 */
	public EntityPath getEntityPath(String id, String username) throws ServletException, IOException, NotFoundException, DatastoreException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+id+UrlHelpers.PATH);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), EntityPath.class);
	}
	
	/**
	 * @param ids
	 * @param username
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws JSONException
	 * @throws JSONObjectAdapterException
	 */
	public BatchResults<EntityHeader> getEntityTypeBatch(List<String> ids,
			String username) throws ServletException, IOException, NotFoundException, DatastoreException, JSONException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY_TYPE);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.setParameter(ServiceConstants.BATCH_PARAM, StringUtils.join(ids, ServiceConstants.BATCH_PARAM_VALUE_SEPARATOR));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.error("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(new JSONObject(response.getContentAsString()));
		BatchResults<EntityHeader> results = new BatchResults<EntityHeader>(EntityHeader.class);
		results.initializeFromJSONObject(adapter);
		return results;
	}

}
