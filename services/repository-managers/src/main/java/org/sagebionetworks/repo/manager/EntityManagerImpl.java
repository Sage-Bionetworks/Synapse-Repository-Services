package org.sagebionetworks.repo.manager;

import java.util.List;
import java.util.Set;

import org.json.JSONObject;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.entity.EntityAuthorizationManager;
import org.sagebionetworks.repo.manager.file.FileHandleManager;
import org.sagebionetworks.repo.manager.file.MultipartUtils;
import org.sagebionetworks.repo.manager.schema.AnnotationsTranslator;
import org.sagebionetworks.repo.manager.schema.EntityJsonSubject;
import org.sagebionetworks.repo.manager.schema.JsonSchemaManager;
import org.sagebionetworks.repo.manager.schema.JsonSubject;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DataType;
import org.sagebionetworks.repo.model.DataTypeResponse;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityChildrenRequest;
import org.sagebionetworks.repo.model.EntityChildrenResponse;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityId;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NextPageToken;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.annotation.v2.Annotations;
import org.sagebionetworks.repo.model.dbo.dao.NodeUtils;
import org.sagebionetworks.repo.model.dbo.schema.EntitySchemaValidationResultDao;
import org.sagebionetworks.repo.model.entity.BindSchemaToEntityRequest;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.entity.FileHandleUpdateRequest;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.jdo.KeyFactory;
import org.sagebionetworks.repo.model.message.ChangeType;
import org.sagebionetworks.repo.model.message.TransactionalMessenger;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.schema.BoundObjectType;
import org.sagebionetworks.repo.model.schema.JsonSchemaObjectBinding;
import org.sagebionetworks.repo.model.schema.ListValidationResultsRequest;
import org.sagebionetworks.repo.model.schema.ListValidationResultsResponse;
import org.sagebionetworks.repo.model.schema.ValidationResults;
import org.sagebionetworks.repo.model.schema.ValidationSummaryStatistics;
import org.sagebionetworks.repo.model.table.Table;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
/**
 *
 */
public class EntityManagerImpl implements EntityManager {

	public static final int MAX_NUMBER_OF_REVISIONS = 15000;
	public static final Direction DEFAULT_SORT_DIRECTION = Direction.ASC;
	public static final SortBy DEFAULT_SORT_BY = SortBy.NAME;
	public static final String ROOT_ID = StackConfigurationSingleton.singleton().getRootFolderEntityId();
	public static final List<EntityType> PROJECT_ONLY = Lists.newArrayList(EntityType.project);

	@Autowired
	private NodeManager nodeManager;
	@Autowired
	private EntityAclManager entityAclManager;
	@Autowired
	private EntityAuthorizationManager entityAuthorizationManager;
	@Autowired
	private FileHandleManager fileHandleManager;
	@Autowired
	private ObjectTypeManager objectTypeManager;
	@Autowired
	private JsonSchemaManager jsonSchemaManager;
	@Autowired
	private AnnotationsTranslator annotationsTranslator;
	@Autowired
	private EntitySchemaValidationResultDao entitySchemaValidationResultDao;
	@Autowired
	private TransactionalMessenger transactionalMessenger;

	boolean allowCreationOfOldEntities = true;

	/**
	 * Injected via spring.
	 * 
	 * @param allowOldEntityTypes
	 */
	public void setAllowCreationOfOldEntities(boolean allowCreationOfOldEntities) {
		this.allowCreationOfOldEntities = allowCreationOfOldEntities;
	}

