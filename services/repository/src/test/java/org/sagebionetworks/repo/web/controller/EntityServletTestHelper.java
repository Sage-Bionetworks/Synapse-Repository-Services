package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Participant;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
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
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.message.ObjectType;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
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
		if(status == 403) {
			throw new UnauthorizedException(message);
		}
		if(status == 404) {
			throw new NotFoundException(message);
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

	public Versionable createNewVersion(String username, Versionable entity) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.ENTITY+"/"+entity.getId()+"/version");
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
	
	/////////////////////////
	// Evaluation Services //
	/////////////////////////
	
	public Evaluation createEvaluation(Evaluation eval, String userId)
			throws JSONObjectAdapterException, IOException, DatastoreException,
			NotFoundException, ServletException 
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		eval.writeToJSONObject(joa);
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
		return new Evaluation(joa);
	}
	
	public Evaluation getEvaluation(String userId, String evalId) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION+"/"+evalId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		return new Evaluation(joa);
	}
	
	public Boolean canAccess(String userId, String evalId, ACCESS_TYPE accessType) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION+"/"+evalId+"/access");
		request.addParameter("accessType", accessType.toString());
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
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
		return (Boolean)joa.get("result");
	}

	
	public Evaluation findEvaluation(String userId, String name) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION+"/name/"+name);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		return new Evaluation(joa);
	}
	
	public PaginatedResults<Evaluation> getAvailableEvaluations(String userId, String status) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION_AVAILABLE);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("limit", "100");
		request.setParameter("offset", "0");
		if (status!=null) request.setParameter("status", status);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		PaginatedResults<Evaluation> res = new PaginatedResults<Evaluation>(Evaluation.class);
		res.initializeFromJSONObject(joa);
		return res;
	}
	
	public Evaluation updateEvaluation(Evaluation eval, String userId) 
			throws JSONObjectAdapterException, IOException, NotFoundException,
			DatastoreException, ServletException
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION+"/"+eval.getId());
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		JSONObjectAdapter joa = new JSONObjectAdapterImpl();
		eval.writeToJSONObject(joa);
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
		return new Evaluation(joa);
	}
	
	public void deleteEvaluation(String evalId, String userId) throws ServletException,
			IOException, NotFoundException, DatastoreException
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION + "/" + evalId);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
	}
	
	public PaginatedResults<Evaluation> getEvaluationsPaginated(String userId, long limit, long offset) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION);
		request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, "" + offset);
		request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, "" + limit);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		StringReader reader = new StringReader(response.getContentAsString());
		String json = JSONEntityHttpMessageConverter.readToString(reader);
		JSONObjectAdapter joa = new JSONObjectAdapterImpl(json);
		PaginatedResults<Evaluation> res = new PaginatedResults<Evaluation>(Evaluation.class);
		res.initializeFromJSONObject(joa);
		return res;
	}
	
	public long getEvaluationCount(String userId) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION_COUNT);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		return Long.parseLong(response.getContentAsString());
	}
	
	public Participant createParticipant(String userId, String evalId)
			throws JSONObjectAdapterException, IOException, DatastoreException,
			NotFoundException, ServletException 
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION+"/"+evalId+"/participant");
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
	
	public Participant getParticipant(String userId, String partId, String evalId) throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {

		// Make sure we are passing in the ID, not the user name
		Long.parseLong(partId);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION+"/"+evalId+"/participant/" + partId);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
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
	
	public void deleteParticipant(String userId, String partId, String evalId) throws ServletException, IOException, DatastoreException, NotFoundException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION + "/" + evalId + "/participant/" + partId);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.NO_CONTENT.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
	}
	
	public PaginatedResults<Participant> getAllParticipants(String userId, String evalId) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION + "/" + evalId + "/participant");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
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
	
	public long getParticipantCount(String userId, String evalId) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION + "/" + evalId + "/participant/count");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		return Long.parseLong(response.getContentAsString());
	}
	
	public Submission createSubmission(Submission sub, String userId, String entityEtag)
			throws JSONObjectAdapterException, IOException, DatastoreException,
			NotFoundException, ServletException 
	{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.SUBMISSION);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter(AuthorizationConstants.ETAG_PARAM, entityEtag);
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
	
	public Submission getSubmission(String userId, String subId) throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.SUBMISSION+"/"+subId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
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
	
	public SubmissionStatus getSubmissionStatus(String userId, String subId) throws ServletException, IOException, DatastoreException, NotFoundException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.SUBMISSION+"/"+subId+"/status");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
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
	
	public PaginatedResults<Submission> getAllSubmissions(String userId, String evalId, SubmissionStatusEnum status) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION + "/" + evalId + "/submission/all");
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
	
	public long getSubmissionCount(String userId, String evalId) throws ServletException, IOException, JSONObjectAdapterException, NotFoundException, DatastoreException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI(UrlHelpers.EVALUATION + "/" + evalId + "/submission/count");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		// Read in the value.
		return Long.parseLong(response.getContentAsString());
	}

	public AccessControlList getEvaluationAcl(String userId, String evalId)
			throws ServletException, IOException, JSONObjectAdapterException {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI(UrlHelpers.EVALUATION + "/" + evalId + UrlHelpers.ACL);
		request.addHeader("Accept", "application/json");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);

		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherServlet.service(request, response);
		assertEquals(HttpStatus.OK.value(), response.getStatus());

		AccessControlList acl = EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), AccessControlList.class);
		return acl;
	}

	public AccessControlList updateEvaluationAcl(String userId, AccessControlList acl)
			throws ServletException, IOException, JSONObjectAdapterException {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("PUT");
		request.setRequestURI(UrlHelpers.EVALUATION_ACL);
		request.addHeader("Accept", "application/json");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		String body = EntityFactory.createJSONStringForEntity(acl);
		request.setContent(body.getBytes("UTF-8"));

		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherServlet.service(request, response);
		assertEquals(HttpStatus.OK.value(), response.getStatus());

		AccessControlList aclUpdated = EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), AccessControlList.class);
		return aclUpdated;
	}

	public UserEvaluationPermissions getEvaluationPermissions(String userId, String evalId)
			throws ServletException, IOException, JSONObjectAdapterException {

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.setRequestURI(UrlHelpers.EVALUATION + "/" + evalId + UrlHelpers.PERMISSIONS);
		request.addHeader("Accept", "application/json");
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);

		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherServlet.service(request, response);
		assertEquals(HttpStatus.OK.value(), response.getStatus());

		UserEvaluationPermissions uep = EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), UserEvaluationPermissions.class);
		return uep;
	}

	/**
	 * Get the migration counts
	 * @param userId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeCounts getMigrationTypeCounts(String userId) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/counts";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, MigrationTypeCounts.class);
	}
	
	/**
	 * Get the RowMetadata for a given Migration type.
	 * This is used to get all metadata from a source stack during migation.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public RowMetadataResult getRowMetadata(String userId, MigrationType type, long limit, long offset) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/rows";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("type", type.name());
		request.setParameter("limit", ""+limit);
		request.setParameter("offset", ""+offset);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, RowMetadataResult.class);
	}
	
	/**
	 * Get the RowMetadata for a given Migration type.
	 * This is used to get all metadata from a source stack during migation.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public RowMetadataResult getRowMetadataDelta(String userId, MigrationType type, IdList list) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/delta";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("type", type.name());
		String body = EntityFactory.createJSONStringForEntity(list);
		request.setContent(body.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, RowMetadataResult.class);
	}
	
	/**
	 * Get the RowMetadata for a given Migration type.
	 * This is used to get all metadata from a source stack during migation.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeList getPrimaryMigrationTypes(String userId) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/primarytypes";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, MigrationTypeList.class);
	}
	
	/**
	 * Start the backup of a list of objects.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public BackupRestoreStatus startBackup(String userId, MigrationType type, IdList list) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/backup";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("type", type.name());
		String body = EntityFactory.createJSONStringForEntity(list);
		request.setContent(body.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, BackupRestoreStatus.class);
	}
	
	/**
	 * Start the backup of a list of objects.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public BackupRestoreStatus startRestore(String userId, MigrationType type, RestoreSubmission sub) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/restore";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("type", type.name());
		String body = EntityFactory.createJSONStringForEntity(sub);
		request.setContent(body.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, BackupRestoreStatus.class);
	}
	
	/**
	 * Start the backup of a list of objects.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public BackupRestoreStatus getBackupRestoreStatus(String userId, String daemonId) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/status";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("daemonId", daemonId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, BackupRestoreStatus.class);
	}
	
	/**
	 * Start the backup of a list of objects.
	 * 
	 * @param userId
	 * @param type
	 * @param limit
	 * @param offset
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public MigrationTypeCount deleteMigrationType(String userId, MigrationType type, IdList list) throws ServletException, IOException, JSONObjectAdapterException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		String uri = "/migration/delete";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.setParameter("type", type.name());
		String body = EntityFactory.createJSONStringForEntity(list);
		request.setContent(body.getBytes("UTF-8"));
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		String resultString = response.getContentAsString();
		return EntityFactory.createEntityFromJSONString(resultString, MigrationTypeCount.class);
	}
	
	/**
	 * 
	 * @param userId
	 * @param toCreate
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public WikiPage createWikiPage(String userId, String ownerId, ObjectType ownerType, WikiPage toCreate) throws ServletException,
			IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("POST");
		request.addHeader("Accept", "application/json");
		String uri = "/"+ownerType.name().toLowerCase() + "/" + ownerId + "/wiki";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		String body;
		body = EntityFactory.createJSONStringForEntity(toCreate);
		request.setContent(body.getBytes("UTF-8"));
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.CREATED.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), WikiPage.class);
	}
	
	/**
	 * Delete a wikipage.
	 * @param key
	 * @param userName
	 * @throws ServletException
	 * @throws IOException
	 */
	public void deleteWikiPage(WikiPageKey key, String userName) throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("DELETE");
		request.addHeader("Accept", "application/json");
		String uri = createURI(key);
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
	}
	
	/**
	 * Get a wiki page.
	 * @param key
	 * @param useranme
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public WikiPage getWikiPage(WikiPageKey key, String useranme) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = createURI(key);
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, useranme);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), WikiPage.class);
	}
	
	/**
	 * Get the root wiki page.
	 * @param ownerId
	 * @param ownerType
	 * @param userName
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType, String userName) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/"+ownerType.name().toLowerCase() + "/" + ownerId + "/wiki";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), WikiPage.class);
	}
	
	/**
	 * Simple helper for creating a URI for a WikiPage using its key
	 * @param key
	 * @return
	 */
	public String createURI(WikiPageKey key) {
		return "/"+key.getOwnerObjectType().name().toLowerCase() + "/" + key.getOwnerObjectId() + "/wiki/"+key.getWikiPageId();
	}
	
	/**
	 * Update a wiki page.
	 * @param userName
	 * @param id
	 * @param entity
	 * @param wiki
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public WikiPage updateWikiPage(String userId, String ownerId, ObjectType ownerType, WikiPage wiki) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("PUT");
		request.addHeader("Accept", "application/json");
		String uri = "/"+ownerType.name().toLowerCase() + "/" + ownerId + "/wiki/"+wiki.getId();
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userId);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		String body;
		body = EntityFactory.createJSONStringForEntity(wiki);
		request.setContent(body.getBytes("UTF-8"));
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), WikiPage.class);
		
	}
	/**
	 * Get the paginated results of a wiki header.
	 * @param userName
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public PaginatedResults<WikiHeader> getWikiHeaderTree(String userName, String ownerId, ObjectType ownerType) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/"+ownerType.name().toLowerCase() + "/" + ownerId + "/wikiheadertree";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(response.getContentAsString());
		PaginatedResults<WikiHeader> result = new PaginatedResults<WikiHeader>(WikiHeader.class);
		result.initializeFromJSONObject(adapter);
		return result;
	}
	
	/**
	 * Get the paginated results of a wiki header.
	 * @param userName
	 * @param ownerId
	 * @param ownerType
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 * @throws JSONObjectAdapterException 
	 */
	public FileHandleResults getWikiFileHandles(String userName, WikiPageKey key) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = createURI(key)+"/attachmenthandles";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), FileHandleResults.class);
	}
	
	/**
	 * Get the file handles for the current version.
	 * @param userName
	 * @param entityId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public FileHandleResults geEntityFileHandlesForCurrentVersion(String userName, String entityId) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/entity/"+entityId+"/filehandles";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), FileHandleResults.class);
	}
	
	/**
	 * Get the file handles for a given version.
	 * @param userName
	 * @param entityId
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 * @throws JSONObjectAdapterException
	 */
	public FileHandleResults geEntityFileHandlesForVersion(String userName, String entityId, Long versionNumber) throws ServletException, IOException, JSONObjectAdapterException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = "/entity/"+entityId+"/version/"+versionNumber+"/filehandles";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		if (response.getStatus() != HttpStatus.OK.value()) {
			throw new ServletTestHelperException(response);
		}
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), FileHandleResults.class);
	}
	
	/**
	 * Get the temporary Redirect URL for a Wiki File.
	 * @param userName
	 * @param key
	 * @param fileName
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public URL getWikiAttachmentFileURL(String userName, WikiPageKey key, String fileName, Boolean redirect) throws ServletException, IOException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = createURI(key)+"/attachment";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.setParameter("fileName", fileName);
		if(redirect != null){
			request.setParameter("redirect", redirect.toString());
		}
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		return handleRedirectReponse(redirect, response);
	}
	
	/**
	 * Get the temporary Redirect URL for a Wiki File.
	 * @param userName
	 * @param key
	 * @param fileName
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public URL getWikiAttachmentPreviewFileURL(String userName, WikiPageKey key, String fileName, Boolean redirect) throws ServletException, IOException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String uri = createURI(key)+"/attachmentpreview";
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		request.setParameter("fileName", fileName);
		if(redirect != null){
			request.setParameter("redirect", redirect.toString());
		}
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		return handleRedirectReponse(redirect, response);
	}
	
	/**
	 * Get the temporary Redirect URL for a Wiki File.
	 * @param userName
	 * @param entityId
	 * @param redirect - Defaults to null, which will follow the redirect.  When set to FALSE, a call will be made without a redirect.
	 * @param preview - Defaults to null, wich will get the File and not the preview of the File.  When set to TRUE, the URL of the preview will be returned.
	 * @param versionNumber - Defaults to null, wich will get the file for the current version.  When set to a version number, the file (or preview) assocaited
	 * with that version number will be returned.
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	private URL getEntityFileURL(String userName, String entityId, Boolean redirect, Boolean preview, Long versionNumber) throws ServletException, IOException{
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		String suffix = "/file";
		if(Boolean.TRUE.equals(preview)){
			// This is a preview request.
			suffix = "/filepreview";
		}
		String version = "";
		if(versionNumber != null){
			version = "/version/"+versionNumber;
		}
		String uri = "/entity/"+entityId+version+suffix;
		request.setRequestURI(uri);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		if(redirect != null){
			request.setParameter("redirect", redirect.toString());
		}
		request.addHeader("Content-Type", "application/json; charset=UTF-8");
		DispatchServletSingleton.getInstance().service(request, response);
		return handleRedirectReponse(redirect, response);
	}
	
	/**
	 * 
	 * @param redirect
	 * @param response
	 * @return
	 * @throws MalformedURLException
	 * @throws UnsupportedEncodingException
	 */
	private URL handleRedirectReponse(Boolean redirect,	MockHttpServletResponse response) throws MalformedURLException,
			UnsupportedEncodingException {
		// Redirect response is different than non-redirect.
		if(redirect == null || Boolean.TRUE.equals(redirect)){
			if (response.getStatus() != HttpStatus.TEMPORARY_REDIRECT.value()) {
				throw new ServletTestHelperException(response);
			}
			// Get the redirect location
			return new URL(response.getRedirectedUrl());
		}else{
			// Redirect=false
			if (response.getStatus() != HttpStatus.OK.value()) {
				throw new ServletTestHelperException(response);
			}
			// Get the redirect location
			return new URL(response.getContentAsString());
		}
	}
	/**
	 * Get the file URL for the current version.
	 * @param userName
	 * @param entityId
	 * @param redirect
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public URL getEntityFileURLForCurrentVersion(String userName, String entityId, Boolean redirect) throws ServletException, IOException {
		Boolean preview = null;
		Long versionNumber = null;
		return getEntityFileURL(userName, entityId, redirect, preview, versionNumber);
	}
	
	/**
	 * Get the file preview URL for the current version.
	 * @param userName
	 * @param entityId
	 * @param redirect
	 * @return
	 * @throws ServletException
	 * @throws IOException
	 */
	public URL getEntityFilePreviewURLForCurrentVersion(String userName, String entityId, Boolean redirect) throws ServletException, IOException {
		Boolean preview = Boolean.TRUE;
		Long versionNumber = null;
		return getEntityFileURL(userName, entityId, redirect, preview, versionNumber);
	}
	/**
	 * 
	 * @param userName
	 * @param id
	 * @param versionNumber
	 * @param redirect
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 */
	public URL getEntityFileURLForVersion(String userName, String entityId, Long versionNumber, Boolean redirect) throws ServletException, IOException {
		Boolean preview = null;
		return getEntityFileURL(userName, entityId, redirect, preview, versionNumber);
	}
	
	/**
	 * 
	 * @param userName
	 * @param id
	 * @param versionNumber
	 * @param redirect
	 * @return
	 * @throws IOException 
	 * @throws ServletException 
	 */
	public URL getEntityFilePreviewURLForVersion(String userName, String entityId, Long versionNumber, Boolean redirect) throws ServletException, IOException {
		Boolean preview = Boolean.TRUE;
		return getEntityFileURL(userName, entityId, redirect, preview, versionNumber);
	}

	public BatchResults<EntityHeader> getEntityHeaderByMd5(String userName, String md5) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("GET");
		request.addHeader("Accept", "application/json");
		request.setRequestURI("/entity/md5/" + md5);
		request.setParameter(AuthorizationConstants.USER_ID_PARAM, userName);
		MockHttpServletResponse response = new MockHttpServletResponse();
		dispatcherServlet.service(request, response);
		log.debug("Results: " + response.getContentAsString());
		if (response.getStatus() != HttpStatus.OK.value()) {
			handleException(response.getStatus(), response.getContentAsString());
		}
		String jsonStr = response.getContentAsString();
		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(jsonStr);
		BatchResults<EntityHeader> results = new BatchResults<EntityHeader>(EntityHeader.class);
		results.initializeFromJSONObject(adapter);
		return results;
	}
}
