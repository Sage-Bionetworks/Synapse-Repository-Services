package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import org.sagebionetworks.StackConfigurationSingleton;
import org.sagebionetworks.repo.manager.file.MultipartUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
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
import org.sagebionetworks.repo.model.NodeDAO;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.entity.Direction;
import org.sagebionetworks.repo.model.entity.EntityLookupRequest;
import org.sagebionetworks.repo.model.entity.SortBy;
import org.sagebionetworks.repo.model.file.ChildStatsRequest;
import org.sagebionetworks.repo.model.file.ChildStatsResponse;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.model.table.EntityView;
import org.sagebionetworks.repo.model.table.TableEntity;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.util.ValidateArgument;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class EntityManagerImpl implements EntityManager {

	public static final Direction DEFAULT_SORT_DIRECTION = Direction.ASC;
	public static final SortBy DEFAULT_SORT_BY = SortBy.NAME;
	public static final String ROOT_ID = StackConfigurationSingleton.singleton().getRootFolderEntityId();
	public static final List<EntityType> PROJECT_ONLY = Lists.newArrayList(EntityType.project);
	
	@Autowired
	NodeManager nodeManager;
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	@Autowired
	UserManager userManager;
	@Autowired
	ObjectTypeManager objectTypeManager;
	
	boolean allowCreationOfOldEntities = true;

	//Temporary fields!
	@Autowired
	NodeDAO nodeDAO;

	/**
	 * Injected via spring.
	 * @param allowOldEntityTypes
	 */
	public void setAllowCreationOfOldEntities(boolean allowCreationOfOldEntities) {
		this.allowCreationOfOldEntities = allowCreationOfOldEntities;
	}

	@WriteTransaction
	@Override
	public <T extends Entity> String createEntity(UserInfo userInfo, T newEntity, String activityId)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException {
		if (newEntity == null)
			throw new IllegalArgumentException("Entity cannot be null");
		// First create a node the represent the entity
		Node node = NodeTranslationUtils.createFromEntity(newEntity);
		// Set the type for this object
		node.setNodeType(EntityTypeUtils.getEntityTypeForClass(newEntity.getClass()));
		node.setActivityId(activityId);
		Annotations entityPropertyAnnotations = new Annotations();
		// Now add all of the annotations and references from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(newEntity, entityPropertyAnnotations);
		// We are ready to create this node
		node = nodeManager.createNewNode(node, entityPropertyAnnotations, userInfo);
		// Return the id of the newly created entity
		return node.getId();
	}

	@Override
	public <T extends Entity> T getEntityWithAnnotations(
			UserInfo userInfo, String entityId, Class<? extends T> entityClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the annotations for this entity
		Annotations entityPropertyAnnotations = nodeManager.getEntityPropertyAnnotations(userInfo, entityId);
		// Fetch the current node from the server
		Node node = nodeManager.get(userInfo, entityId);
		// Does the node type match the requested type?
		validateType(EntityTypeUtils.getEntityTypeForClass(entityClass),
				node.getNodeType(), entityId);
		return populateEntityWithNodeAndAnnotations(entityClass, entityPropertyAnnotations, node);
	}
	
	@Override
	public Entity getEntity(UserInfo user, String entityId) throws DatastoreException, UnauthorizedException, NotFoundException {
		// Get the annotations for this entity
		Annotations entityPropertyAnnotations = nodeManager.getEntityPropertyAnnotations(user, entityId);
		// Fetch the current node from the server
		Node node = nodeManager.get(user, entityId);
		return populateEntityWithNodeAndAnnotations(EntityTypeUtils.getClassForType(node.getNodeType()), entityPropertyAnnotations, node);
	}

	/**
	 * Validate that the requested entity type matches the actual entity type.
	 * See http://sagebionetworks.jira.com/browse/PLFM-431.
	 * 
	 * @param <T>
	 * @param requestedType
	 * @param acutalType
	 * @param id
	 */
	private <T extends Entity> void validateType(EntityType requestedType,
			EntityType acutalType, String id) {
		if (acutalType != requestedType) {
			throw new IllegalArgumentException("The Entity: syn" + id
					+ " has an entityType=" + EntityTypeUtils.getEntityTypeClassName(acutalType)
					+ " and cannot be changed to entityType="
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
	private <T extends Entity> T getEntityVersionWithAnnotations(
			UserInfo userInfo, String entityId, Long versionNumber,
			Class<? extends T> entityClass) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Get the annotations for this entity
		Annotations annos = nodeManager.getEntityPropertyForVersion(userInfo,
				entityId, versionNumber);
		// Fetch the current node from the server
		Node node = nodeManager.getNodeForVersionNumber(userInfo, entityId,
				versionNumber);
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
	private <T extends Entity> T populateEntityWithNodeAndAnnotations(
			Class<? extends T> entityClass, Annotations entityProperties, Node node)
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
	public <T extends Entity> T getEntity(UserInfo userInfo, String entityId,
			Class<? extends T> entityClass) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity ID cannot be null");
		// To fully populate an entity we must also load its annotations.
		// Therefore, we get both but only return the entity for this call.
		return getEntityWithAnnotations(userInfo, entityId, entityClass);
	}

	@Override
	public <T extends Entity> T getEntityForVersion(UserInfo userInfo,
			String entityId, Long versionNumber, Class<? extends T> entityClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// To fully populate an entity we must also load its annotations.
		// Therefore, we get both but only return the entity for this call.
		return getEntityVersionWithAnnotations(userInfo, entityId, versionNumber, entityClass);
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
	private <T> T createNewEntity(Class<? extends T> entityClass) {
		T newEntity;
		try {
			newEntity = entityClass.newInstance();
		} catch (InstantiationException e) {
			throw new IllegalArgumentException(
					"Class must have a no-argument constructor: "
							+ Entity.class.getName());
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException(
					"Class must have a public no-argument constructor: "
							+ Entity.class.getName());
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
	public void deleteEntityVersion(UserInfo userInfo, String id,
			Long versionNumber) throws NotFoundException, DatastoreException,
			UnauthorizedException, ConflictingUpdateException {
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
	public Annotations getAnnotationsForVersion(UserInfo userInfo, String id,
			Long versionNumber) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		// Get all of the annotations.
		return nodeManager.getUserAnnotationsForVersion(userInfo, id, versionNumber);
	}

	@WriteTransaction
	@Override
	public void updateAnnotations(UserInfo userInfo, String entityId,
			Annotations updated) throws ConflictingUpdateException,
			NotFoundException, DatastoreException, UnauthorizedException,
			InvalidModelException {
		if (updated == null)
			throw new IllegalArgumentException("Annoations cannot be null");
		// The user has updated the additional annotations.
		
		Annotations updatedClone = new Annotations();
		cloneAnnotations(updated, updatedClone);
		// the following *changes* the passed annotations (specifically the etag) so we just pass a clone
		nodeManager.updateUserAnnotations(userInfo, entityId, updatedClone);
	}
	
	public static void cloneAnnotations(Annotations src, Annotations dst) {
		dst.setBlobAnnotations(src.getBlobAnnotations());
		dst.setDateAnnotations(src.getDateAnnotations());
		dst.setDoubleAnnotations(src.getDoubleAnnotations());
		dst.setEtag(src.getEtag());
		dst.setId(src.getId());
		dst.setLongAnnotations(src.getLongAnnotations());
		dst.setStringAnnotations(src.getStringAnnotations());
	}

	@WriteTransaction
	@Override
	public <T extends Entity> boolean updateEntity(UserInfo userInfo, T updated,
			boolean newVersion, String activityId) throws NotFoundException, DatastoreException,
			UnauthorizedException, ConflictingUpdateException,
			InvalidModelException {

		if (updated == null)
			throw new IllegalArgumentException("Entity cannot be null");
		if (updated.getId() == null)
			throw new IllegalArgumentException(
					"The updated Entity cannot have a null ID");

		Node node = nodeManager.get(userInfo, updated.getId());
		// Now get the annotations for this node
		Annotations entityPropertyAnnotations = nodeManager.getEntityPropertyAnnotations(userInfo,
				updated.getId());
		
		// Auto-version FileEntity See PLFM-1744
		if(!newVersion && (updated instanceof FileEntity)){
			FileEntity updatedFile = (FileEntity) updated;
			if(!updatedFile.getDataFileHandleId().equals(node.getFileHandleId())){
				newVersion = true;
				// setting this to null we cause the revision id to be used.
				updatedFile.setVersionLabel(null);
				updatedFile.setVersionComment(null);
			}
		}

		if(updated instanceof TableEntity || updated instanceof EntityView) {
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
		
		// Set activityId if new version or if not changing versions and activityId is defined
		if(newVersionFinal || (!newVersionFinal && activityId != null)) {
			node.setActivityId(activityId);
		}
		
		updateNodeAndAnnotationsFromEntity(updated, node, entityPropertyAnnotations);
		// Now update both at the same time
		nodeManager.update(userInfo, node, entityPropertyAnnotations, null, newVersionFinal);
		return newVersionFinal;
	}

	/**
	 * Will update both the passed node and annotations using the passed entity
	 * 
	 * @param <T>
	 * @param entity
	 * @param node
	 * @param annos
	 */
	private <T extends Entity> void updateNodeAndAnnotationsFromEntity(
			T entity, Node node, Annotations annos) {
		// Update the annotations from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(entity, annos);
		// Update the node from the entity
		NodeTranslationUtils.updateNodeFromObject(entity, node);
		// Set the Annotations Etag
		annos.setEtag(entity.getEtag());
	}

	@WriteTransaction
	@Override
	public <T extends Entity> List<String> aggregateEntityUpdate(
			UserInfo userInfo, String parentId, Collection<T> update)
			throws NotFoundException, DatastoreException,
			UnauthorizedException, ConflictingUpdateException,
			InvalidModelException {
		if (update == null)
			throw new IllegalArgumentException("AggregateUpdate cannot be null");
		// We are going to lock on the parent so the first step is to get the
		// parent
		Node parentNode = nodeManager.get(userInfo, parentId);
		// We start by updating the parent, this will lock the parent for this
		// transaction
		// If we do not lock on the parent then we could get DEADLOCK while
		// attempting to lock the children.
		nodeManager.update(userInfo, parentNode);
		Iterator<T> it = update.iterator();
		List<String> ids = new ArrayList<String>();
		while (it.hasNext()) {
			T child = it.next();
			// Each child must have this parent's id
			child.setParentId(parentId);
			// Update each child.
			String id = null;
			if (child.getId() == null) {
				id = this.createEntity(userInfo, child, null);
			} else {
				id = child.getId();
				updateEntity(userInfo, child, false, null);
			}
			ids.add(id);
		}
		return ids;
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
	public EntityHeader getEntityHeader(UserInfo userInfo, String entityId, Long versionNumber)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		return nodeManager.getNodeHeader(userInfo, entityId, versionNumber);
	}
	
	@Override
	public List<EntityHeader> getEntityHeader(UserInfo userInfo,
			List<Reference> references) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		return nodeManager.getNodeHeader(userInfo, references);
	}

	@Override
	public List<VersionInfo> getVersionsOfEntity(UserInfo userInfo, String entityId,
			long offset, long limit) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		// pass through
		List<VersionInfo> versionsOfEntity = nodeManager.getVersionsOfEntity(userInfo, entityId, offset, limit);
		for (VersionInfo version : versionsOfEntity) {
			version.setModifiedBy(version.getModifiedByPrincipalId());
		}
		return versionsOfEntity;
	}

	@Override
	public List<EntityHeader> getEntityPath(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// pass through
		return nodeManager.getNodePath(userInfo, entityId);
	}

	@Override
	public String getEntityPathAsFilePath(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException,
			UnauthorizedException {
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
	public List<EntityHeader> getEntityPathAsAdmin(String entityId)
			throws NotFoundException, DatastoreException {
		// pass through
		return nodeManager.getNodePathAsAdmin(entityId);
	}
	
	@Override
	public void validateReadAccess(UserInfo userInfo, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		if (!entityPermissionsManager.hasAccess(entityId,
				ACCESS_TYPE.READ, userInfo).isAuthorized()) {
			throw new UnauthorizedException(
					"update access is required to obtain an S3Token for entity "
							+ entityId);
		}
	}
	
	@Override
	public void validateUpdateAccess(UserInfo userInfo, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		if (!entityPermissionsManager.hasAccess(entityId,
				ACCESS_TYPE.UPDATE, userInfo).isAuthorized()) {
			throw new UnauthorizedException(
					"update access is required to obtain an S3Token for entity "
							+ entityId);
		}
	}

	@Override
	public boolean doesEntityHaveChildren(UserInfo userInfo, String entityId) throws DatastoreException, UnauthorizedException, NotFoundException {
		validateReadAccess(userInfo, entityId);
		return nodeManager.doesNodeHaveChildren(entityId);
	}

	@Override
	public Activity getActivityForEntity(UserInfo userInfo, String entityId,
			Long versionNumber) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		return nodeManager.getActivityForNode(userInfo, entityId, versionNumber);		
	}

	@WriteTransaction
	@Override	
	public Activity setActivityForEntity(UserInfo userInfo, String entityId,
			String activityId) throws DatastoreException,
			UnauthorizedException, NotFoundException {
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
	public EntityChildrenResponse getChildren(UserInfo user,
			EntityChildrenRequest request) {
		ValidateArgument.required(user, "UserInfo");
		ValidateArgument.required(request, "EntityChildrenRequest");
		if(request.getParentId() == null){
			// Null parentId is used to list projects.
			request.setParentId(ROOT_ID);
			request.setIncludeTypes(PROJECT_ONLY);
		}
		ValidateArgument.required(request.getIncludeTypes(), "EntityChildrenRequest.includeTypes");
		if(request.getIncludeTypes().isEmpty()){
			throw new IllegalArgumentException("EntityChildrenRequest.includeTypes must include at least one type");
		}
		if(request.getSortBy() == null){
			request.setSortBy(DEFAULT_SORT_BY);
		}
		if(request.getSortDirection() == null){
			request.setSortDirection(DEFAULT_SORT_DIRECTION);
		}
		if(!ROOT_ID.equals(request.getParentId())){
			// Validate the caller has read access to the parent
			entityPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, user).checkAuthorizationOrElseThrow();
		}

		// Find the children of this entity that the caller cannot see.
		Set<Long> childIdsToExclude = entityPermissionsManager.getNonvisibleChildren(user, request.getParentId());
		NextPageToken nextPage = new NextPageToken(request.getNextPageToken());
		List<EntityHeader> page = nodeManager.getChildren(
				request.getParentId(), request.getIncludeTypes(),
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
		if(request.getParentId() == null){
			// Null parentId is used to look up projects.
			request.setParentId(ROOT_ID);
		}
		if(!ROOT_ID.equals(request.getParentId())){
			if(!entityPermissionsManager.hasAccess(request.getParentId(), ACCESS_TYPE.READ, userInfo).isAuthorized()){
				throw new UnauthorizedException("Lack of READ permission on the parent entity.");
			}
		}
		String entityId = nodeManager.lookupChild(request.getParentId(), request.getEntityName());
		if(!entityPermissionsManager.hasAccess(entityId, ACCESS_TYPE.READ, userInfo).isAuthorized()){
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
}
