package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.sagebionetworks.repo.manager.NodeManager.FileHandleReason;
import org.sagebionetworks.repo.manager.file.MultipartManagerImpl;
import org.sagebionetworks.repo.manager.file.MultipartUtils;
import org.sagebionetworks.repo.model.ACCESS_TYPE;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.EntityTypeUtils;
import org.sagebionetworks.repo.model.EntityWithAnnotations;
import org.sagebionetworks.repo.model.FileEntity;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.QueryResults;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.VersionInfo;
import org.sagebionetworks.repo.model.provenance.Activity;
import org.sagebionetworks.repo.transactions.WriteTransaction;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class EntityManagerImpl implements EntityManager {

	@Autowired
	NodeManager nodeManager;
	@Autowired
	private EntityPermissionsManager entityPermissionsManager;
	@Autowired
	UserManager userManager;
	
	boolean allowCreationOfOldEntities = true;
	
	/**
	 * Injected via spring.
	 * @param allowOldEntityTypes
	 */
	public void setAllowCreationOfOldEntities(boolean allowCreationOfOldEntities) {
		this.allowCreationOfOldEntities = allowCreationOfOldEntities;
	}

	public EntityManagerImpl() {
	}
	
	public EntityManagerImpl(NodeManager nodeManager,
			EntityPermissionsManager permissionsManager, UserManager userManager) {
		super();
		this.nodeManager = nodeManager;
		this.entityPermissionsManager = permissionsManager;
		this.userManager = userManager;
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
		NamedAnnotations annos = new NamedAnnotations();
		// Now add all of the annotations and references from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(newEntity, annos.getPrimaryAnnotations());
		// We are ready to create this node
		node = nodeManager.createNewNode(node, annos, userInfo);
		// Return the id of the newly created entity
		return node.getId();
	}

	@Override
	public <T extends Entity> EntityWithAnnotations<T> getEntityWithAnnotations(
			UserInfo userInfo, String entityId, Class<? extends T> entityClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the annotations for this entity
		NamedAnnotations annos = nodeManager.getAnnotations(userInfo, entityId);
		// Fetch the current node from the server
		Node node = nodeManager.get(userInfo, entityId);
		// Does the node type match the requested type?
		validateType(EntityTypeUtils.getEntityTypeForClass(entityClass),
				node.getNodeType(), entityId);
		return populateEntityWithNodeAndAnnotations(entityClass, annos, node);
	}
	
	@Override
	public Entity getEntity(UserInfo user, String entityId) throws DatastoreException, UnauthorizedException, NotFoundException {
		// Get the annotations for this entity
		NamedAnnotations annos = nodeManager.getAnnotations(user, entityId);
		// Fetch the current node from the server
		Node node = nodeManager.get(user, entityId);
		EntityWithAnnotations ewa = populateEntityWithNodeAndAnnotations(EntityTypeUtils.getClassForType(node.getNodeType()), annos, node);
		return ewa.getEntity();
	}
	
	@Override
	public <T extends Entity> T getEntitySecondaryFields(UserInfo user, String entityId, Class<T> type)  throws DatastoreException, UnauthorizedException, NotFoundException {
		// Get the annotations for this entity
		Node node = new Node();
		node.setCreatedByPrincipalId(0L);
		node.setModifiedByPrincipalId(0L);
		NamedAnnotations annos = nodeManager.getAnnotations(user, entityId);
		EntityWithAnnotations<T> ewa = populateEntityWithNodeAndAnnotations(type, annos, node);
		return ewa.getEntity();
	}

	@Override
	public <T extends Entity> T getEntitySecondaryFieldsForVersion(UserInfo user, String entityId, Long versionNumber, Class<T> type)
			throws DatastoreException, UnauthorizedException, NotFoundException {
		Node node = new Node();
		node.setCreatedByPrincipalId(0L);
		node.setModifiedByPrincipalId(0L);
		NamedAnnotations annos = nodeManager.getAnnotationsForVersion(user, entityId, versionNumber);
		EntityWithAnnotations<T> ewa = populateEntityWithNodeAndAnnotations(type, annos, node);
		return ewa.getEntity();
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
	private <T extends Entity> EntityWithAnnotations<T> getEntityVersionWithAnnotations(
			UserInfo userInfo, String entityId, Long versionNumber,
			Class<? extends T> entityClass) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		// Get the annotations for this entity
		NamedAnnotations annos = nodeManager.getAnnotationsForVersion(userInfo,
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
	 * @param annos
	 * @param node
	 * @return
	 */
	private <T extends Entity> EntityWithAnnotations<T> populateEntityWithNodeAndAnnotations(
			Class<? extends T> entityClass, NamedAnnotations annos, Node node)
			throws DatastoreException, NotFoundException {
		// Return the new object from the dataEntity
		T newEntity = createNewEntity(entityClass);
		// Populate the entity using the annotations and references
		NodeTranslationUtils.updateObjectFromNodeSecondaryFields(newEntity, annos.getPrimaryAnnotations());
		// Populate the entity using the node
		NodeTranslationUtils.updateObjectFromNode(newEntity, node);
		newEntity.setCreatedBy(node.getCreatedByPrincipalId().toString());
		newEntity.setModifiedBy(node.getModifiedByPrincipalId().toString());
		EntityWithAnnotations<T> ewa = new EntityWithAnnotations<T>();
		ewa.setEntity(newEntity);
		ewa.setAnnotations(annos.getAdditionalAnnotations());
		return ewa;
	}

	@Override
	public <T extends Entity> T getEntity(UserInfo userInfo, String entityId,
			Class<? extends T> entityClass) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		if (entityId == null)
			throw new IllegalArgumentException("Entity ID cannot be null");
		// To fully populate an entity we must also load its annotations.
		// Therefore, we get both but only return the entity for this call.
		EntityWithAnnotations<T> ewa = getEntityWithAnnotations(userInfo,
				entityId, entityClass);
		return ewa.getEntity();
	}

	@Override
	public <T extends Entity> T getEntityForVersion(UserInfo userInfo,
			String entityId, Long versionNumber, Class<? extends T> entityClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// To fully populate an entity we must also load its annotations.
		// Therefore, we get both but only return the entity for this call.
		EntityWithAnnotations<T> ewa = getEntityVersionWithAnnotations(
				userInfo, entityId, versionNumber, entityClass);
		return ewa.getEntity();
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
		NamedAnnotations annos = nodeManager.getAnnotations(userInfo, entityId);
		// When the user is asking for the annotations, then they want the
		// additional
		// annotations and not the primary annotations.
		return annos.getAdditionalAnnotations();
	}

	@Override
	public Annotations getAnnotationsForVersion(UserInfo userInfo, String id,
			Long versionNumber) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		// Get all of the annotations.
		NamedAnnotations annos = nodeManager.getAnnotationsForVersion(userInfo,
				id, versionNumber);
		// When the user is asking for the annotations, then they want the
		// additional
		// annotations and not the primary annotations.
		return annos.getAdditionalAnnotations();
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
		nodeManager.updateAnnotations(userInfo, entityId, updatedClone,
				NamedAnnotations.NAME_SPACE_ADDITIONAL);
	}
	
	public static void cloneAnnotations(Annotations src, Annotations dst) {
		dst.setBlobAnnotations(src.getBlobAnnotations());
		dst.setCreationDate(src.getCreationDate());
		dst.setDateAnnotations(src.getDateAnnotations());
		dst.setDoubleAnnotations(src.getDoubleAnnotations());
		dst.setEtag(src.getEtag());
		dst.setId(src.getId());
		dst.setLongAnnotations(src.getLongAnnotations());
		dst.setStringAnnotations(src.getStringAnnotations());
		dst.setUri(src.getUri());
	}

	@WriteTransaction
	@Override
	public <T extends Entity> void updateEntity(UserInfo userInfo, T updated,
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
		NamedAnnotations annos = nodeManager.getAnnotations(userInfo,
				updated.getId());
		annos.setEtag(updated.getEtag());
		
		// Auto-version FileEntity See PLFM-1744
		if(!newVersion && (updated instanceof FileEntity)){
			FileEntity updatedFile = (FileEntity) updated;
			if(!updatedFile.getDataFileHandleId().equals(node.getFileHandleId())){
				newVersion = true;
				// setting this to null we cause the revision id to be used.
				updatedFile.setVersionLabel(null);
			}
		}

		final boolean newVersionFinal = newVersion;
		
		// Set activityId if new version or if not changing versions and activityId is defined
		if(newVersionFinal || (!newVersionFinal && activityId != null)) {
			node.setActivityId(activityId);
		}
		
		updateNodeAndAnnotationsFromEntity(updated, node,
				annos.getPrimaryAnnotations());
		// Now update both at the same time
		nodeManager.update(userInfo, node, annos, newVersionFinal);
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
	public <T extends Entity> List<T> getEntityChildren(UserInfo userInfo,
			String parentId, Class<? extends T> childrenClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		List<T> resultSet = new ArrayList<T>();
		Set<Node> children = nodeManager.getChildren(userInfo, parentId);
		Iterator<Node> it = children.iterator();
		EntityType type = EntityTypeUtils.getEntityTypeForClass(childrenClass);
		while (it.hasNext()) {
			Node child = it.next();
			if (child.getNodeType() == type) {
				resultSet.add(this.getEntity(userInfo, child.getId(),
						childrenClass));
			}
		}
		return resultSet;
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
	public List<Long> getAllVersionNumbersForEntity(UserInfo userInfo,
			String entityId) throws NotFoundException, DatastoreException,
			UnauthorizedException {
		// pass through
		return nodeManager.getAllVersionNumbersForNode(userInfo, entityId);
	}

	@Override
	public QueryResults<VersionInfo> getVersionsOfEntity(UserInfo userInfo, String entityId,
			long offset, long limit) throws DatastoreException,
			UnauthorizedException, NotFoundException {
		// pass through
		QueryResults<VersionInfo> versionsOfEntity = nodeManager.getVersionsOfEntity(userInfo, entityId, offset, limit);
		for (VersionInfo version : versionsOfEntity.getResults()) {
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

	/**
	 * @param userInfo
	 * @param entityId
	 * @param versionNumber
	 * @return the headers of the entities which refer to the given entityId,
	 *         filtered by the access permissions of 'userInfo'
	 */
	public QueryResults<EntityHeader> getEntityReferences(UserInfo userInfo,
			String entityId, Integer versionNumber, Integer offset,
			Integer limit) throws NotFoundException, DatastoreException {
		// pass through

		QueryResults<EntityHeader> results = nodeManager.getEntityReferences(
				userInfo, entityId, versionNumber, offset, limit);
		// Note: This is a hack that we currently depend on for Mike's demo.
		// In the demo we want to show that one dataset is derived from another
		// dataset. The current implementation.
		// involves making a link entity as a child of the original dataset that
		// points to the derived datasts.
		// Lastly, from the derived datast's page we want to show the original
		// datast in the "Referenced By" window.
		// In order for this to work we must replace the link entity with its
		// PARENT! This is a total hack that cause
		// weird behavior for other scenarios but for now we must leave it in
		// place. The plan to address this issue
		// is with the new Provenance feature. Once that feature is in place the
		// derived dataset will have the following
		// property:
		// Reference derivedFrom
		// This propery will then point to the original dataset. At that point
		// this method will work without this hack!

		if (results != null && results.getResults() != null) {
			List<EntityHeader> list = results.getResults();
			for (int i = 0; i < list.size(); i++) {
				EntityHeader header = list.get(i);
				EntityType type = EntityType.valueOf(header.getType());
				if (EntityType.link == type) {
					try {
						List<EntityHeader> path = nodeManager.getNodePath(
								userInfo, header.getId());
						if (path != null && path.size() > 1) {
							// Get the parent path
							EntityHeader parent = path.get(path.size() - 2);
							list.set(i, parent);
						}
					} catch (UnauthorizedException e) {
						// This should not occur
						throw new DatastoreException(e);
					}

				}
			}
		}
		return results;
	}
	
	@Override
	public void validateReadAccess(UserInfo userInfo, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		if (!entityPermissionsManager.hasAccess(entityId,
				ACCESS_TYPE.READ, userInfo).getAuthorized()) {
			throw new UnauthorizedException(
					"update access is required to obtain an S3Token for entity "
							+ entityId);
		}
	}
	
	@Override
	public void validateUpdateAccess(UserInfo userInfo, String entityId)
			throws DatastoreException, NotFoundException, UnauthorizedException {
		if (!entityPermissionsManager.hasAccess(entityId,
				ACCESS_TYPE.UPDATE, userInfo).getAuthorized()) {
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
	public String getFileHandleIdForVersion(UserInfo userInfo, String id, Long versionNumber, FileHandleReason reason)
			throws UnauthorizedException, NotFoundException {
		// The manager handles this call.
		return nodeManager.getFileHandleIdForVersion(userInfo, id, versionNumber, reason);
	}

	@Override
	public List<Reference> getCurrentRevisionNumbers(List<String> entityIds) {
		return nodeManager.getCurrentRevisionNumbers(entityIds);
	}

	@Override
	public String getEntityIdForAlias(String alias) {
		return nodeManager.getEntityIdForAlias(alias);
	}



}
