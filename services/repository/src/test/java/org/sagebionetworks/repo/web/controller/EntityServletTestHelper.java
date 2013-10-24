package org.sagebionetworks.repo.web.controller;

import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServlet;

import org.apache.commons.lang.StringUtils;
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
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.daemon.RestoreSubmission;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.migration.IdList;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.migration.RowMetadataResult;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ServletTestHelperUtils.HTTPMODE;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
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
public class EntityServletTestHelper {

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
	 * Create an entity without an entity type
	 */
	public Entity createEntity(Entity entity, String username, String activityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.ENTITY, username, entity);
		request.setParameter(ServiceConstants.GENERATED_BY_PARAM, activityId);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return ServletTestHelperUtils.readResponseEntity(response);
	}

	/**
	 * Delete an entity without knowing the type
	 */
	public void deleteEntity(String id, String username) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.ENTITY + "/" + id, username, null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.NO_CONTENT);
	}

	/**
	 * Get an entity using only the ID
	 */
	public Entity getEntity(String id, String username) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id, username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponseEntity(response);
	}

	/**
	 * Get an entity bundle using only the ID
	 */
	public EntityBundle getEntityBundle(String id, int mask, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.BUNDLE,
				username, null);
		request.setParameter("mask", "" + mask);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new EntityBundle(
				ServletTestHelperUtils.readResponseJSON(response));
	}

	/**
	 * Get an entity bundle for a specific version using the ID and
	 * versionNumber.
	 */
	public EntityBundle getEntityBundleForVersion(String id,
			Long versionNumber, int mask, String username) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.VERSION
						+ "/" + versionNumber + UrlHelpers.BUNDLE, username,
				null);
		request.setParameter("mask", "" + mask);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new EntityBundle(
				ServletTestHelperUtils.readResponseJSON(response));
	}

	/**
	 * Update an entity.
	 */
	public Entity updateEntity(Entity toUpdate, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + toUpdate.getId(),
				username, toUpdate);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponseEntity(response);
	}

	/**
	 * Get the annotations for an entity.
	 */
	public Annotations getEntityAnnotations(String id, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id
						+ UrlHelpers.ANNOTATIONS, username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), Annotations.class);
	}

	/**
	 * Update the annotations of an entity
	 */
	public Annotations updateAnnotations(Annotations annos, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + annos.getId()
						+ UrlHelpers.ANNOTATIONS, username, annos);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), Annotations.class);
	}

	/**
	 * Get the user's permissions for an entity
	 */
	public UserEntityPermissions getUserEntityPermissions(String id,
			String username) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id
						+ UrlHelpers.PERMISSIONS, username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), UserEntityPermissions.class);
	}

	/**
	 * Get the user's permissions for an entity.
	 */
	public EntityPath getEntityPath(String id, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.PATH,
				username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), EntityPath.class);
	}

	/**
	 * Get the types of entities
	 */
	public BatchResults<EntityHeader> getEntityTypeBatch(List<String> ids,
			String username) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY_TYPE, username, null);
		request.setParameter(ServiceConstants.BATCH_PARAM, StringUtils.join(
				ids, ServiceConstants.BATCH_PARAM_VALUE_SEPARATOR));

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		JSONObjectAdapter adapter = new JSONObjectAdapterImpl(new JSONObject(
				response.getContentAsString()));
		BatchResults<EntityHeader> results = new BatchResults<EntityHeader>(
				EntityHeader.class);
		results.initializeFromJSONObject(adapter);
		return results;
	}

	/**
	 * Get the list of all REST resources
	 */
	public RestResourceList getRESTResources() throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.REST_RESOURCES, null, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), RestResourceList.class);
	}

	/**
	 * Get the effective schema for a resource
	 */
	public ObjectSchema getEffectiveSchema(String resourceId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.REST_RESOURCES
						+ UrlHelpers.EFFECTIVE_SCHEMA, null, null);
		request.addParameter(UrlHelpers.RESOURCE_ID, resourceId);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), ObjectSchema.class);
	}

	/**
	 * Get the full schema for a resource
	 */
	public ObjectSchema getFullSchema(String resourceId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.REST_RESOURCES + UrlHelpers.SCHEMA,
				null, null);
		request.addParameter(UrlHelpers.RESOURCE_ID, resourceId);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), ObjectSchema.class);
	}

	/**
	 * Get the entity registry
	 */
	public EntityRegistry getEntityRegistry() throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + UrlHelpers.REGISTRY, null,
				null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), EntityRegistry.class);
	}

	/**
	 * Creates a new version of an entity
	 */
	public Versionable createNewVersion(String username, Versionable entity)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + entity.getId()
						+ "/version", username, entity);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), Versionable.class);
	}

	// ///////////////////////
	// Evaluation Services //
	// ///////////////////////

	/**
	 * Creates an evaluation
	 */
	public Evaluation createEvaluation(Evaluation eval, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.EVALUATION, username, eval);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return new Evaluation(ServletTestHelperUtils.readResponseJSON(response));
	}

	/**
	 * Gets an evaluation
	 */
	public Evaluation getEvaluation(String username, String evalId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId, username,
				null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Evaluation(ServletTestHelperUtils.readResponseJSON(response));
	}

	/**
	 * Returns whether the user has access rights to the evaluation
	 */
	public Boolean canAccess(String username, String evalId,
			ACCESS_TYPE accessType) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId + "/access",
				username, null);
		request.addParameter("accessType", accessType.toString());

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		JSONObjectAdapter joa = ServletTestHelperUtils
				.readResponseJSON(response);
		return (Boolean) joa.get("result");
	}

	/**
	 * Looks for an evaluation by name
	 */
	public Evaluation findEvaluation(String username, String name)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/name/" + name,
				username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Evaluation(ServletTestHelperUtils.readResponseJSON(response));
	}

	/**
	 * Gets a paginated list of available evaluations
	 */
	public PaginatedResults<Evaluation> getAvailableEvaluations(
			String username, String status) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION_AVAILABLE, username, null);
		request.setParameter("limit", "100");
		request.setParameter("offset", "0");
		if (status != null) {
			request.setParameter("status", status);
		}

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				Evaluation.class);
	}

	public Evaluation updateEvaluation(Evaluation eval, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.EVALUATION + "/" + eval.getId(),
				username, eval);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Evaluation(ServletTestHelperUtils.readResponseJSON(response));
	}

	public void deleteEvaluation(String evalId, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.EVALUATION + "/" + evalId,
				username, null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.NO_CONTENT);
	}

	public PaginatedResults<Evaluation> getEvaluationsByContentSourcePaginated(
			String username, String id, long limit, long offset)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.EVALUATION,
				username, null);
		request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, ""
				+ offset);
		request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, ""
				+ limit);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				Evaluation.class);
	}

	public PaginatedResults<Evaluation> getEvaluationsPaginated(
			String username, long limit, long offset) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION, username, null);
		request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, ""
				+ offset);
		request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, ""
				+ limit);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				Evaluation.class);
	}

	public long getEvaluationCount(String username) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION_COUNT, username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return Long.parseLong(response.getContentAsString());
	}

	public Participant createParticipant(String username, String evalId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.EVALUATION + "/" + evalId
						+ "/participant", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return new Participant(
				ServletTestHelperUtils.readResponseJSON(response));
	}

	public Participant getParticipant(String username, String partId,
			String evalId) throws Exception {
		// Make sure we are passing in the ID, not the user name
		Long.parseLong(partId);

		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ "/participant/" + partId, username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Participant(
				ServletTestHelperUtils.readResponseJSON(response));
	}

	public void deleteParticipant(String username, String partId, String evalId)
			throws Exception {
		// Make sure we are passing in the ID, not the user name
		Long.parseLong(partId);

		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.EVALUATION + "/" + evalId
						+ "/participant/" + partId, username, null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.NO_CONTENT);
	}

	public PaginatedResults<Participant> getAllParticipants(String username,
			String evalId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ "/participant", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				Participant.class);
	}

	public long getParticipantCount(String username, String evalId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ "/participant/count", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return Long.parseLong(response.getContentAsString());
	}

	public Submission createSubmission(Submission sub, String username,
			String entityEtag) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.SUBMISSION, username, sub);
		request.setParameter(AuthorizationConstants.ETAG_PARAM, entityEtag);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return new Submission(ServletTestHelperUtils.readResponseJSON(response));
	}

	public Submission getSubmission(String username, String subId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.SUBMISSION + "/" + subId, username,
				null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Submission(ServletTestHelperUtils.readResponseJSON(response));
	}

	public SubmissionStatus getSubmissionStatus(String username, String subId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.SUBMISSION + "/" + subId + "/status",
				username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new SubmissionStatus(
				ServletTestHelperUtils.readResponseJSON(response));
	}

	public SubmissionStatus updateSubmissionStatus(SubmissionStatus subStatus,
			String username) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.SUBMISSION + "/" + subStatus.getId()
						+ "/status", username, subStatus);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new SubmissionStatus(
				ServletTestHelperUtils.readResponseJSON(response));
	}

	public void deleteSubmission(String subId, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.SUBMISSION + "/" + subId, username,
				null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.NO_CONTENT);
	}

	public PaginatedResults<Submission> getAllSubmissions(String username,
			String evalId, SubmissionStatusEnum status) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ "/submission/all", username, null);
		if (status != null) {
			request.setParameter(UrlHelpers.STATUS, status.toString());
		}

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				Submission.class);
	}

	public long getSubmissionCount(String username, String evalId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ "/submission/count", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return Long.parseLong(response.getContentAsString());
	}

	public AccessControlList getEvaluationAcl(String username, String evalId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ UrlHelpers.ACL, username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), AccessControlList.class);
	}

	public AccessControlList updateEvaluationAcl(String username,
			AccessControlList acl) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.EVALUATION_ACL, username, acl);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), AccessControlList.class);
	}

	public UserEvaluationPermissions getEvaluationPermissions(String username,
			String evalId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ UrlHelpers.PERMISSIONS, username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), UserEvaluationPermissions.class);
	}

	public BatchResults<EntityHeader> getEntityHeaderByMd5(String username,
			String md5) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/entity/md5/" + md5, username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		JSONObjectAdapter adapter = ServletTestHelperUtils
				.readResponseJSON(response);
		BatchResults<EntityHeader> results = new BatchResults<EntityHeader>(
				EntityHeader.class);
		results.initializeFromJSONObject(adapter);
		return results;
	}

	/**
	 * Get the migration counts
	 */
	public MigrationTypeCounts getMigrationTypeCounts(String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/migration/counts", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), MigrationTypeCounts.class);
	}

	/**
	 * Get the RowMetadata for a given Migration type. This is used to get all
	 * metadata from a source stack during migation.
	 */
	public RowMetadataResult getRowMetadata(String username,
			MigrationType type, long limit, long offset) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/migration/rows", username, null);
		request.setParameter("type", type.name());
		request.setParameter("limit", "" + limit);
		request.setParameter("offset", "" + offset);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), RowMetadataResult.class);
	}

	/**
	 * Get the RowMetadata for a given Migration type. This is used to get all
	 * metadata from a source stack during migation.
	 */
	public RowMetadataResult getRowMetadataDelta(String username,
			MigrationType type, IdList list) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/migration/delta", username, list);
		request.setParameter("type", type.name());

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), RowMetadataResult.class);
	}

	/**
	 * Get the RowMetadata for a given Migration type. This is used to get all
	 * metadata from a source stack during migation.
	 */
	public MigrationTypeList getPrimaryMigrationTypes(String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/migration/primarytypes", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), MigrationTypeList.class);
	}

	/**
	 * Start the backup of a list of objects
	 */
	public BackupRestoreStatus startBackup(String username, MigrationType type,
			IdList list) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, "/migration/backup", username, list);
		request.setParameter("type", type.name());

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), BackupRestoreStatus.class);
	}

	/**
	 * Start the restore of a list of objects
	 */
	public BackupRestoreStatus startRestore(String username,
			MigrationType type, RestoreSubmission sub) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, "/migration/restore", username, sub);
		request.setParameter("type", type.name());

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), BackupRestoreStatus.class);
	}

	/**
	 * Get the status of a migration operation
	 */
	public BackupRestoreStatus getBackupRestoreStatus(String username,
			String daemonId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/migration/status", username, null);
		request.setParameter("daemonId", daemonId);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), BackupRestoreStatus.class);
	}

	/**
	 * Deletes all objects of a given type
	 */
	public MigrationTypeCount deleteMigrationType(String username,
			MigrationType type, IdList list) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, "/migration/delete", username, list);
		request.setParameter("type", type.name());

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), MigrationTypeCount.class);
	}

	public WikiPage createWikiPage(String username, String ownerId,
			ObjectType ownerType, WikiPage toCreate) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki", username, toCreate);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), WikiPage.class);
	}

	/**
	 * Delete a wikipage
	 */
	public void deleteWikiPage(WikiPageKey key, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, ServletTestHelperUtils.createWikiURI(key),
				username, null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.OK);
	}

	/**
	 * Get a wiki page.
	 */
	public WikiPage getWikiPage(WikiPageKey key, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createWikiURI(key),
				username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), WikiPage.class);
	}

	/**
	 * Get the root wiki page
	 */
	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType,
			String username) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), WikiPage.class);
	}

	/**
	 * Update a wiki page
	 */
	public WikiPage updateWikiPage(String username, String ownerId,
			ObjectType ownerType, WikiPage wiki) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki/" + wiki.getId(), username, wiki);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), WikiPage.class);
	}

	/**
	 * Get the paginated results of a wiki header
	 */
	public PaginatedResults<WikiHeader> getWikiHeaderTree(String username,
			String ownerId, ObjectType ownerType) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wikiheadertree", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				WikiHeader.class);
	}

	/**
	 * Get the paginated results of a wiki header
	 */
	public FileHandleResults getWikiFileHandles(String username, WikiPageKey key)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createWikiURI(key)
						+ "/attachmenthandles", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), FileHandleResults.class);
	}

	/**
	 * Get the file handles for the current version
	 */
	public FileHandleResults geEntityFileHandlesForCurrentVersion(
			String username, String entityId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/entity/" + entityId + "/filehandles", username,
				null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), FileHandleResults.class);
	}

	/**
	 * Get the file handles for a given version
	 */
	public FileHandleResults geEntityFileHandlesForVersion(String username,
			String entityId, Long versionNumber) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/entity/" + entityId + "/version/"
						+ versionNumber + "/filehandles", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), FileHandleResults.class);
	}

	/**
	 * Get the temporary Redirect URL for a Wiki File
	 */
	public URL getWikiAttachmentFileURL(String username, WikiPageKey key,
			String fileName, Boolean redirect) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createWikiURI(key)
						+ "/attachment", username, null);
		request.setParameter("fileName", fileName);
		if (redirect != null) {
			request.setParameter("redirect", redirect.toString());
		}

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, null);

		return ServletTestHelperUtils.handleRedirectReponse(redirect, response);
	}

	/**
	 * Get the temporary Redirect URL for a Wiki File
	 */
	public URL getWikiAttachmentPreviewFileURL(String username,
			WikiPageKey key, String fileName, Boolean redirect)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createWikiURI(key)
						+ "/attachmentpreview", username, null);
		request.setParameter("fileName", fileName);
		if (redirect != null) {
			request.setParameter("redirect", redirect.toString());
		}

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, null);

		return ServletTestHelperUtils.handleRedirectReponse(redirect, response);
	}

	/**
	 * Get the temporary Redirect URL for a Wiki File
	 * 
	 * @param redirect
	 *            - Defaults to null, which will follow the redirect. When set
	 *            to FALSE, a call will be made without a redirect.
	 * @param preview
	 *            - Defaults to null, which will get the File and not the
	 *            preview of the File. When set to TRUE, the URL of the preview
	 *            will be returned.
	 * @param versionNumber
	 *            - Defaults to null, which will get the file for the current
	 *            version. When set to a version number, the file (or preview)
	 *            associated with that version number will be returned.
	 */
	private URL getEntityFileURL(String username, String entityId,
			Boolean redirect, Boolean preview, Long versionNumber)
			throws Exception {
		String suffix = "/file";
		if (Boolean.TRUE.equals(preview)) {
			// This is a preview request
			suffix = "/filepreview";
		}
		String version = "";
		if (versionNumber != null) {
			version = "/version/" + versionNumber;
		}
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/entity/" + entityId + version + suffix,
				username, null);
		if (redirect != null) {
			request.setParameter("redirect", redirect.toString());
		}

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, null);

		return ServletTestHelperUtils.handleRedirectReponse(redirect, response);
	}

	/**
	 * Get the file URL for the current version
	 */
	public URL getEntityFileURLForCurrentVersion(String userName,
			String entityId, Boolean redirect) throws Exception {
		Boolean preview = null;
		Long versionNumber = null;
		return getEntityFileURL(userName, entityId, redirect, preview,
				versionNumber);
	}

	/**
	 * Get the file preview URL for the current version
	 */
	public URL getEntityFilePreviewURLForCurrentVersion(String userName,
			String entityId, Boolean redirect) throws Exception {
		Boolean preview = Boolean.TRUE;
		Long versionNumber = null;
		return getEntityFileURL(userName, entityId, redirect, preview,
				versionNumber);
	}

	public URL getEntityFileURLForVersion(String userName, String entityId,
			Long versionNumber, Boolean redirect) throws Exception {
		Boolean preview = null;
		return getEntityFileURL(userName, entityId, redirect, preview,
				versionNumber);
	}

	public URL getEntityFilePreviewURLForVersion(String userName,
			String entityId, Long versionNumber, Boolean redirect)
			throws Exception {
		Boolean preview = Boolean.TRUE;
		return getEntityFileURL(userName, entityId, redirect, preview,
				versionNumber);
	}
	
	// V2 Wiki methods
	/**
	 * Create V2 wiki page
	 */
	public V2WikiPage createV2WikiPage(String username, String ownerId,
			ObjectType ownerType, V2WikiPage toCreate) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki2", username, toCreate);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), V2WikiPage.class);
	}
	
	/**
	 * Get V2 wiki page
	 */
	public V2WikiPage getV2WikiPage(WikiPageKey key, String username) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
			HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key),
			username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
			.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), V2WikiPage.class);
	}
	
	/**
	 * Get the root V2 wiki page
	 */
	public V2WikiPage getRootV2WikiPage(String ownerId, ObjectType ownerType,
			String username) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki2", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), V2WikiPage.class);
	}
	
	/**
	 * Update a V2 wiki page
	 */
	public V2WikiPage updateWikiPage(String username, String ownerId,
			ObjectType ownerType, V2WikiPage wiki) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki2/" + wiki.getId(), username, wiki);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), V2WikiPage.class);
	}

	/**
	 * Get the paginated results of a wiki header
	 */
	public PaginatedResults<V2WikiHeader> getV2WikiHeaderTree(String username,
			String ownerId, ObjectType ownerType) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wikiheadertree2", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				V2WikiHeader.class);
	}
	
	/**
	 * Get the paginated results of a wiki header
	 */
	public FileHandleResults getV2WikiFileHandles(String username, WikiPageKey key)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key)
						+ "/attachmenthandles", username, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), FileHandleResults.class);
	}
	
	/**
	 * Get the temporary Redirect URL for a Wiki File
	 */
	public URL getV2WikiAttachmentFileURL(String username, WikiPageKey key,
			String fileName, Boolean redirect) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key)
						+ "/attachment", username, null);
		request.setParameter("fileName", fileName);
		if (redirect != null) {
			request.setParameter("redirect", redirect.toString());
		}

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, null);

		return ServletTestHelperUtils.handleRedirectReponse(redirect, response);
	}

	/**
	 * Get the temporary Redirect URL for a Wiki File
	 */
	public URL getV2WikiAttachmentPreviewFileURL(String username,
			WikiPageKey key, String fileName, Boolean redirect)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key)
						+ "/attachmentpreview", username, null);
		request.setParameter("fileName", fileName);
		if (redirect != null) {
			request.setParameter("redirect", redirect.toString());
		}

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, null);

		return ServletTestHelperUtils.handleRedirectReponse(redirect, response);
	}
	
	/**
	 * Delete a wikipage
	 */
	public void deleteV2WikiPage(WikiPageKey key, String username)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, ServletTestHelperUtils.createV2WikiURI(key),
				username, null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.OK);
	}
	
	/**
	 * Restore a V2 wiki page to other content
	 */
	public V2WikiPage restoreWikiPage(String username, String ownerId,
			ObjectType ownerType, V2WikiPage wiki, Long version) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki2/" + wiki.getId() + "/" + version, username, wiki);
		//request.setParameter("version", version.toString());
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), V2WikiPage.class);
	}
	
	/**
	 * Get wiki history
	 */
	public PaginatedResults<V2WikiHistorySnapshot> getV2WikiHistory(WikiPageKey key, String username, Long offset, Long limit) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key) + "/wikihistory",
				username, null);
		request.setParameter("offset", offset.toString());
		request.setParameter("limit", limit.toString());
		MockHttpServletResponse response = ServletTestHelperUtils
			.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				V2WikiHistorySnapshot.class);
	}

}
