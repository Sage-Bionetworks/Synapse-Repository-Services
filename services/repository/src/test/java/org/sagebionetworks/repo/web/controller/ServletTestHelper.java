package org.sagebionetworks.repo.web.controller;

import static org.junit.Assert.assertNotNull;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.junit.Assert;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.ACLInheritanceException;
import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationConstants.BOOTSTRAP_PRINCIPAL;
import org.sagebionetworks.repo.model.BooleanResult;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityIdList;
import org.sagebionetworks.repo.model.EntityPath;
import org.sagebionetworks.repo.model.MembershipInvtnSubmission;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.ServiceConstants;
import org.sagebionetworks.repo.model.ServiceConstants.AttachmentType;
import org.sagebionetworks.repo.model.Team;
import org.sagebionetworks.repo.model.TrashedEntity;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserGroupHeaderResponsePage;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.UserProfile;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.Versionable;
import org.sagebionetworks.repo.model.attachment.PresignedUrl;
import org.sagebionetworks.repo.model.attachment.S3AttachmentToken;
import org.sagebionetworks.repo.model.auth.UserEntityPermissions;
import org.sagebionetworks.repo.model.daemon.BackupRestoreStatus;
import org.sagebionetworks.repo.model.doi.Doi;
import org.sagebionetworks.repo.model.file.ExternalFileHandle;
import org.sagebionetworks.repo.model.file.S3FileHandle;
import org.sagebionetworks.repo.model.message.MessageBundle;
import org.sagebionetworks.repo.model.message.MessageRecipientSet;
import org.sagebionetworks.repo.model.message.MessageSortBy;
import org.sagebionetworks.repo.model.message.MessageStatus;
import org.sagebionetworks.repo.model.message.MessageStatusType;
import org.sagebionetworks.repo.model.message.MessageToUser;
import org.sagebionetworks.repo.model.ontology.Concept;
import org.sagebionetworks.repo.model.ontology.ConceptResponsePage;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.search.SearchResults;
import org.sagebionetworks.repo.model.search.query.SearchQuery;
import org.sagebionetworks.repo.model.status.StackStatus;
import org.sagebionetworks.repo.model.storage.StorageUsage;
import org.sagebionetworks.repo.model.storage.StorageUsageSummaryList;
import org.sagebionetworks.repo.model.table.ColumnModel;
import org.sagebionetworks.repo.model.table.PaginatedColumnModels;
import org.sagebionetworks.repo.model.table.RowReferenceSet;
import org.sagebionetworks.repo.model.table.RowSet;
import org.sagebionetworks.repo.model.versionInfo.SynapseVersionInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.controller.ServletTestHelperUtils.HTTPMODE;
import org.sagebionetworks.repo.web.service.EntityService;
import org.sagebionetworks.schema.adapter.org.json.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Helper class to make HttpServlet request.
 * 
 * Users can use all the static methods if they like.
 * 
 * Alternatively the instance methods add a level of convenience by managing the
 * deletion of entities created during testing and also the user account(s) to
 * be used during testing.
 * 
 * @author jmhill
 * 
 */
public class ServletTestHelper {

	private static final EntityObjectMapper objectMapper = new EntityObjectMapper();

	// Used for cleanup
	@Autowired
	private EntityService entityController;

	@Autowired
	private UserManager userManager;

	private static HttpServlet dispatchServlet = null;
	private UserInfo testUser = null;
	private List<String> toDelete = null;
	private Long userId = null;

	/**
	 * Setup the servlet, default test user, and entity list for test cleanup.
	 * 
	 * Create a Spring MVC DispatcherServlet so that we can test our URL
	 * mapping, request format, response format, and response status code.
	 * 
	 * @throws Exception
	 */
	public void setUp() throws Exception {
		dispatchServlet = DispatchServletSingleton.getInstance();
		assertNotNull(entityController);
		toDelete = new ArrayList<String>();

		this.setTestUser(BOOTSTRAP_PRINCIPAL.ANONYMOUS_USER.getPrincipalId());
	}

	/**
	 * Change the test user
	 */
	public void setTestUser(Long userId) throws Exception {
		// Make sure we have a valid user.
		testUser = userManager.getUserInfo(userId);
		UserInfo.validateUserInfo(testUser);
		this.userId = userId;
	}

	public UserInfo getTestUser() throws Exception {
		return testUser;
	}

	/**
	 * Cleanup the created entities and destroy the servlet
	 */
	public void tearDown() throws Exception {
		if (entityController != null && toDelete != null) {
			for (String idToDelete : toDelete) {
				try {
					entityController
							.deleteEntity(BOOTSTRAP_PRINCIPAL.THE_ADMIN_USER
									.getPrincipalId(), idToDelete);
				} catch (NotFoundException e) {
					// nothing to do here
				} catch (DatastoreException e) {
					// nothing to do here.
				}
			}
		}
	}

