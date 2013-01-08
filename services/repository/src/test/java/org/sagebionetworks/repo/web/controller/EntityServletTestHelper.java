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
import org.sagebionetworks.competition.model.Competition;
import org.sagebionetworks.competition.model.Participant;
import org.sagebionetworks.competition.model.Submission;
import org.sagebionetworks.competition.model.SubmissionStatus;
import org.sagebionetworks.competition.model.SubmissionStatusEnum;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.BatchResults;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.NameConflictException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
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
		dispatcherServlet = DispatchServletSingleton.getInstance();
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
	public Entity createEntity(Entity entity, String username, String activityId)
			throws JSONObjectAdapterException, ServletException, IOException, NotFoundException, DatastoreException, NameConflictException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.setParameter(ServiceConstants.GENERATED_BY_PARAM, activityId);
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
	 * Get an entity bundle using only the ID.
	 * @param id
	 * @param testUser1
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public EntityBundle getEntityBundle(String id, int mask, String username) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+id+UrlHelpers.BUNDLE);
		request.setParameter("mask", "" + mask);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		return new EntityBundle(joa);
	}

	/**
	 * Get an entity bundle for a specific version using the ID and versionNumber.
	 * @param id
	 * @param versionNumber 
	 * @param testUser1
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 * @throws DatastoreException 
	 * @throws NotFoundException 
	 */
	public EntityBundle getEntityBundleForVersion(String id, Long versionNumber, int mask, String username) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+id+UrlHelpers.VERSION+"/"+versionNumber+UrlHelpers.BUNDLE);
		request.setParameter("mask", "" + mask);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		return new EntityBundle(joa);
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
		if(status == 409){
			throw new NameConflictException();
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
	
	
	/**
	 * Get the list of all REST resources.
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public RestResourceList getRESTResources() throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.REST_RESOURCES);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		// Done!
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), RestResourceList.class);
	}
	
	/**
	 * Get the effective schema for a resource.
	 * @param resourceId
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public ObjectSchema getEffectiveSchema(String resourceId) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.REST_RESOURCES+UrlHelpers.EFFECTIVE_SCHEMA);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.addParameter(UrlHelpers.RESOURCE_ID, resourceId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		// Done!
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), ObjectSchema.class);
		
	}
	
	/**
	 * Get the full schema for a resource.
	 * @param resourceId
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public ObjectSchema getFullSchema(String resourceId) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.REST_RESOURCES+UrlHelpers.SCHEMA);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.addParameter(UrlHelpers.RESOURCE_ID, resourceId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		// Done!
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), ObjectSchema.class);
		
	}

	/**
	 * Get the entity registry
	 * @param resourceId
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public EntityRegistry getEntityRegistry() throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+UrlHelpers.REGISTRY);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		// Done!
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), EntityRegistry.class);
		
	}

	public VersionInfo promoteVersion(String username, String entityId, Long versionNumber)  throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+entityId+UrlHelpers.PROMOTE_VERSION+"/"+versionNumber);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		dispatcherServlet.service(request, response);
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new ServletTestHelperException(response);
		}
		JSONObjectAdapterImpl joa = new JSONObjectAdapterImpl(response.getContentAsString());
		VersionInfo info = new VersionInfo();
		info.initializeFromJSONObject(joa);
		return info;
	}

	public Versionable createNewVersion(String username, Versionable entity) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+entity.getId());
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, username);
		StringWriter out = new StringWriter();
		String body = EntityFactory.createJSONStringForEntity(entity);
		request.setContent(body.getBytes("UTF-8"));
		dispatcherServlet.service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		JSONObjectAdapterImpl joa = new JSONObjectAdapterImpl(response.getContentAsString());

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), entity.getClass());
	}
	
	//////////////////////////
	// Competition Services //
	//////////////////////////
	
	public Competition createCompetition(Competition comp, String userId)
			throws JSONObjectAdapterException, IOException, DatastoreException,
			NotFoundException, ServletException 
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		comp.writeToJSONObject(joa);
		String body = joa.toJSONString();
		request.setContent(body.getBytes("UTF-8"));
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		joa = new JSONObjectAdapterImpl(json);
		return new Competition(joa);
	}
	
	public Competition getCompetition(String compId) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION+"/"+compId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		return new Competition(joa);
	}
	
	public Competition findCompetition(String name) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION+"/name/"+name);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		return new Competition(joa);
	}
	
	public Competition updateCompetition(Competition comp, String userId) 
			throws JSONObjectAdapterException, IOException, NotFoundException,
			DatastoreException, ServletException
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION+"/"+comp.getId());
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		comp.writeToJSONObject(joa);
		String body = joa.toJSONString();
		request.setContent(body.getBytes("UTF-8"));
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		joa = new JSONObjectAdapterImpl(json);
		return new Competition(joa);
	}
	
	public void deleteCompetition(String compId, String userId) throws ServletException,
			IOException, NotFoundException, DatastoreException
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION + "/" + compId);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
	}
	
	public PaginatedResults<Competition> getCompetitionsPaginated(long limit, long offset) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION);
		request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, "" + offset);
		request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, "" + limit);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		PaginatedResults<Competition> res = new PaginatedResults<Competition>(Competition.class);
		res.initializeFromJSONObject(joa);
		return res;
	}
	
	public long getCompetitionCount() throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION_COUNT);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		return Long.parseLong(response.getContentAsString());
	}
	
	public Participant createParticipant(String userId, String compId)
			throws JSONObjectAdapterException, IOException, DatastoreException,
			NotFoundException, ServletException 
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION+"/"+compId+"/participant");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		String body = joa.toJSONString();
		request.setContent(body.getBytes("UTF-8"));
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		joa = new JSONObjectAdapterImpl(json);
		return new Participant(joa);
	}
	
	public Participant getParticipant(String partId, String compId) throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION+"/"+compId+"/participant/" + partId);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, partId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		return new Participant(joa);
	}
	
	public void deleteParticipant(String partId, String compId) throws ServletException, IOException, DatastoreException, NotFoundException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION + "/" + compId + "/participant/" + partId);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, partId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
	}
	
	public PaginatedResults<Participant> getAllParticipants(String compId) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION + "/" + compId + "/participant");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		PaginatedResults<Participant> res = new PaginatedResults<Participant>(Participant.class);
		res.initializeFromJSONObject(joa);
		return res;
	}
	
	public long getParticipantCount(String compId) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION + "/" + compId + "/participant/count");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		return Long.parseLong(response.getContentAsString());
	}
	
	public Submission createSubmission(Submission sub, String userId)
			throws JSONObjectAdapterException, IOException, DatastoreException,
			NotFoundException, ServletException 
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.SUBMISSION);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		sub.writeToJSONObject(joa);
		String body = joa.toJSONString();
		request.setContent(body.getBytes("UTF-8"));
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		joa = new JSONObjectAdapterImpl(json);
		return new Submission(joa);
	}
	
	public Submission getSubmission(String subId) throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.SUBMISSION+"/"+subId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		return new Submission(joa);
	}
	
	public SubmissionStatus getSubmissionStatus(String subId) throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.SUBMISSION+"/"+subId+"/status");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		return new SubmissionStatus(joa);
	}
	
	public SubmissionStatus updateSubmissionStatus(SubmissionStatus subStatus, String userId) 
			throws JSONObjectAdapterException, IOException, NotFoundException,
			DatastoreException, ServletException
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.SUBMISSION+"/"+subStatus.getId()+"/status");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		subStatus.writeToJSONObject(joa);
		String body = joa.toJSONString();
		request.setContent(body.getBytes("UTF-8"));
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		joa = new JSONObjectAdapterImpl(json);
		return new SubmissionStatus(joa);
	}
	
	public void deleteSubmission(String subId, String userId) throws ServletException, IOException, DatastoreException, NotFoundException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.SUBMISSION+"/"+subId);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
	}
	
	public PaginatedResults<Submission> getAllSubmissions(String userId, String compId, SubmissionStatusEnum status) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION + "/" + compId + "/submission");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		if (status != null) {
			request.setParameter(UrlHelpers.STATUS, status.toString());
		}
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		PaginatedResults<Submission> res = new PaginatedResults<Submission>(Submission.class);
		res.initializeFromJSONObject(joa);
		return res;
	}
	
	public long getSubmissionCount(String compId) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.COMPETITION + "/" + compId + "/submission/count");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		return Long.parseLong(response.getContentAsString());
	}
}