	@WriteTransaction
	@Override
	public <T extends Entity> String createEntity(UserInfo userInfo, T newEntity, String activityId)
			throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		// First create a node the represent the entity
		Node node = NodeTranslationUtils.createFromEntity(newEntity);
		// Set the type for this object
		node.setNodeType(EntityTypeUtils.getEntityTypeForClass(newEntity.getClass()));
		node.setActivityId(activityId);
		org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations = new org.sagebionetworks.repo.model.Annotations();
		// Now add all of the annotations and references from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(newEntity, entityPropertyAnnotations);
		// We are ready to create this node
		node = nodeManager.createNewNode(node, entityPropertyAnnotations, userInfo);
		// Return the id of the newly created entity
		return node.getId();
	}

	@Override
	public <T extends Entity> T getEntity(UserInfo userInfo, String entityId, Class<? extends T> entityClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");
		entityAuthorizationManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		return getEntity(entityId, entityClass);
	}

	@Override
	public Entity getEntity(UserInfo user, String entityId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		Class<? extends Entity> entityClass = null;
		return getEntity(user, entityId, entityClass);
	}
	
	/**
	 * Get an entity without an authorization check.
	 * @param <T>
	 * @param entityId
	 * @param entityClass
	 * @return
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	public <T extends Entity> T getEntity(String entityId, Class<? extends T> entityClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		ValidateArgument.required(entityId, "entityId");
		org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations = nodeManager
				.getEntityPropertyAnnotations(entityId);
		Node node = nodeManager.getNode(entityId);
		if(entityClass == null) {
			entityClass = (Class<? extends T>) EntityTypeUtils.getClassForType(node.getNodeType());
		}
		// Does the node type match the requested type?
		validateType(EntityTypeUtils.getEntityTypeForClass(entityClass), node.getNodeType(), entityId);
		return populateEntityWithNodeAndAnnotations(entityClass, entityPropertyAnnotations, node);
	}

	
	/**
	 * Validate that the requested entity type matches the actual entity type. See
	 * http://sagebionetworks.jira.com/browse/PLFM-431.
	 * 
	 * @param <T>
	 * @param requestedType
	 * @param acutalType
	 * @param id
	 */
	private <T extends Entity> void validateType(EntityType requestedType, EntityType acutalType, String id) {
		if (acutalType != requestedType) {
			throw new IllegalArgumentException("The Entity: syn" + id + " has an entityType="
					+ EntityTypeUtils.getEntityTypeClassName(acutalType) + " and cannot be changed to entityType="
					+ EntityTypeUtils.getEntityTypeClassName(requestedType));
		}
	}

	/**
	 * @param <T>
	 * @param userInfo
	 * @param entityId
	 * @param versionNumber
	 * @param entityClass
	 * @return the entity version
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@Override
	public <T extends Entity> T getEntityForVersion(UserInfo userInfo, String entityId, Long versionNumber,
			Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the annotations for this entity
		org.sagebionetworks.repo.model.Annotations annos = nodeManager.getEntityPropertyForVersion(userInfo, entityId,
				versionNumber);
		// Fetch the current node from the server
		Node node = nodeManager.getNodeForVersionNumber(userInfo, entityId, versionNumber);
		return populateEntityWithNodeAndAnnotations(entityClass, annos, node);
	}

	/**
	 * Create and populate an instance of an entity using both a node and
	 * annotations.
	 * 
	 * @param <T>
	 * @param entityClass
	 * @param userAnnotations
	 * @param node
	 * @return
	 */
	private static <T extends Entity> T populateEntityWithNodeAndAnnotations(Class<? extends T> entityClass,
			org.sagebionetworks.repo.model.Annotations entityProperties, Node node)
			throws DatastoreException, NotFoundException {
		// Return the new object from the dataEntity
		T newEntity = createNewEntity(entityClass);
		// Populate the entity using the annotations and references
		NodeTranslationUtils.updateObjectFromNodeSecondaryFields(newEntity, entityProperties);
		// Populate the entity using the node
		NodeTranslationUtils.updateObjectFromNode(newEntity, node);
		newEntity.setCreatedBy(node.getCreatedByPrincipalId().toString());
		newEntity.setModifiedBy(node.getModifiedByPrincipalId().toString());
		return newEntity;
	}

	@Override
	public List<EntityHeader> getEntityHeaderByMd5(UserInfo userInfo, String md5)
			throws NotFoundException, DatastoreException {
		return nodeManager.getNodeHeaderByMd5(userInfo, md5);
	}

	/**
	 * Will convert the any exceptions to runtime.
	 * 
	 * @param <T>
	 * @param entityClass
	 * @return
	 */
	private static <T> T createNewEntity(Class<? extends T> entityClass) {
		T newEntity;
		try {
			newEntity = entityClass.newInstance();
		} catch (InstantiationException e) {
			throw new IllegalArgumentException("Class must have a no-argument constructor: " + Entity.class.getName());
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(
					"Class must have a public no-argument constructor: " + Entity.class.getName());
		}
		return newEntity;
	}

	@WriteTransaction
	@Override
	public void deleteEntity(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity ID cannot be null");
		nodeManager.delete(userInfo, entityId);
	}

	@WriteTransaction
	@Override
	public void deleteEntityVersion(UserInfo userInfo, String id, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		nodeManager.deleteVersion(userInfo, id, versionNumber);
	}

	@Override
	public Annotations getAnnotations(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity ID cannot be null");
		// This is a simple pass through
		return nodeManager.getUserAnnotations(userInfo, entityId);
	}

	@Override
	public Annotations getAnnotationsForVersion(UserInfo userInfo, String id, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get all of the annotations.
		return nodeManager.getUserAnnotationsForVersion(userInfo, id, versionNumber);
	}

	@WriteTransaction
	@Override
	public void updateAnnotations(UserInfo userInfo, String entityId, Annotations updated)
			throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {
		if (updated == null)
			throw new IllegalArgumentException("Annoations cannot be null");
		// The user has updated the additional annotations.
		nodeManager.updateUserAnnotations(userInfo, entityId, updated);
	}

	@WriteTransaction
	@Override
	public <T extends Entity> boolean updateEntity(UserInfo userInfo, T updated, boolean newVersion, String activityId)
			throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException,
			InvalidModelException {

		Node node = nodeManager.getNode(userInfo, updated.getId());
		// Now get the annotations for this node
		org.sagebionetworks.repo.model.Annotations entityPropertyAnnotations = nodeManager
				.getEntityPropertyAnnotations(userInfo, updated.getId());

		// Auto-version FileEntity when the id of the file handle is changed (and the MD5 is different) See PLFM-1744 and PLFM-6429
		if (!newVersion && (updated instanceof FileEntity)) {
			FileEntity updatedFile = (FileEntity) updated;
			String currentFileHandlId = node.getFileHandleId();
			String updatedFileHandleId = updatedFile.getDataFileHandleId();
			if (!currentFileHandlId.equals(updatedFileHandleId) && !fileHandleManager.isMatchingMD5(currentFileHandlId, updatedFileHandleId)) {
				newVersion = true;
				// Since this is an automatic action we reset the version label, by default creating a new revision with a null
				// version label will automatically set it to the revision id (See NodeDAOImpl.createNewVersion).
				updatedFile.setVersionLabel(null);
				updatedFile.setVersionComment(null);
			}
		}

		if (updated instanceof Table) {
			/*
			 * Fix for PLFM-5702. Creating a new version is fundamentally different than
			 * creating a table/view snapshot. We cannot block callers from creating new
			 * versions of tables/views because the Python/R client syn.store() method
			 * automatically sets 'newVersion'=true. Therefore, to prevent users from
			 * breaking their tables/views by explicitly creating new entity versions, we
			 * unconditionally ignore this parameter for table/views.
			 */
			newVersion = false;
		}

		final boolean newVersionFinal = newVersion;

		if (newVersion) {
			long currentRevisionNumber = nodeManager.getCurrentRevisionNumber(updated.getId());
			if (currentRevisionNumber + 1 > MAX_NUMBER_OF_REVISIONS) {
				throw new IllegalArgumentException(
						"Exceeded the maximum number of " + MAX_NUMBER_OF_REVISIONS + " versions for a single Entity");
			}
		}

		// Set activityId if new version or if not changing versions and activityId is
		// defined
		if (newVersionFinal || (!newVersionFinal && activityId != null)) {
			node.setActivityId(activityId);
		}

		updateNodeAndAnnotationsFromEntity(updated, node, entityPropertyAnnotations);
		// Now update both at the same time
		nodeManager.update(userInfo, node, entityPropertyAnnotations, newVersionFinal);
		return newVersionFinal;
	}
	
	@Override
	@WriteTransaction
	public void updateEntityFileHandle(UserInfo userInfo, String entityId, Long versionNumber,
			FileHandleUpdateRequest updateRequest)
			throws NotFoundException, ConflictingUpdateException, UnauthorizedException {
		nodeManager.updateNodeFileHandle(userInfo, entityId, versionNumber, updateRequest);
	}

	/**
	 * Will update both the passed node and annotations using the passed entity
	 * 
	 * @param <T>
	 * @param entity
	 * @param node
	 * @param annos
	 */
	<T extends Entity> void updateNodeAndAnnotationsFromEntity(T entity, Node node,
			org.sagebionetworks.repo.model.Annotations annos) {
		// Update the annotations from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(entity, annos);
		// Update the node from the entity
		NodeTranslationUtils.updateNodeFromObject(entity, node);
		// Set the Annotations Etag
		annos.setEtag(entity.getEtag());
	}

	@Override
	public EntityType getEntityType(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return nodeManager.getNodeType(userInfo, entityId);
	}

	@Override
	public EntityType getEntityTypeForDeletion(String entityId) throws NotFoundException, DatastoreException {
		return nodeManager.getNodeTypeForDeletion(entityId);
	}

	@Override
	public EntityHeader getEntityHeader(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return nodeManager.getNodeHeader(userInfo, entityId);
	}

	@Override
	public List<EntityHeader> getEntityHeader(UserInfo userInfo, List<Reference> references)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return nodeManager.getNodeHeader(userInfo, references);
	}

	@Override
	public List<VersionInfo> getVersionsOfEntity(UserInfo userInfo, String entityId, long offset, long limit)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		// pass through
		return nodeManager.getVersionsOfEntity(userInfo, entityId, offset, limit);
	}

	@Override
	public List<EntityHeader> getEntityPath(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// pass through
		return nodeManager.getNodePath(userInfo, entityId);
	}

	@Override
	public String getEntityPathAsFilePath(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		List<EntityHeader> entityPath = getEntityPath(userInfo, entityId);

		// we skip the root node
		int startIndex = 1;
		if (entityPath.size() == 1) {
			startIndex = 0;
		}
		StringBuilder path = new StringBuilder(256);

		for (int i = startIndex; i < entityPath.size(); i++) {
			if (path.length() > 0) {
				path.append(MultipartUtils.FILE_TOKEN_TEMPLATE_SEPARATOR);
			}
			path.append(entityPath.get(i).getName());
		}
		return path.toString();
	}

	@Override
	public List<EntityHeader> getEntityPathAsAdmin(String entityId) throws NotFoundException, DatastoreException {
		// pass through
		return nodeManager.getNodePathAsAdmin(entityId);
	}

	@Override
	public void validateReadAccess(UserInfo userInfo, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		entityAuthorizationManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
	}

	@Override
	public void validateUpdateAccess(UserInfo userInfo, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		entityAuthorizationManager.hasAccess(userInfo, entityId, ACCESS_TYPE.UPDATE).checkAuthorizationOrElseThrow();
	}

	@Override
	public boolean doesEntityHaveChildren(UserInfo userInfo, String entityId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		validateReadAccess(userInfo, entityId);
		return nodeManager.doesNodeHaveChildren(entityId);
	}

	@Override
	public Activity getActivityForEntity(UserInfo userInfo, String entityId, Long versionNumber)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		return nodeManager.getActivityForNode(userInfo, entityId, versionNumber);
	}

	@WriteTransaction
	@Override
	public Activity setActivityForEntity(UserInfo userInfo, String entityId, String activityId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		validateUpdateAccess(userInfo, entityId);
		nodeManager.setActivityForNode(userInfo, entityId, activityId);
		return nodeManager.getActivityForNode(userInfo, entityId, null);
	}

	@WriteTransaction
	@Override
	public void deleteActivityForEntity(UserInfo userInfo, String entityId)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		validateUpdateAccess(userInfo, entityId);
		nodeManager.deleteActivityLinkToNode(userInfo, entityId);
	}

	@Override
	public String getFileHandleIdForVersion(UserInfo userInfo, String id, Long versionNumber)
			throws UnauthorizedException, NotFoundException {
		// The manager handles this call.
		return nodeManager.getFileHandleIdForVersion(userInfo, id, versionNumber);
	}

	@Override
	public List<Reference> getCurrentRevisionNumbers(List<String> entityIds) {
		return nodeManager.getCurrentRevisionNumbers(entityIds);
	}

	@Override
	public String getEntityIdForAlias(String alias) {
		return nodeManager.getEntityIdForAlias(alias);
	}

	@Override
	public EntityChildrenResponse getChildren(UserInfo user, EntityChildrenRequest request) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(request, "EntityChildrenRequest");
		if (request.getParentId() == null) {
			// Null parentId is used to list projects.
			request.setParentId(ROOT_ID);
			request.setIncludeTypes(PROJECT_ONLY);
		}
		ValidateArgument.required(request.getIncludeTypes(), "EntityChildrenRequest.includeTypes");
		if (request.getIncludeTypes().isEmpty()) {
			throw new IllegalArgumentException("EntityChildrenRequest.includeTypes must include at least one type");
		}
		if (request.getSortBy() == null) {
			request.setSortBy(DEFAULT_SORT_BY);
		}
		if (request.getSortDirection() == null) {
			request.setSortDirection(DEFAULT_SORT_DIRECTION);
		}
		// Find the children of this entity that the caller cannot see.
		Set<Long> childIdsToExclude = authorizedListChildren(user, request.getParentId());
		NextPageToken nextPage = new NextPageToken(request.getNextPageToken());
		List<EntityHeader> page = nodeManager.getChildren(request.getParentId(), request.getIncludeTypes(),
				childIdsToExclude, request.getSortBy(), request.getSortDirection(), nextPage.getLimitForQuery(),
				nextPage.getOffset());
		// Gather count and size sum if requested.
		ChildStatsResponse stats = nodeManager
				.getChildrenStats(new ChildStatsRequest().withParentId(request.getParentId())
						.withIncludeTypes(request.getIncludeTypes()).withChildIdsToExclude(childIdsToExclude)
						.withIncludeTotalChildCount(request.getIncludeTotalChildCount())
						.withIncludeSumFileSizes(request.getIncludeSumFileSizes()));
		EntityChildrenResponse response = new EntityChildrenResponse();
		response.setPage(page);
		response.setNextPageToken(nextPage.getNextPageTokenForCurrentResults(page));
		response.setTotalChildCount(stats.getTotalChildCount());
		response.setSumFileSizesBytes(stats.getSumFileSizesBytes());
		return response;
	}

	@Override
	public EntityId lookupChild(UserInfo userInfo, EntityLookupRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getEntityName(), "EntityLookupRequest.entityName");
		if (request.getParentId() == null) {
			// Null parentId is used to look up projects.
			request.setParentId(ROOT_ID);
		}
		if (!NodeUtils.isRootEntityId(request.getParentId())) {
			if (!entityAuthorizationManager.hasAccess(userInfo, request.getParentId(), ACCESS_TYPE.READ).isAuthorized()) {
				throw new UnauthorizedException("Lack of READ permission on the parent entity.");
			}
		}
		String entityId = nodeManager.lookupChild(request.getParentId(), request.getEntityName());
		if (!entityAuthorizationManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ).isAuthorized()) {
			throw new UnauthorizedException("Lack of READ permission on the requested entity.");
		}
		EntityId result = new EntityId();
		result.setId(entityId);
		return result;
	}

	@Override
	public DataTypeResponse changeEntityDataType(UserInfo userInfo, String entityId, DataType dataType) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "id");
		ValidateArgument.required(dataType, "DataType");
		return objectTypeManager.changeObjectsDataType(userInfo, entityId, ObjectType.ENTITY, dataType);
	}

	@WriteTransaction
	@Override
	public JsonSchemaObjectBinding bindSchemaToEntity(UserInfo userInfo, BindSchemaToEntityRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getEntityId(), "request.entityId");
		ValidateArgument.required(request.getSchema$id(), "request.schema$id");
		entityAuthorizationManager.hasAccess(userInfo, request.getEntityId(), ACCESS_TYPE.UPDATE)
				.checkAuthorizationOrElseThrow();
		JsonSchemaObjectBinding binding = jsonSchemaManager.bindSchemaToObject(userInfo.getId(), request.getSchema$id(),
				KeyFactory.stringToKey(request.getEntityId()), BoundObjectType.entity);
		sendEntityUpdateNotifications(request.getEntityId());
		return binding;
	}

	@Override
	public JsonSchemaObjectBinding getBoundSchema(UserInfo userInfo, String id) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(id, "id");
		entityAuthorizationManager.hasAccess(userInfo, id, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		return getBoundSchema(id);
	}
	
	@Override
	public JsonSchemaObjectBinding getBoundSchema(String entityId) {
		Long boundEntityId = nodeManager.findFirstBoundJsonSchema(KeyFactory.stringToKey(entityId));
		return jsonSchemaManager.getJsonSchemaObjectBinding(boundEntityId, BoundObjectType.entity);
	}
	

	@WriteTransaction
	@Override
	public void clearBoundSchema(UserInfo userInfo, String id) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(id, "id");
		entityAuthorizationManager.hasAccess(userInfo, id, ACCESS_TYPE.DELETE).checkAuthorizationOrElseThrow();
		jsonSchemaManager.clearBoundSchema(KeyFactory.stringToKey(id), BoundObjectType.entity);
		sendEntityUpdateNotifications(id);
	}
	
	void sendEntityUpdateNotifications(String entityId) {
		// Send a change for this entity.
		transactionalMessenger.sendMessageAfterCommit(entityId, ObjectType.ENTITY, ChangeType.UPDATE);
		EntityType type = nodeManager.getNodeType(entityId);
		if(NodeUtils.isProjectOrFolder(type)){
			// Send a recursive change to all children of this container.
			transactionalMessenger.sendMessageAfterCommit(entityId, ObjectType.ENTITY_CONTAINER, ChangeType.UPDATE);
		}
	}

	@Override
	public JSONObject getEntityJson(UserInfo userInfo, String entityId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");
		entityAuthorizationManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		return getEntityJson(entityId);
	}
	
	@Override
	public JSONObject getEntityJson(String entityId) {
		return getEntityJsonSubject(entityId).toJson();
	}

	@Override
	public JSONObject updateEntityJson(UserInfo userInfo, String entityId, JSONObject jsonObject) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");
		ValidateArgument.required(jsonObject, "jsonObject");
		// Note: Authorization checks occur at the node manager level.
		EntityType type = nodeManager.getNodeType(userInfo, entityId);
		Class<? extends Entity> entityClass = EntityTypeUtils.getClassForType(type);
		Annotations newAnnotations = annotationsTranslator.readFromJsonObject(entityClass, jsonObject);
		nodeManager.updateUserAnnotations(userInfo, entityId, newAnnotations);
		return getEntityJson(entityId);
	}

	@Override
	public JsonSubject getEntityJsonSubject(String entityId) {
		ValidateArgument.required(entityId, "entityId");
		Class<? extends Entity> entityClass = null;
		Entity entity = getEntity(entityId, entityClass);
		Annotations annotations = nodeManager.getUserAnnotations(entityId);
		JSONObject json = annotationsTranslator.writeToJsonObject(entity, annotations);
		return new EntityJsonSubject(entity, json);
	}

	@Override
	public ValidationResults getEntityValidationResults(UserInfo userInfo, String entityId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");
		entityAuthorizationManager.hasAccess(userInfo, entityId, ACCESS_TYPE.READ).checkAuthorizationOrElseThrow();
		return entitySchemaValidationResultDao.getValidationResults(entityId);
	}

	@Override
	public ValidationSummaryStatistics getEntityValidationStatistics(UserInfo userInfo, String entityId) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(entityId, "entityId");
		Set<Long> childIdsToExclude = authorizedListChildren(userInfo, entityId);
		return entitySchemaValidationResultDao.getEntityValidationStatistics(entityId, childIdsToExclude);
	}

	@Override
	public ListValidationResultsResponse getInvalidEntitySchemaValidationResults(UserInfo userInfo,
			ListValidationResultsRequest request) {
		ValidateArgument.required(userInfo, "userInfo");
		ValidateArgument.required(request, "request");
		ValidateArgument.required(request.getContainerId(), "request.containerId");
		Set<Long> childIdsToExclude = authorizedListChildren(userInfo, request.getContainerId());
		NextPageToken nextPage = new NextPageToken(request.getNextPageToken());
		List<ValidationResults> page = entitySchemaValidationResultDao.getInvalidEntitySchemaValidationPage(
				request.getContainerId(), childIdsToExclude, nextPage.getLimitForQuery(), nextPage.getOffset());
		ListValidationResultsResponse response = new ListValidationResultsResponse();
		response.setNextPageToken(nextPage.getNextPageTokenForCurrentResults(page));
		response.setPage(page);
		return response;
	}
	
	/**
	 * If the passed container ID is not the root Entity, then the caller must have the READ permission on the container.
	 * @param userInfo
	 * @param containerId
	 * @return The children IDs of the container that the caller does not have the READ permission.
	 */
	Set<Long> authorizedListChildren(UserInfo userInfo, String containerId){
		if (!NodeUtils.isRootEntityId((containerId))) {
			// The caller must have read on the container.
			entityAuthorizationManager.hasAccess(userInfo, containerId, ACCESS_TYPE.READ)
					.checkAuthorizationOrElseThrow();
		}
		// Find the children of this entity that the caller cannot see.
		return entityAclManager.getNonvisibleChildren(userInfo, containerId);
	}
}