	public <T extends Entity> T createEntity(T entity,
			Map<String, String> extraParams) throws Exception {
		T returnedEntity = ServletTestHelper.createEntity(dispatchServlet,
				entity, userId, extraParams);
		toDelete.add(returnedEntity.getId());
		return returnedEntity;
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> T createObject(String uri, T object)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, uri, userId, null);

		StringWriter out = new StringWriter();
		objectMapper.writeValue(out, object);
		String body = out.toString();
		// TODO why is this adding the jsonschema property?
		JSONObject obj = new JSONObject(body);
		obj.remove("jsonschema");
		body = obj.toString();
		request.setContent(body.getBytes("UTF-8"));

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return (T) objectMapper.readValue(response.getContentAsString(),
				object.getClass());
	}

	@SuppressWarnings("unchecked")
	public <T extends Entity> T getEntity(T entity,
			Map<String, String> extraParams) throws Exception {
		return (T) getEntityById(entity.getClass(), entity.getId(), extraParams);
	}

	public <T extends Entity> T getEntityById(Class<? extends T> clazz,
			String id, Map<String, String> extraParams) throws Exception {
		return ServletTestHelper.getEntity(dispatchServlet, clazz, id, userId,
				extraParams);
	}

	public <T extends Entity> T updateEntity(T entity,
			Map<String, String> extraParams) throws Exception {
		return ServletTestHelper.updateEntity(dispatchServlet, entity, userId,
				extraParams);
	}

	public <T extends Entity> void deleteEntity(Class<? extends T> clazz,
			String id, Map<String, String> extraParams) throws Exception {
		ServletTestHelper.deleteEntity(dispatchServlet, clazz, id, userId,
				extraParams);
	}

	public QueryResults<Map<String, Object>> query(String query)
			throws Exception {
		return ServletTestHelper.query(dispatchServlet, query, userId);
	}

	public <T extends Entity> AccessControlList getEntityACL(T entity)
			throws Exception {
		return ServletTestHelper.getEntityACL(dispatchServlet, entity.getId(),
				userId);
	}

	public <T extends Entity> AccessControlList updateEntityAcl(T entity,
			AccessControlList entityACL) throws Exception {
		return ServletTestHelper.updateEntityAcl(dispatchServlet,
				entity.getId(), entityACL, userId);
	}

	public SearchResults getSearchResults(Map<String, String> params)
			throws Exception {
		return ServletTestHelper.getSearchResults(dispatchServlet, userId,
				params);
	}

	/**
	 * Create the passed entity by making a request to the passed servlet
	 */
	public static <T extends Entity> T createEntity(
			HttpServlet dispatchServlet, T entity, Long userId)
			throws Exception {
		return ServletTestHelper.createEntity(dispatchServlet, entity, userId,
				null);
	}

	/**
	 * Create the passed entity by making a request to the passed servlet
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Entity> T createEntity(
			HttpServlet dispatchServlet, T entity, Long userId,
			Map<String, String> extraParams) throws Exception {
		entity.setEntityType(entity.getClass().getName());

		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.ENTITY, userId, entity);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return (T) objectMapper.readValue(response.getContentAsString(),
				entity.getClass());
	}

	/**
	 * Get an entity using an id
	 */
	public static <T extends Entity> T getEntity(HttpServlet dispatchServlet,
			Class<? extends T> clazz, String id, Long userId) throws Exception {
		return ServletTestHelper.getEntity(dispatchServlet, clazz, id, userId,
				null);
	}

	/**
	 * Get an entity using an id
	 */
	public static <T extends Entity> T getEntity(HttpServlet dispatchServlet,
			Class<? extends T> clazz, String id, Long userId,
			Map<String, String> extraParams) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id, userId, null);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (T) objectMapper.readValue(response.getContentAsString(), clazz);
	}

	/**
	 * Get an entity using an id
	 */
	public static <T extends Versionable> T getEntityForVersion(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long versionNumber, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.VERSION
						+ "/" + versionNumber, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (T) objectMapper.readValue(response.getContentAsString(), clazz);
	}

	/**
	 * Get the annotations for an entity
	 */
	public static <T extends Entity> Annotations getEntityAnnotations(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id
						+ UrlHelpers.ANNOTATIONS, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				Annotations.class);
	}

	/**
	 * Get the annotations for an entity
	 */
	public static <T extends Entity> EntityPath getEntityPath(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.PATH,
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (EntityPath) objectMapper.readValue(
				response.getContentAsString(), clazz);
	}

	/**
	 * Get the annotations for a given version
	 */
	public static <T extends Entity> Annotations getEntityAnnotationsForVersion(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long versionNumber, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.VERSION
						+ "/" + versionNumber + UrlHelpers.ANNOTATIONS, userId,
				null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				Annotations.class);
	}

	/**
	 * Update the annotations for an entity
	 */
	public static <T extends Entity> Annotations updateEntityAnnotations(
			HttpServlet dispatchServlet, Class<? extends T> clazz,
			Annotations updatedAnnos, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + updatedAnnos.getId()
						+ UrlHelpers.ANNOTATIONS, userId, updatedAnnos);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				Annotations.class);
	}

	/**
	 * Update an entity
	 */
	public static <T extends Entity> T updateEntity(
			HttpServlet dispatchServlet, T entity, Long userId)
			throws Exception {
		return ServletTestHelper.updateEntity(dispatchServlet, entity, userId,
				null);
	}

	/**
	 * Update an entity
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Entity> T updateEntity(
			HttpServlet dispatchServlet, T entity, Long userId,
			Map<String, String> extraParams) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + entity.getId(), userId,
				entity);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (T) objectMapper.readValue(response.getContentAsString(),
				entity.getClass());
	}

	/**
	 * Update an entity
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Versionable> T createNewVersion(
			HttpServlet dispatchServlet, T entity, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + entity.getId()
						+ UrlHelpers.VERSION, userId, entity);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (T) objectMapper.readValue(response.getContentAsString(),
				entity.getClass());
	}

	/**
	 * Get all objects of type
	 */
	public static PaginatedResults<VersionInfo> getAllVersionsOfEntity(
			HttpServlet dispatchServlet, String entityId, Integer offset,
			Integer limit, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + entityId
						+ UrlHelpers.VERSION, userId, null);
		if (offset != null) {
			request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM,
					offset.toString());
		}
		if (limit != null) {
			request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM,
					limit.toString());
		}

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				VersionInfo.class);
	}

	/**
	 * Delete an entity
	 */
	public static <T extends Entity> void deleteEntity(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long userId) throws Exception {
		ServletTestHelper
				.deleteEntity(dispatchServlet, clazz, id, userId, null);
	}

	/**
	 * Delete an entity
	 */
	public static <T extends Entity> void deleteEntity(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long userId, Map<String, String> extraParams) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.ENTITY + "/" + id, userId, null);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.NO_CONTENT);
	}

	/**
	 * Delete a specific version of an entity
	 */
	public static <T extends Entity> void deleteEntityVersion(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long versionNumber, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.ENTITY + "/" + id
						+ UrlHelpers.VERSION + "/" + versionNumber, userId,
				null);

		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.NO_CONTENT);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Entity> QueryResults<Map<String, Object>> query(
			HttpServlet dispatchServlet, String query, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.QUERY, userId, null);
		request.setParameter(ServiceConstants.QUERY_PARAM, query);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				QueryResults.class);
	}

	/**
	 * Create the Access Control List (ACL) for an entity
	 */
	public static <T extends Entity> AccessControlList createEntityACL(
			HttpServlet dispatchServlet, String id,
			AccessControlList entityACL, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.ENTITY + "/" + id + UrlHelpers.ACL,
				userId, entityACL);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return objectMapper.readValue(response.getContentAsString(),
				AccessControlList.class);
	}

	/**
	 * Get the Access Control List (ACL) for an entity
	 */
	public static <T extends Entity> AccessControlList getEntityACL(
			HttpServlet dispatchServlet, String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.ACL,
				userId, null);

		MockHttpServletResponse response;
		try {
			response = ServletTestHelperUtils.dispatchRequest(dispatchServlet,
					request, HttpStatus.OK);
		} catch (NotFoundException e) {
			throw new ACLInheritanceException(e.getMessage());
		}

		return objectMapper.readValue(response.getContentAsString(),
				AccessControlList.class);
	}

	/**
	 * Update an entity ACL
	 */
	public static <T extends Entity> AccessControlList updateEntityAcl(
			HttpServlet dispatchServlet, String id,
			AccessControlList entityACL, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + id + UrlHelpers.ACL,
				userId, entityACL);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				AccessControlList.class);
	}

	/**
	 * Delete an entity ACL
	 */
	public static <T extends Entity> void deleteEntityACL(
			HttpServlet dispatchServlet, String resourceId, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.ENTITY + "/" + resourceId
						+ UrlHelpers.ACL, userId, null);

		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.NO_CONTENT);
	}

	/**
	 * Get the principals
	 */
	public static PaginatedResults<UserProfile> getUsers(
			HttpServlet dispatchServlet, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.USER, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				UserProfile.class);
	}

	/**
	 * Get the principals
	 */
	public static PaginatedResults<UserGroup> getGroups(
			HttpServlet dispatchServlet, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.USERGROUP, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				UserGroup.class);
	}

	/**
	 * Calls 'hasAccess'
	 */
	public static <T extends Entity> BooleanResult hasAccess(
			HttpServlet dispatchServlet, Class<? extends T> clazz, String id,
			Long userId, String accessType) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.ACCESS,
				userId, null);
		request.setParameter(UrlHelpers.ACCESS_TYPE_PARAM, accessType);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (BooleanResult) objectMapper.readValue(
				response.getContentAsString(), BooleanResult.class);
	}

	/**
	 * Get the status of a backup/restore daemon
	 */
	public static BackupRestoreStatus getDaemonStatus(
			HttpServlet dispatchServlet, Long userId, String id)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.DAEMON + "/" + id, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (BackupRestoreStatus) objectMapper.readValue(
				response.getContentAsString(), BackupRestoreStatus.class);
	}

	/**
	 * Get the status of a backup/restore daemon
	 */
	public static StackStatus getStackStatus(HttpServlet dispatchServlet)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.STACK_STATUS, null, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (StackStatus) objectMapper.readValue(
				response.getContentAsString(), StackStatus.class);
	}

	/**
	 * Get the status of a backup/restore daemon
	 */
	public static StackStatus updateStackStatus(HttpServlet dispatchServlet,
			Long userId, StackStatus toUpdate) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.STACK_STATUS, userId, toUpdate);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (StackStatus) objectMapper.readValue(
				response.getContentAsString(), StackStatus.class);
	}

	public static void terminateDaemon(HttpServlet dispatchServlet,
			Long userId, String id) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.DAEMON + "/" + id, userId, null);

		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.NO_CONTENT);
	}

	public static EntityHeader getEntityType(HttpServlet dispatchServlet,
			String id, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.TYPE,
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (EntityHeader) objectMapper.readValue(
				response.getContentAsString(), EntityHeader.class);
	}

	public static PaginatedResults<EntityHeader> getEntityReferences(
			HttpServlet dispatchServlet, String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id
						+ UrlHelpers.REFERENCED_BY, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				EntityHeader.class);
	}

	public static PaginatedResults<EntityHeader> getEntityReferences(
			HttpServlet dispatchServlet, String id, Long versionNumber,
			Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id + UrlHelpers.VERSION
						+ "/" + versionNumber + UrlHelpers.REFERENCED_BY,
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				EntityHeader.class);
	}

	/**
	 * Get the PermissionInfo for a given entity
	 */
	public static <T extends Entity> EntityHeader getEntityBenefactor(
			HttpServlet dispatchServlet, String id, Class<? extends T> clazz,
			Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id
						+ UrlHelpers.BENEFACTOR, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return (EntityHeader) objectMapper.readValue(
				response.getContentAsString(), EntityHeader.class);
	}

	/**
	 * Get search results
	 */
	public static SearchResults getSearchResults(HttpServlet dispatchServlet,
			Long userId, Map<String, String> extraParams) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/search", userId, null);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				SearchResults.class);
	}

	public static MessageToUser sendMessage(Long userId, MessageToUser message)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.MESSAGE, userId, message);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return ServletTestHelperUtils.readResponse(response,
				MessageToUser.class);
	}

	private static Map<String, String> fillInMessagingParams(
			List<MessageStatusType> inboxFilter, MessageSortBy orderBy,
			Boolean descending, long limit, long offset) {
		HashMap<String, String> params = new HashMap<String, String>();
		if (inboxFilter != null) {
			params.put(UrlHelpers.MESSAGE_INBOX_FILTER_PARAM,
					StringUtils.join(inboxFilter.toArray(), ','));
		}
		if (orderBy != null) {
			params.put(UrlHelpers.MESSAGE_ORDER_BY_PARAM, orderBy.name());
		}
		if (descending != null) {
			params.put(UrlHelpers.MESSAGE_DESCENDING_PARAM, "" + descending);
		}
		params.put(ServiceConstants.PAGINATION_LIMIT_PARAM, "" + limit);
		params.put(ServiceConstants.PAGINATION_OFFSET_PARAM, "" + offset);
		return params;
	}

	public static PaginatedResults<MessageBundle> getInbox(Long userId,
			List<MessageStatusType> inboxFilter, MessageSortBy orderBy,
			Boolean descending, long limit, long offset) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.MESSAGE_INBOX, userId, null);
		ServletTestHelperUtils.addExtraParams(
				request,
				fillInMessagingParams(inboxFilter, orderBy, descending, limit,
						offset));

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				MessageBundle.class);
	}

	public static PaginatedResults<MessageToUser> getOutbox(Long userId,
			MessageSortBy orderBy, Boolean descending, long limit, long offset)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.MESSAGE_OUTBOX, userId, null);
		ServletTestHelperUtils
				.addExtraParams(
						request,
						fillInMessagingParams(null, orderBy, descending, limit,
								offset));

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				MessageToUser.class);
	}

	public static MessageToUser getMessage(Long userId, String messageId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.MESSAGE + "/" + messageId, userId,
				null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponse(response,
				MessageToUser.class);
	}

	public static MessageToUser forwardMessage(Long userId, String messageId,
			MessageRecipientSet recipients) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.MESSAGE + "/" + messageId
						+ UrlHelpers.FORWARD, userId, recipients);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return ServletTestHelperUtils.readResponse(response,
				MessageToUser.class);
	}

	public static PaginatedResults<MessageToUser> getConversation(Long userId,
			String associatedMessageId, MessageSortBy orderBy,
			Boolean descending, long limit, long offset) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.MESSAGE + "/" + associatedMessageId
						+ UrlHelpers.CONVERSATION, userId, null);
		ServletTestHelperUtils
				.addExtraParams(
						request,
						fillInMessagingParams(null, orderBy, descending, limit,
								offset));

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				MessageToUser.class);
	}

	public static void updateMessageStatus(Long userId, MessageStatus status)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.MESSAGE_STATUS, userId, status);

		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.OK);
	}

	public static ConceptResponsePage getConceptsForParent(String parentId,
			String pefix, int limit, int offset) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.CONCEPT + "/" + parentId
						+ UrlHelpers.CHILDERN_TRANSITIVE, null, null);
		if (pefix != null) {
			request.setParameter(UrlHelpers.PREFIX_FILTER, pefix);
		}
		request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, ""
				+ limit);
		request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, ""
				+ offset);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				ConceptResponsePage.class);
	}

	/**
	 * Get a single concept from its id
	 */
	public static Concept getConcept(String id) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.CONCEPT + "/" + id, null, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				Concept.class);
	}

	/**
	 * Get a single concept from its id
	 */
	public static String getConceptAsJSONP(String id, String callbackName)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.CONCEPT + "/" + id, null, null);
		// Add the header that indicates we want JSONP
		request.addParameter(UrlHelpers.REQUEST_CALLBACK_JSONP, callbackName);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return response.getContentAsString();
	}

	public static UserGroupHeaderResponsePage getUserGroupHeadersByPrefix(
			String pefix, int limit, int offest) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.USER_GROUP_HEADERS, null, null);
		if (pefix != null) {
			request.setParameter(UrlHelpers.PREFIX_FILTER, pefix);
		}
		request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, ""
				+ limit);
		request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, ""
				+ offest);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				UserGroupHeaderResponsePage.class);
	}

	public static String getUserGroupHeadersAsJSONP(String pefix, int limit,
			int offest, String callbackName) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.CONCEPT + "/", null, null);
		// Add the header that indicates we want JSONP
		request.addParameter(UrlHelpers.REQUEST_CALLBACK_JSONP, callbackName);
		if (pefix != null) {
			request.setParameter(UrlHelpers.PREFIX_FILTER, pefix);
		}
		request.setParameter(ServiceConstants.PAGINATION_LIMIT_PARAM, ""
				+ limit);
		request.setParameter(ServiceConstants.PAGINATION_OFFSET_PARAM, ""
				+ offest);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return response.getContentAsString();
	}

	public static UserEntityPermissions getUserEntityPermissions(
			HttpServlet dispatchServlet, String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + id
						+ UrlHelpers.PERMISSIONS, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				UserEntityPermissions.class);
	}

	/**
	 * Create an attachment token
	 */
	public static S3AttachmentToken createS3AttachmentToken(Long userId,
			ServiceConstants.AttachmentType attachentType, String id,
			S3AttachmentToken token) throws Exception {
		Assert.assertNotNull(id);
		Assert.assertNotNull(token);

		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.getAttachmentTypeURL(attachentType)
						+ "/" + id + UrlHelpers.ATTACHMENT_S3_TOKEN, userId,
				token);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), S3AttachmentToken.class);
	}

	/**
	 * Get a pre-signed URL for a an attachment
	 */
	public PresignedUrl getAttachmentUrl(Long userIdId, String entityId,
			String tokenId) throws Exception {
		return getAttachmentUrl(userId, AttachmentType.ENTITY, entityId,
				tokenId);
	}

	/**
	 * Get a pre-signed URL for a user profile attachment
	 */
	public PresignedUrl getUserProfileAttachmentUrl(Long userIdId,
			String targetProfileId, String tokenId) throws Exception {
		return getAttachmentUrl(userId, AttachmentType.USER_PROFILE,
				targetProfileId, tokenId);
	}

	/**
	 * Get a pre-signed URL for a an attachment
	 */
	public PresignedUrl getAttachmentUrl(Long userId, AttachmentType type,
			String id, String tokenId) throws Exception {
		Assert.assertNotNull(id);
		Assert.assertNotNull(tokenId);

		PresignedUrl url = new PresignedUrl();
		url.setTokenID(tokenId);
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.getAttachmentTypeURL(type) + "/" + id
						+ UrlHelpers.ATTACHMENT_URL, userId, url);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return EntityFactory.createEntityFromJSONString(
				response.getContentAsString(), PresignedUrl.class);
	}

	public String checkAmznHealth() throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.HEAD, UrlHelpers.HEALTHCHECK, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return response.getContentAsString();
	}

	@SuppressWarnings("unchecked")
	public static <T extends AccessRequirement> T createAccessRequirement(
			HttpServlet dispatchServlet, T accessRequirement, Long userId,
			Map<String, String> extraParams) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.ACCESS_REQUIREMENT, userId,
				accessRequirement);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return (T) objectMapper.readValue(response.getContentAsString(),
				accessRequirement.getClass());
	}

	public static PaginatedResults<AccessRequirement> getEntityAccessRequirements(
			HttpServlet dispatchServlet, String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/entity/" + id + UrlHelpers.ACCESS_REQUIREMENT,
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponseVariablePaginatedResults(
				response, AccessRequirement.class);
	}

	public static PaginatedResults<AccessRequirement> getEvaluationAccessRequirements(
			HttpServlet dispatchServlet, String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/evaluation/" + id
						+ UrlHelpers.ACCESS_REQUIREMENT, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponseVariablePaginatedResults(
				response, AccessRequirement.class);
	}

	public static PaginatedResults<AccessRequirement> getUnmetEntityAccessRequirements(
			HttpServlet dispatchServlet, String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils
				.initRequest(HTTPMODE.GET, "/entity/" + id
						+ "/accessRequirementUnfulfilled", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponseVariablePaginatedResults(
				response, AccessRequirement.class);
	}

	public static PaginatedResults<AccessRequirement> getUnmetEvaluationAccessRequirements(
			HttpServlet dispatchServlet, String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/evaluation/" + id
						+ "/accessRequirementUnfulfilled", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponseVariablePaginatedResults(
				response, AccessRequirement.class);
	}

	public static void deleteAccessRequirements(HttpServlet dispatchServlet,
			String id, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.ACCESS_REQUIREMENT + "/" + id,
				userId, null);

		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.OK);
	}

	@SuppressWarnings("unchecked")
	public static <T extends AccessApproval> T createAccessApproval(
			HttpServlet dispatchServlet, T accessApproval, Long userId,
			Map<String, String> extraParams) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.ACCESS_APPROVAL, userId,
				accessApproval);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return (T) objectMapper.readValue(response.getContentAsString(),
				accessApproval.getClass());
	}

	public static PaginatedResults<AccessApproval> getEntityAccessApprovals(
			HttpServlet dispatchServlet, String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/entity/" + id + UrlHelpers.ACCESS_APPROVAL,
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponseVariablePaginatedResults(
				response, AccessApproval.class);
	}

	public static PaginatedResults<AccessApproval> getEvaluationAccessApprovals(
			HttpServlet dispatchServlet, String id, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/evaluation/" + id + UrlHelpers.ACCESS_APPROVAL,
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponseVariablePaginatedResults(
				response, AccessApproval.class);
	}

	public static void deleteAccessApprovals(HttpServlet dispatchServlet,
			String id, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.ACCESS_APPROVAL + "/" + id, userId,
				null);

		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.OK);
	}

	public SynapseVersionInfo getVersionInfo() throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.VERSIONINFO, null, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				SynapseVersionInfo.class);
	}

	public static Activity createActivity(HttpServlet dispatchServlet,
			Activity activity, Long userId, Map<String, String> extraParams)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.ACTIVITY, userId, activity);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return objectMapper.readValue(response.getContentAsString(),
				Activity.class);
	}

	public static Activity getActivity(HttpServlet dispatchServlet,
			String activityId, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ACTIVITY + "/" + activityId, userId,
				null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return new Activity(ServletTestHelperUtils.readResponseJSON(response));
	}

	public static Activity updateActivity(HttpServlet dispatchServlet,
			Activity activity, Long userId, Map<String, String> extraParams)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ACTIVITY + "/" + activity.getId(),
				userId, activity);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return objectMapper.readValue(response.getContentAsString(),
				Activity.class);
	}

	public static void deleteActivity(HttpServlet dispatchServlet,
			String activityId, Long userId, Map<String, String> extraParams)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.ACTIVITY + "/" + activityId,
				userId, null);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.NO_CONTENT);
	}

	public static PaginatedResults<Reference> getEntitiesGeneratedBy(
			HttpServlet dispatchServlet, Activity activity, Long userId,
			Map<String, String> extraParams) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ACTIVITY + "/" + activity.getId()
						+ UrlHelpers.GENERATED, userId, activity);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				Reference.class);
	}

	public static EntityHeader addFavorite(HttpServlet dispatchServlet,
			String entityId, Long userId, Map<String, String> extraParams)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.FAVORITE + "/" + entityId, userId,
				null);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return objectMapper.readValue(response.getContentAsString(),
				EntityHeader.class);
	}

	public static void removeFavorite(HttpServlet dispatchServlet,
			String entityId, Long userId, Map<String, String> extraParams)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.FAVORITE + "/" + entityId, userId,
				null);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.NO_CONTENT);
	}

	public static PaginatedResults<EntityHeader> getFavorites(
			HttpServlet dispatchServlet, Long userId,
			Map<String, String> extraParams) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.FAVORITE, userId, null);
		ServletTestHelperUtils.addExtraParams(request, extraParams);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				EntityHeader.class);
	}

	/**
	 * Create a ColumnModel
	 * 
	 * @param instance
	 * @param cm
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static ColumnModel createColumnModel(DispatcherServlet instance,
			ColumnModel cm, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.COLUMN, userId, cm);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(instance, request, HttpStatus.CREATED);
		return ServletTestHelperUtils.readResponse(response, ColumnModel.class);
	}

	/**
	 * Get a ColumnModel from its ID
	 * 
	 * @param instance
	 * @param cm
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static ColumnModel getColumnModel(DispatcherServlet instance,
			String columnId, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.COLUMN + "/" + columnId, userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(instance, request, HttpStatus.OK);
		return ServletTestHelperUtils.readResponse(response, ColumnModel.class);
	}

	/**
	 * Get the list of ColumnModles for a given TableEntity ID.
	 * 
	 * @param instance
	 * @param entityId
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static List<ColumnModel> getColumnModelsForTableEntity(
			DispatcherServlet instance, String entityId, Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + entityId
						+ UrlHelpers.COLUMN, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(instance, request, HttpStatus.OK);
		PaginatedColumnModels pcm = ServletTestHelperUtils.readResponse(
				response, PaginatedColumnModels.class);
		return pcm.getResults();
	}

	/**
	 * Append some rows to a table.
	 * 
	 * @param instance
	 * @param rows
	 * @param user
	 * @return
	 * @throws Exception
	 */
	public static RowReferenceSet appendTableRows(DispatcherServlet instance,
			RowSet rows, Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.ENTITY + "/" + rows.getTableId()
						+ UrlHelpers.TABLE, userId, rows);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(instance, request, HttpStatus.CREATED);
		return ServletTestHelperUtils.readResponse(response,
				RowReferenceSet.class);
	}

	/**
	 * List all of the ColumnModels in Synapse.
	 * 
	 * @param instance
	 * @param user
	 * @param prefix
	 * @param limit
	 * @param offset
	 * @return
	 * @throws Exception
	 */
	public static PaginatedColumnModels listColumnModels(
			DispatcherServlet instance, Long userId, String prefix, Long limit,
			Long offset) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.COLUMN, userId, null);
		if (prefix != null) {
			request.addParameter("prefix", prefix);
		}
		if (limit != null) {
			request.addParameter("limit", limit.toString());
		}
		if (offset != null) {
			request.addParameter("offset", offset.toString());
		}
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(instance, request, HttpStatus.OK);
		PaginatedColumnModels pcm = ServletTestHelperUtils.readResponse(
				response, PaginatedColumnModels.class);
		return pcm;
	}

	public static Team createTeam(HttpServlet dispatchServlet, Long userId,
			Team team) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.TEAM, userId, team);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return objectMapper
				.readValue(response.getContentAsString(), Team.class);
	}

	public static void deleteTeam(HttpServlet dispatchServlet, Long userId,
			Team team) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, UrlHelpers.TEAM + "/" + team.getId(), userId,
				null);
		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.NO_CONTENT);
	}

	public static MembershipInvtnSubmission createMembershipInvitation(
			HttpServlet dispatchServlet, Long userId,
			MembershipInvtnSubmission mis) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, UrlHelpers.MEMBERSHIP_INVITATION, userId, mis);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return objectMapper.readValue(response.getContentAsString(),
				MembershipInvtnSubmission.class);
	}

	public static MembershipInvtnSubmission getMembershipInvitation(
			HttpServlet dispatchServlet, Long userId, String misId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.MEMBERSHIP_INVITATION + "/" + misId,
				userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return new MembershipInvtnSubmission(
				ServletTestHelperUtils.readResponseJSON(response));
	}

	public static PaginatedResults<MembershipInvtnSubmission> getMembershipInvitationSubmissions(
			HttpServlet dispatchServlet, Long userId, String teamId)
			throws Exception {

		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.TEAM + "/" + teamId
						+ "/openInvitation", userId, null);

		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				MembershipInvtnSubmission.class);
	}

	public static void deleteMembershipInvitation(HttpServlet dispatchServlet,
			Long userId, MembershipInvtnSubmission mis) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE,
				UrlHelpers.MEMBERSHIP_INVITATION + "/" + mis.getId(), userId,
				null);
		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.NO_CONTENT);
	}

	public static Doi putDoiWithoutVersion(Long userId, String entityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + entityId
						+ UrlHelpers.DOI, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.ACCEPTED);

		return ServletTestHelperUtils.readResponse(response, Doi.class);
	}

	public static Doi putDoiWithVersion(Long userId, String entityId,
			int versionNumber) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ENTITY + "/" + entityId
						+ UrlHelpers.VERSION + "/" + versionNumber
						+ UrlHelpers.DOI, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.ACCEPTED);

		return ServletTestHelperUtils.readResponse(response, Doi.class);
	}

	public static Doi getDoiWithoutVersion(Long userId, String entityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + entityId
						+ UrlHelpers.DOI, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponse(response, Doi.class);
	}

	public static Doi getDoiWithVersion(Long userId, String entityId,
			int versionNumber) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + entityId
						+ UrlHelpers.VERSION + "/" + versionNumber
						+ UrlHelpers.DOI, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponse(response, Doi.class);
	}

	/**
	 * Get search results
	 */
	public static SearchResults getSearchResults(Long userId, SearchQuery query)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, "/search", userId, query);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.CREATED);

		return ServletTestHelperUtils.readResponse(response,
				SearchResults.class);
	}

	public static EntityIdList getAncestors(Long userId, String entityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils
				.initRequest(HTTPMODE.GET, UrlHelpers.ENTITY + "/" + entityId
						+ "/ancestors", userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils
				.readResponse(response, EntityIdList.class);
	}

	public static EntityId getParent(Long userId, String entityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + entityId + "/parent",
				userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponse(response, EntityId.class);
	}

	public static EntityIdList getDescendants(Long userId, String entityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + entityId
						+ "/descendants", userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils
				.readResponse(response, EntityIdList.class);
	}

	public static EntityIdList getDescendantsWithGeneration(Long userId,
			String entityId, int generation) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + entityId
						+ "/descendants/" + generation, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils
				.readResponse(response, EntityIdList.class);
	}

	public static EntityIdList getChildren(Long userId, String entityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ENTITY + "/" + entityId + "/children",
				userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils
				.readResponse(response, EntityIdList.class);
	}

	public static StorageUsageSummaryList getStorageUsageGrandTotal(Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.STORAGE_SUMMARY, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponse(response,
				StorageUsageSummaryList.class);
	}

	public static StorageUsageSummaryList getStorageUsageAggregatedTotal(
			Long userId, String aggregation) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.STORAGE_SUMMARY + "/" + userId,
				userId, null);
		request.setParameter(ServiceConstants.AGGREGATION_DIMENSION,
				aggregation);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponse(response,
				StorageUsageSummaryList.class);
	}

	public static PaginatedResults<StorageUsage> getStorageUsageItemized(
			Long userId, String aggregation) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.STORAGE_DETAILS, userId, null);
		request.setParameter(ServiceConstants.AGGREGATION_DIMENSION,
				aggregation);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				StorageUsage.class);
	}

	public static PaginatedResults<TrashedEntity> getTrashCan(Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.TRASHCAN_VIEW, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				TrashedEntity.class);
	}

	public static void trashEntity(Long userId, String entityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.TRASHCAN + "/trash/" + entityId,
				userId, null);
		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.OK);
	}

	public static void purgeEntityInTrash(Long userId, String entityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.TRASHCAN_PURGE + "/" + entityId,
				userId, null);
		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.OK);
	}

	public static void purgeTrash(Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.TRASHCAN_PURGE, userId, null);
		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.OK);
	}

	public static void restoreEntity(Long userId, String entityId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.TRASHCAN + "/restore/" + entityId,
				userId, null);
		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.OK);
	}

	public static void adminPurgeTrash(Long userId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.PUT, UrlHelpers.ADMIN_TRASHCAN_PURGE, userId, null);
		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.OK);
	}

	public static PaginatedResults<TrashedEntity> adminGetTrashCan(Long userId)
			throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, UrlHelpers.ADMIN_TRASHCAN_VIEW, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponsePaginatedResults(response,
				TrashedEntity.class);
	}
	
	public static S3FileHandle getFileHandle(Long userId, String fileHandleId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.GET, "/fileHandle/" + fileHandleId, userId, null);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponse(response, S3FileHandle.class);
	}
	
	public static void deleteFilePreview(Long userId, String fileHandleId) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.DELETE, "/fileHandle/" + fileHandleId + "/filepreview", userId, null);
		ServletTestHelperUtils.dispatchRequest(dispatchServlet, request,
				HttpStatus.OK);
	}
	
	public static ExternalFileHandle createExternalFileHandle(Long userId, ExternalFileHandle handle) throws Exception {
		MockHttpServletRequest request = ServletTestHelperUtils.initRequest(
				HTTPMODE.POST, "/externalFileHandle", userId, handle);
		MockHttpServletResponse response = ServletTestHelperUtils
				.dispatchRequest(dispatchServlet, request, HttpStatus.OK);

		return ServletTestHelperUtils.readResponse(response, ExternalFileHandle.class);
	}
}
