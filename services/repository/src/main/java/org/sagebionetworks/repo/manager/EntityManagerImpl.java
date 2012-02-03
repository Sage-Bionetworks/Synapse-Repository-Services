package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Entity;
import org.sagebionetworks.repo.model.ConflictingUpdateException;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Locationable;
import org.sagebionetworks.repo.model.NamedAnnotations;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.EntityType;
import org.sagebionetworks.repo.model.NodeConstants;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 */
@Transactional(readOnly = true)
public class EntityManagerImpl implements EntityManager {
	
	@Autowired
	NodeManager nodeManager;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Entity> String createEntity(UserInfo userInfo, T newEntity) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		if(newEntity == null) throw new IllegalArgumentException("Entity cannot be null");
		// First create a node the represent the entity
		Node node = NodeTranslationUtils.createFromEntity(newEntity);
		// Set the type for this object
		node.setNodeType(EntityType.getNodeTypeForClass(newEntity.getClass()).name());
		NamedAnnotations annos = new NamedAnnotations();
		// Now add all of the annotations and references from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(newEntity, annos.getPrimaryAnnotations(), node.getReferences());
		// We are ready to create this node
		String nodeId = nodeManager.createNewNode(node, annos, userInfo);
		// Return the id of the newly created entity
		return nodeId;
	}
	
	@Transactional(readOnly = true)
	@Override
	public <T extends Entity> EntityWithAnnotations<T> getEntityWithAnnotations(UserInfo userInfo, String entityId, Class<? extends T> entityClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the annotations for this entity
		NamedAnnotations annos = nodeManager.getAnnotations(userInfo, entityId);
		// Fetch the current node from the server
		Node node = nodeManager.get(userInfo, entityId);
		// Does the node type match the requested type?
		validateType(EntityType.getNodeTypeForClass(entityClass), EntityType.valueOf(node.getNodeType()), entityId);
		return populateEntityWithNodeAndAnnotations(entityClass, annos, node);
	}
	
	/**
	 * Validate that the requested entity type matches the actual entity type. See http://sagebionetworks.jira.com/browse/PLFM-431.
	 * @param <T>
	 * @param requestedType
	 * @param acutalType
	 * @param id
	 */
	private <T extends Entity> void validateType(EntityType requestedType, EntityType acutalType, String id){
		if(acutalType != requestedType){
			throw new IllegalArgumentException("Requested "+requestedType.getUrlPrefix()+"/"+id+" but the entity with ID="+id+" is not of type: "+requestedType.name());
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
	@Transactional(readOnly = true)
	public <T extends Entity> EntityWithAnnotations<T> getEntityVersionWithAnnotations(UserInfo userInfo, String entityId, Long versionNumber, Class<? extends T> entityClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get the annotations for this entity
		NamedAnnotations annos = nodeManager.getAnnotationsForVersion(userInfo, entityId, versionNumber);
		// Fetch the current node from the server
		Node node = nodeManager.getNodeForVersionNumber(userInfo, entityId, versionNumber);
		return populateEntityWithNodeAndAnnotations(entityClass, annos, node);
	}


	/**
	 * Create and populate an instance of an entity using both a node and annotations.
	 * @param <T>
	 * @param entityClass
	 * @param annos
	 * @param node
	 * @return
	 */
	private <T extends Entity> EntityWithAnnotations<T> populateEntityWithNodeAndAnnotations(
			Class<? extends T> entityClass, NamedAnnotations annos, Node node) {
		// Return the new object from the dataEntity
		T newEntity = createNewEntity(entityClass);
		// Populate the entity using the annotations and references
		NodeTranslationUtils.updateObjectFromNodeSecondaryFields(newEntity, annos.getPrimaryAnnotations(), node.getReferences());
		// Populate the entity using the node
		NodeTranslationUtils.updateObjectFromNode(newEntity, node);
		EntityWithAnnotations<T> ewa = new EntityWithAnnotations<T>();
		ewa.setEntity(newEntity);
		ewa.setAnnotations(annos.getAdditionalAnnotations());
		return ewa;
	}

	@Transactional(readOnly = true)
	@Override
	public <T extends Entity> T getEntity(UserInfo userInfo, String entityId, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity ID cannot be null");
		// To fully populate an entity we must also load its annotations.
		// Therefore, we get both but only return the entity for this call.
		EntityWithAnnotations<T> ewa = getEntityWithAnnotations(userInfo, entityId, entityClass);
		return ewa.getEntity();
	}
	
	@Transactional(readOnly = true)
	@Override
	public <T extends Entity> T getEntityForVersion(UserInfo userInfo,
			String entityId, Long versionNumber, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException {
		// To fully populate an entity we must also load its annotations.
		// Therefore, we get both but only return the entity for this call.
		EntityWithAnnotations<T> ewa = getEntityVersionWithAnnotations(userInfo, entityId, versionNumber, entityClass);
		return ewa.getEntity();
	}
	
	/**
	 * Will convert the any exceptions to runtime.
	 * @param <T>
	 * @param entityClass
	 * @return
	 */
	private <T> T createNewEntity(Class<? extends T> entityClass) {
		T newEntity;
		try {
			newEntity = entityClass.newInstance();
		} catch (InstantiationException e) {
			throw new IllegalArgumentException("Class must have a no-argument constructor: "+Entity.class.getName());
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("Class must have a public no-argument constructor: "+Entity.class.getName());
		}
		return newEntity;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void deleteEntity(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity ID cannot be null");
		nodeManager.delete(userInfo, entityId);
	}
	
	@Override
	public void deleteEntityVersion(UserInfo userInfo, String id,
			Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		nodeManager.deleteVersion(userInfo, id, versionNumber);
	}

	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnotations(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity ID cannot be null");
		// This is a simple pass through
		NamedAnnotations annos = nodeManager.getAnnotations(userInfo, entityId);
		// When the user is asking for the annotations, then they want the additional
		// annotations and not the primary annotations.
		return annos.getAdditionalAnnotations();
	}
	
	@Override
	public Annotations getAnnotationsForVersion(UserInfo userInfo, String id,	Long versionNumber) throws NotFoundException, DatastoreException, UnauthorizedException {
		// Get all of the annotations.
		NamedAnnotations annos = nodeManager.getAnnotationsForVersion(userInfo, id, versionNumber);
		// When the user is asking for the annotations, then they want the additional
		// annotations and not the primary annotations.
		return annos.getAdditionalAnnotations();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateAnnotations(UserInfo userInfo, String entityId, Annotations updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updated == null) throw new IllegalArgumentException("Annoations cannot be null");
		// The user has updated the additional annotations.
		nodeManager.updateAnnotations(userInfo,entityId, updated, NamedAnnotations.NAME_SPACE_ADDITIONAL);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Entity> void updateEntity(UserInfo userInfo, T updated, boolean newVersion) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException {

		if(updated == null) throw new IllegalArgumentException("Entity cannot be null");
		if(updated.getId() == null) throw new IllegalArgumentException("The updated Entity cannot have a null ID");

		Node node = nodeManager.get(userInfo, updated.getId());	
		// Now get the annotations for this node
		NamedAnnotations annos = nodeManager.getAnnotations(userInfo, updated.getId());
		annos.setEtag(updated.getEtag());

		// Auto-version locationable entities
		if(false == newVersion && Locationable.class.isAssignableFrom(updated.getClass())) {
			Locationable locationable = (Locationable) updated;
			String currentMd5 = (String) annos.getPrimaryAnnotations().getSingleValue("md5");
			if(null != currentMd5 && !currentMd5.equals(locationable.getMd5())) {
				newVersion = true;
				// Programmatically construct a unique version label
				locationable.setVersionLabel(NodeConstants.AUTOCREATED_VERSION_LABEL_PREFIX + locationable.getVersionNumber().toString());
			}
		}

		updateNodeAndAnnotationsFromEntity(updated, node, annos.getPrimaryAnnotations());
		// Now update both at the same time
		nodeManager.update(userInfo, node, annos, newVersion);
	}
	
	/**
	 * Will update both the passed node and annotations using the passed entity
	 * @param <T>
	 * @param entity
	 * @param node
	 * @param annos
	 */
	private <T extends Entity> void updateNodeAndAnnotationsFromEntity(T entity, Node node, Annotations annos){
		// Update the annotations from the entity
		NodeTranslationUtils.updateNodeSecondaryFieldsFromObject(entity, annos, node.getReferences());
		// Update the node from the entity
		NodeTranslationUtils.updateNodeFromObject(entity, node);
		// Set the Annotations Etag
		annos.setEtag(entity.getEtag());
	}

	@Override
	public void overrideAuthDaoForTest(AuthorizationManager mockAuth) {
		nodeManager.setAuthorizationManager(mockAuth);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Entity> List<String> aggregateEntityUpdate(UserInfo userInfo, String parentId, Collection<T> update) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException {
		if(update == null) throw new IllegalArgumentException("AggregateUpdate cannot be null");
		// We are going to lock on the parent so the first step is to get the parent
		Node parentNode = nodeManager.get(userInfo, parentId);
		// We start by updating the parent, this will lock the parent for this transaction
		// If we do not lock on the parent then we could get DEADLOCK while attempting to lock the children.
		nodeManager.update(userInfo, parentNode);
		Iterator<T> it = update.iterator();
		List<String> ids = new ArrayList<String>();
		while(it.hasNext()){
			T child = it.next();
			// Each child must have this parent's id
			child.setParentId(parentId);
			// Update each child.
			String id = null;
			if(child.getId() == null){
				id = this.createEntity(userInfo, child);
			}else{
				id = child.getId();
				updateEntity(userInfo, child, false);
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
		EntityType type = EntityType.getNodeTypeForClass(childrenClass);
		while(it.hasNext()){
			Node child = it.next();
			if(EntityType.valueOf(child.getNodeType()) == type){
				resultSet.add(this.getEntity(userInfo, child.getId(), childrenClass));
			}
		}
		return resultSet;
	}


	@Override
	public EntityType getEntityType(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException {
		return nodeManager.getNodeType(userInfo, entityId);
	}
	
	@Override
	public EntityHeader getEntityHeader(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException {
		return nodeManager.getNodeHeader(userInfo, entityId);
	}

	@Override
	public List<Long> getAllVersionNumbersForEntity(UserInfo userInfo,
			String entityId) throws NotFoundException, DatastoreException, UnauthorizedException {
		// pass through
		return nodeManager.getAllVersionNumbersForNode(userInfo, entityId);
	}

	@Override
	public List<EntityHeader> getEntityPath(UserInfo userInfo, String entityId)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// pass through
		return nodeManager.getNodePath(userInfo, entityId);
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
	 * @return the headers of the entities which refer to the given entityId, filtered by the access permissions of 'userInfo'
	 */
	public List<EntityHeader> getEntityReferences(UserInfo userInfo, String entityId) 
				throws NotFoundException, DatastoreException {
		// pass through
		return nodeManager.getEntityReferences(userInfo, entityId);
	}

	/**
	 * @param userInfo
	 * @param entityId
	 * @param versionNumber
	 * @return the headers of the entities which refer to the given entityId, filtered by the access permissions of 'userInfo'
	 */
	public List<EntityHeader> getEntityReferences(UserInfo userInfo, String entityId, int versionNumber) 
				throws NotFoundException, DatastoreException {
		// pass through
		return nodeManager.getEntityReferences(userInfo, entityId, versionNumber);
	}

}
