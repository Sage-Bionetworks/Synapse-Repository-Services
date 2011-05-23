package org.sagebionetworks.repo.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.text.html.parser.Entity;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseChild;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.ObjectType;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class EntityManagerImpl implements EntityManager {
	
//	private static final Logger log = Logger.getLogger(EntityManagerImpl.class.getName());
	
	@Autowired
	NodeManager nodeManager;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Base> String createEntity(UserInfo userInfo, T newEntity) throws DatastoreException, InvalidModelException, UnauthorizedException, NotFoundException {
		if(newEntity == null) throw new IllegalArgumentException("Dataset cannot be null");
		// First create a node the represent the dataset
		Node node = NodeTranslationUtils.createFromBase(newEntity);
		// Set the type for this object
		node.setNodeType(ObjectType.getNodeTypeForClass(newEntity.getClass()).toString());
		// We are ready to create this node
		String nodeId = nodeManager.createNewNode(node, userInfo);
		// Now get the annotations for this node
		Annotations annos = nodeManager.getAnnotations(userInfo, nodeId);
		// Now add all of the annotations from the dataset
		NodeTranslationUtils.updateAnnoationsFromObject(newEntity, annos);
		// Now update the annotations
		try {
			nodeManager.updateAnnotations(userInfo,nodeId, annos);
		} catch (ConflictingUpdateException e) {
			// This should never happen
			throw new DatastoreException("A confilct occured on a node create", e);
		}
		// Now fetch the dataset we just created
		return nodeId;
	}
	
	
	@Transactional(readOnly = true)
	@Override
	public <T extends Base> EntityWithAnnotations<T> getEntityWithAnnotations(UserInfo userInfo, String entityId, Class<? extends T> entityClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		// Return the new object from the database
		T newEntity = createNewEntity(entityClass);
		// Get the annotations for this entity
		Annotations annos = nodeManager.getAnnotations(userInfo, entityId);
		NodeTranslationUtils.updateObjectFromAnnotations(newEntity, annos);
		// Fetch the current node from the server
		Node node = nodeManager.get(userInfo, entityId);
		NodeTranslationUtils.updateObjectFromNode(newEntity, node);
		EntityWithAnnotations<T> ewa = new EntityWithAnnotations<T>();
		ewa.setEntity(newEntity);
		ewa.setAnnotations(annos);
		return ewa;
	}

	@Transactional(readOnly = true)
	@Override
	public <T extends Base> T getEntity(UserInfo userInfo, String entityId, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Entity ID cannot be null");
		// To fully populate an entity we must also load its annotations.
		// Therefore, we get both but only return the entity for this call.
		EntityWithAnnotations<T> ewa = getEntityWithAnnotations(userInfo, entityId, entityClass);
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
		if(entityId == null) throw new IllegalArgumentException("Dataset ID cannot be null");
		nodeManager.delete(userInfo, entityId);
	}

	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnotations(UserInfo userInfo, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Dataset ID cannot be null");
		// This is a simple pass through
		return nodeManager.getAnnotations(userInfo, entityId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateAnnotations(UserInfo userInfo, String entityId, Annotations updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updated == null) throw new IllegalArgumentException("Annoations cannot be null");
		nodeManager.updateAnnotations(userInfo,entityId, updated);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Base> void updateEntity(UserInfo userInfo, T updated) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException {
		if(updated == null) throw new IllegalArgumentException("Dataset cannot be null");
		if(updated.getId() == null) throw new IllegalArgumentException("The updated Entity cannot have a null ID");
		// Now get the annotations for this node
		Annotations annos = nodeManager.getAnnotations(userInfo, updated.getId());
		// Now add all of the annotations from the entity
		NodeTranslationUtils.updateAnnoationsFromObject(updated, annos);
		Node node = nodeManager.get(userInfo, updated.getId());
		NodeTranslationUtils.updateNodeFromObject(updated, node);
		annos.setEtag(updated.getEtag());
		// NOw update both at the same time
		nodeManager.update(userInfo, node, annos);
	}

	@Override
	public void overrideAuthDaoForTest(AuthorizationManager mockAuth) {
		nodeManager.setAuthorizationManager(mockAuth);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends BaseChild> List<String> aggregateEntityUpdate(UserInfo userInfo, String parentId, Collection<T> update) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException, InvalidModelException {
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
				updateEntity(userInfo, child);
			}
			ids.add(id);
		}
		return ids;
	}


	@Override
	public <T extends BaseChild> List<T> getEntityChildren(UserInfo userInfo,
			String parentId, Class<? extends T> childrenClass)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		List<T> resultSet = new ArrayList<T>();
		Set<Node> children = nodeManager.getChildren(userInfo, parentId);
		Iterator<Node> it = children.iterator();
		ObjectType type = ObjectType.getNodeTypeForClass(childrenClass);
		while(it.hasNext()){
			Node child = it.next();
			if(ObjectType.valueOf(child.getNodeType()) == type){
				resultSet.add(this.getEntity(userInfo, child.getId(), childrenClass));
			}
		}
		return resultSet;
	}


}
