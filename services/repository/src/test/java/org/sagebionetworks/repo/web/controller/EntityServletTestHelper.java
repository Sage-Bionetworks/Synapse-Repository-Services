package org.sagebionetworks.repo.web.controller;

import java.net.URL;
import java.util.List;

import javax.servlet.http.HttpServlet;

import org.apache.commons.lang3.StringUtils;
import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.evaluation.model.Submission;
import org.sagebionetworks.evaluation.model.SubmissionStatus;
import org.sagebionetworks.evaluation.model.SubmissionStatusEnum;
import org.sagebionetworks.evaluation.model.UserEvaluationPermissions;
import org.sagebionetworks.reflection.model.PaginatedResults;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityBundle;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.IdList;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.RestResourceList;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.VersionableEntity;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.dao.WikiPageKey;
import org.sagebionetworks.repo.model.file.FileHandleResults;
import org.sagebionetworks.repo.model.migration.MigrationRangeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationType;
import org.sagebionetworks.repo.model.migration.MigrationTypeChecksum;
import org.sagebionetworks.repo.model.migration.MigrationTypeCount;
import org.sagebionetworks.repo.model.migration.MigrationTypeCounts;
import org.sagebionetworks.repo.model.migration.MigrationTypeList;
import org.sagebionetworks.repo.model.registry.EntityRegistry;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHeader;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiHistorySnapshot;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiOrderHint;
import org.sagebionetworks.repo.model.v2.wiki.V2WikiPage;
import org.sagebionetworks.repo.model.wiki.WikiHeader;
import org.sagebionetworks.repo.model.wiki.WikiPage;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ServletTestHelperUtils.HTTPMODE;
import org.sagebionetworks.schema.ObjectSchema;
import org.sagebionetworks.schema.adapter.JSONObjectAdapter;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.sagebionetworks.schema.adapter.org.json.JSONObjectAdapterImpl;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class EntityServletTestHelper {

	private HttpServlet dispatcherServlet = null;

	/**
	 * Setup the servlet, default test user, and entity list for test cleanup.
	 * 
	 * Create a Spring MVC DispatcherServlet so that we can test our URL
	 * mapping, request format, response format, and response status code.
	 * 
	 * @throws Exception
	 */
	public EntityServletTestHelper(HttpServlet dispatcherServlet) throws Exception {
		this.dispatcherServlet = dispatcherServlet;
	}

	/**
	 * Create an entity without an entity type
	 */
	public Entity createEntity(Entity entity, Long userId, String activityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.ENTITY, userId, entity);
		request.setParameter(ServiceConstants.GENERATED_BY_PARAM, activityId);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return ServletTestHelperUtils.readResponseEntity(response);
	}

	/**
	 * Delete an entity without knowing the type
	 */
	public void deleteEntity(String id, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.ENTITY + "/" + id, userId, null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.NO_CONTENT);
	}

	/**
	 * Get an entity using only the ID
	 */
	public Entity getEntity(String id, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponseEntity(response);
	}

	/**
	 * Get an entity bundle using only the ID
	 */
	public EntityBundle getEntityBundle(String id, int mask, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.BUNDLE,
				userId, null);
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
			Long versionNumber, int mask, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.VERSION
						+ "/" + versionNumber + UrlHelpers.BUNDLE, userId,
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
	public Entity updateEntity(Entity toUpdate, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + toUpdate.getId(),
				userId, toUpdate);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponseEntity(response);
	}

	/**
	 * Get the annotations for an entity.
	 */
	public Annotations getEntityAnnotations(String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id
						+ UrlHelpers.ANNOTATIONS, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), Annotations.class);
	}

	/**
	 * Update the annotations of an entity
	 */
	public Annotations updateAnnotations(Annotations annos, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + annos.getId()
						+ UrlHelpers.ANNOTATIONS, userId, annos);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), Annotations.class);
	}

	/**
	 * Get the user's permissions for an entity
	 */
	public UserEntityPermissions getUserEntityPermissions(String id,
			Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id
						+ UrlHelpers.PERMISSIONS, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), UserEntityPermissions.class);
	}

	/**
	 * Get the user's permissions for an entity.
	 */
	public EntityPath getEntityPath(String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.PATH,
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), EntityPath.class);
	}

	/**
	 * Get the types of entities
	 */
	public PaginatedResults<EntityHeader> getEntityTypeBatch(List<String> ids,
			Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY_TYPE, userId, null);
		request.setParameter(ServiceConstants.BATCH_PARAM, StringUtils.join(
				ids, ServiceConstants.BATCH_PARAM_VALUE_SEPARATOR));

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		String json = response.getContentAsString();
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(json);
		return PaginatedResults.createFromJSONObjectAdapter(adapter, EntityHeader.class);
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
	public VersionableEntity createNewVersion(Long userId, VersionableEntity entity)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + entity.getId()
						+ "/version", userId, entity);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), VersionableEntity.class);
	}

	// ///////////////////////
	// Evaluation Services //
	// ///////////////////////

	/**
	 * Creates an evaluation
	 */
	public Evaluation createEvaluation(Evaluation eval, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.EVALUATION, userId, eval);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return new Evaluation(ServletTestHelperUtils.readResponseJSON(response));
	}

	/**
	 * Gets an evaluation
	 */
	public Evaluation getEvaluation(Long userId, String evalId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId, userId,
				null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Evaluation(ServletTestHelperUtils.readResponseJSON(response));
	}

	/**
	 * Returns whether the user has access rights to the evaluation
	 */
	public Boolean canAccess(Long userId, String evalId,
			ACCESS_TYPE accessType) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId + "/access",
				userId, null);
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
	public Evaluation findEvaluation(Long userId, String name)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/name/" + name,
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Evaluation(ServletTestHelperUtils.readResponseJSON(response));
	}

	/**
	 * Gets a paginated list of available evaluations
	 */
	public PaginatedResults<Evaluation> getAvailableEvaluations(
			Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION_AVAILABLE, userId, null);
		request.setParameter("limit", "100");
		request.setParameter("offset", "0");

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				Evaluation.class);
	}

	public Evaluation updateEvaluation(Evaluation eval, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.EVALUATION + "/" + eval.getId(),
				userId, eval);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Evaluation(ServletTestHelperUtils.readResponseJSON(response));
	}

	public void deleteEvaluation(String evalId, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.EVALUATION + "/" + evalId,
				userId, null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.NO_CONTENT);
	}

	public PaginatedResults<Evaluation> getEvaluationsByContentSourcePaginated(
			Long userId, String id, long limit, long offset)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.EVALUATION,
				userId, null);
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
			Long userId, long limit, long offset) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION, userId, null);
		request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, ""
				+ offset);
		request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, ""
				+ limit);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				Evaluation.class);
	}

	public Submission createSubmission(Submission sub, Long userId,
			String entityEtag) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.SUBMISSION, userId, sub);
		request.setParameter(AuthorizationConstants.ETAG_PARAM, entityEtag);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return new Submission(ServletTestHelperUtils.readResponseJSON(response));
	}

	public Submission getSubmission(Long userId, String subId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.SUBMISSION + "/" + subId, userId,
				null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new Submission(ServletTestHelperUtils.readResponseJSON(response));
	}

	public SubmissionStatus getSubmissionStatus(Long userId, String subId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.SUBMISSION + "/" + subId + "/status",
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new SubmissionStatus(
				ServletTestHelperUtils.readResponseJSON(response));
	}

	public SubmissionStatus updateSubmissionStatus(SubmissionStatus subStatus,
			Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.SUBMISSION + "/" + subStatus.getId()
						+ "/status", userId, subStatus);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return new SubmissionStatus(
				ServletTestHelperUtils.readResponseJSON(response));
	}

	public void deleteSubmission(String subId, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.SUBMISSION + "/" + subId, userId,
				null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.NO_CONTENT);
	}

	public PaginatedResults<Submission> getAllSubmissions(Long userId,
			String evalId, SubmissionStatusEnum status) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ "/submission/all", userId, null);
		if (status != null) {
			request.setParameter(UrlHelpers.STATUS, status.toString());
		}

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				Submission.class);
	}

	public long getSubmissionCount(Long userId, String evalId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ "/submission/count", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return Long.parseLong(response.getContentAsString());
	}

	public AccessControlList getEvaluationAcl(Long userId, String evalId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ UrlHelpers.ACL, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), AccessControlList.class);
	}

	public AccessControlList updateEvaluationAcl(Long userId,
			AccessControlList acl) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.EVALUATION_ACL, userId, acl);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), AccessControlList.class);
	}

	public UserEvaluationPermissions getEvaluationPermissions(Long userId,
			String evalId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.EVALUATION + "/" + evalId
						+ UrlHelpers.PERMISSIONS, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), UserEvaluationPermissions.class);
	}

	public PaginatedResults<EntityHeader> getEntityHeaderByMd5(Long userId,
			String md5) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/entity/md5/" + md5, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		String json = response.getContentAsString();
		JSONObjectAdapterImpl adapter = new JSONObjectAdapterImpl(json);
		return PaginatedResults.createFromJSONObjectAdapter(adapter, EntityHeader.class);
	}

	/**
	 * Get the migration counts
	 */
	public MigrationTypeCounts getMigrationTypeCounts(Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/migration/counts", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), MigrationTypeCounts.class);
	}
	
	public MigrationTypeCount getMigrationTypeCount(Long userId, MigrationType type) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/migration/count", userId, null);
		request.setParameter("type", type.name());

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), MigrationTypeCount.class);
	}

	/**
	 * Get the RowMetadata for a given Migration type. This is used to get all
	 * metadata from a source stack during migation.
	 */
	public MigrationTypeList getPrimaryMigrationTypes(Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/migration/primarytypes", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), MigrationTypeList.class);
	}

	/**
	 * Deletes all objects of a given type
	 */
	public MigrationTypeCount deleteMigrationType(Long userId,
			MigrationType type, IdList list) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, "/migration/delete", userId, list);
		request.setParameter("type", type.name());

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), MigrationTypeCount.class);
	}
	
	/**
	 * Returns checksum for migration type and id range
	 * @throws Exception 
	 */
	public MigrationRangeChecksum getChecksumForIdRange(Long userId, MigrationType type,
			String salt, String minId, String maxId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/migration/rangechecksum", userId, null);
		request.setParameter("migrationType", type.name());
		request.setParameter("salt", salt);
		request.setParameter("minId", minId);
		request.setParameter("maxId", maxId);
		
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);
		
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), MigrationRangeChecksum.class);
		
	}
	
	public MigrationTypeChecksum getChecksumForType(Long userId, MigrationType type) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/migration/typechecksum", userId, null);
		request.setParameter("migrationType", type.name());
		
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);
		
		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), MigrationTypeChecksum.class);
	}

	public WikiPage createWikiPage(Long userId, String ownerId,
			ObjectType ownerType, WikiPage toCreate) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki", userId, toCreate);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), WikiPage.class);
	}

	/**
	 * Delete a wikipage
	 */
	public void deleteWikiPage(WikiPageKey key, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, ServletTestHelperUtils.createWikiURI(key),
				userId, null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.OK);
	}

	/**
	 * Get a wiki page.
	 */
	public WikiPage getWikiPage(WikiPageKey key, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createWikiURI(key),
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), WikiPage.class);
	}
	
	/**
	 * Get a wiki order hint.
	 */
	public V2WikiOrderHint getWikiOrderHint(Long userId, String ownerId, ObjectType type)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/" + type.name().toLowerCase() + "/"
						+ ownerId + "/wiki2orderhint",
				userId, null);
		
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), V2WikiOrderHint.class);
	}
	
	/**
	 * Get the root wiki page
	 */
	public WikiPage getRootWikiPage(String ownerId, ObjectType ownerType,
			Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), WikiPage.class);
	}

	/**
	 * Update a wiki page
	 */
	public WikiPage updateWikiPage(Long userId, String ownerId,
			ObjectType ownerType, WikiPage wiki) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki/" + wiki.getId(), userId, wiki);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), WikiPage.class);
	}
	
	/**
	 * Get the paginated results of a wiki header
	 */
	public PaginatedResults<WikiHeader> getWikiHeaderTree(Long userId,
			String ownerId, ObjectType ownerType) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wikiheadertree", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				WikiHeader.class);
	}

	/**
	 * Get the paginated results of a wiki header
	 */
	public FileHandleResults getWikiFileHandles(Long userId, WikiPageKey key)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createWikiURI(key)
						+ "/attachmenthandles", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), FileHandleResults.class);
	}

	/**
	 * Get the file handles for the current version
	 */
	public FileHandleResults geEntityFileHandlesForCurrentVersion(
			Long userId, String entityId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/entity/" + entityId + "/filehandles", userId,
				null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), FileHandleResults.class);
	}

	/**
	 * Get the file handles for a given version
	 */
	public FileHandleResults geEntityFileHandlesForVersion(Long userId,
			String entityId, Long versionNumber) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/entity/" + entityId + "/version/"
						+ versionNumber + "/filehandles", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), FileHandleResults.class);
	}

	/**
	 * Get the temporary Redirect URL for a Wiki File
	 */
	public URL getWikiAttachmentFileURL(Long userId, WikiPageKey key,
			String fileName, Boolean redirect) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createWikiURI(key)
						+ "/attachment", userId, null);
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
	public URL getWikiAttachmentPreviewFileURL(Long userId,
			WikiPageKey key, String fileName, Boolean redirect)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createWikiURI(key)
						+ "/attachmentpreview", userId, null);
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
	private URL getEntityFileURL(Long userId, String entityId,
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
				userId, null);
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
	public URL getEntityFileURLForCurrentVersion(Long userId,
			String entityId, Boolean redirect) throws Exception {
		Boolean preview = null;
		Long versionNumber = null;
		return getEntityFileURL(userId, entityId, redirect, preview,
				versionNumber);
	}

	/**
	 * Get the file preview URL for the current version
	 */
	public URL getEntityFilePreviewURLForCurrentVersion(Long userId,
			String entityId, Boolean redirect) throws Exception {
		Boolean preview = Boolean.TRUE;
		Long versionNumber = null;
		return getEntityFileURL(userId, entityId, redirect, preview,
				versionNumber);
	}

	public URL getEntityFileURLForVersion(Long userId, String entityId,
			Long versionNumber, Boolean redirect) throws Exception {
		Boolean preview = null;
		return getEntityFileURL(userId, entityId, redirect, preview,
				versionNumber);
	}

	public URL getEntityFilePreviewURLForVersion(Long userId,
			String entityId, Long versionNumber, Boolean redirect)
			throws Exception {
		Boolean preview = Boolean.TRUE;
		return getEntityFileURL(userId, entityId, redirect, preview,
				versionNumber);
	}
	
	// V2 Wiki methods
	/**
	 * Create V2 wiki page
	 */
	public V2WikiPage createV2WikiPage(Long userId, String ownerId,
			ObjectType ownerType, V2WikiPage toCreate) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki2", userId, toCreate);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.CREATED);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), V2WikiPage.class);
	}
	
	/**
	 * Get V2 wiki page
	 */
	public V2WikiPage getV2WikiPage(WikiPageKey key, Long userId, Long wikiVersion) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
			HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key),
			userId, null);

		if(wikiVersion != null) {
			request.setParameter("wikiVersion", String.valueOf(wikiVersion));
		}
		MockHttpServletResponse response = ServletTestHelperUtils
			.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(response.getContentAsString(), V2WikiPage.class);
	}
	
	/**
	 * Get the root V2 wiki page
	 */
	public V2WikiPage getRootV2WikiPage(String ownerId, ObjectType ownerType,
			Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki2", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), V2WikiPage.class);
	}
	
	/**
	 * Update a V2 wiki page
	 */
	public V2WikiPage updateWikiPage(Long userId, String ownerId,
			ObjectType ownerType, V2WikiPage wiki) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki2/" + wiki.getId(), userId, wiki);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), V2WikiPage.class);
	}
	
	/**
	 * Update an order hint
	 */
	public V2WikiOrderHint updateWikiOrderHint(Long userId, V2WikiOrderHint orderHint) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, "/" + orderHint.getOwnerObjectType().name().toLowerCase() + "/"
						+ orderHint.getOwnerId() + "/wiki2orderhint", userId, orderHint);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), V2WikiOrderHint.class);
	}

	/**
	 * Get the paginated results of a wiki header
	 */
	public PaginatedResults<V2WikiHeader> getV2WikiHeaderTree(Long userId,
			String ownerId, ObjectType ownerType) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wikiheadertree2", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				V2WikiHeader.class);
	}
	
	/**
	 * Get the paginated results of a wiki header
	 */
	public FileHandleResults getV2WikiFileHandles(Long userId, WikiPageKey key,
			Long wikiVersion)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key)
						+ "/attachmenthandles", userId, null);

		if(wikiVersion != null) {
			request.setParameter("wikiVersion", String.valueOf(wikiVersion));
		}
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), FileHandleResults.class);
	}
	
	/**
	 * Get the temporary Redirect URL for a Wiki File
	 * @param wikiVersion TODO
	 */
	public URL getV2WikiAttachmentFileURL(Long userId, WikiPageKey key,
			String fileName, Boolean redirect, Long wikiVersion) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key)
						+ "/attachment", userId, null);
		request.setParameter("fileName", fileName);
		if (redirect != null) {
			request.setParameter("redirect", redirect.toString());
		}
		if(wikiVersion != null) {
			request.setParameter("wikiVersion", String.valueOf(wikiVersion));
		}

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, null);

		return ServletTestHelperUtils.handleRedirectReponse(redirect, response);
	}

	/**
	 * Get the temporary Redirect URL for a Wiki File
	 * @param wikiVersion TODO
	 */
	public URL getV2WikiAttachmentPreviewFileURL(Long userId,
			WikiPageKey key, String fileName, Boolean redirect, Long wikiVersion)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key)
						+ "/attachmentpreview", userId, null);
		request.setParameter("fileName", fileName);
		if (redirect != null) {
			request.setParameter("redirect", redirect.toString());
		}
		if(wikiVersion != null) {
			request.setParameter("wikiVersion", String.valueOf(wikiVersion));
		}
		
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, null);

		return ServletTestHelperUtils.handleRedirectReponse(redirect, response);
	}
	
	/**
	 * Get the temporary Redirect URL for a Wiki's markdown
	 */
	public URL getV2WikiMarkdownFileURL(Long userId, WikiPageKey key,
			Long wikiVersion, Boolean redirect) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key)
						+ "/markdown", userId, null);
		if(wikiVersion != null) {
			request.setParameter("wikiVersion", String.valueOf(wikiVersion));
		}
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
	public void deleteV2WikiPage(WikiPageKey key, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, ServletTestHelperUtils.createV2WikiURI(key),
				userId, null);

		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request,
				HttpStatus.OK);
	}
	
	/**
	 * Restore a V2 wiki page to other content
	 */
	public V2WikiPage restoreWikiPage(Long userId, String ownerId,
			ObjectType ownerType, V2WikiPage wiki, Long version) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, "/" + ownerType.name().toLowerCase() + "/"
						+ ownerId + "/wiki2/" + wiki.getId() + "/" + version, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), V2WikiPage.class);
	}
	
	/**
	 * Get wiki history
	 */
	public PaginatedResults<V2WikiHistorySnapshot> getV2WikiHistory(WikiPageKey key, Long userId, Long offset, Long limit) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, ServletTestHelperUtils.createV2WikiURI(key) + "/wikihistory",
				userId, null);
		request.setParameter("offset", offset.toString());
		request.setParameter("limit", limit.toString());
		MockHttpServletResponse response = ServletTestHelperUtils
			.dispatchRequest(dispatcherServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				V2WikiHistorySnapshot.class);
	}

	public void cancelSubmission(Long userId, String subId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.SUBMISSION+"/"+subId+"/cancellation",
				userId, null);
		ServletTestHelperUtils.dispatchRequest(dispatcherServlet, request, HttpStatus.NO_CONTENT);
	}
}
