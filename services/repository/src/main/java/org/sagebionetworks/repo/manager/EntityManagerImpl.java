package org.sagebionetworks.repo.manager;

import java.util.logging.Logger;

import javax.swing.text.html.parser.Entity;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.AuthorizationDAO;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.Node;
import org.sagebionetworks.repo.model.NodeQueryDao;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.query.ObjectType;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class EntityManagerImpl implements EntityManager {
	
	private static final Logger log = Logger.getLogger(EntityManagerImpl.class.getName());
	
	@Autowired
	NodeManager nodeManager;

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Base> String createEntity(String userId, T newEntity) throws DatastoreException, InvalidModelException, NotFoundException, UnauthorizedException {
		if(newEntity == null) throw new IllegalArgumentException("Dataset cannot be null");
		// First create a node the represent the dataset
		Node node = NodeTranslationUtils.createFromBase(newEntity);
		// Set the type for this object
		node.setType(ObjectType.getNodeTypeForClass(newEntity.getClass()).toString());
		node.setId(null);
		// We are ready to create this node
		String nodeId = nodeManager.createNewNode(node, userId);
		// Now get the annotations for this node
		Annotations annos = nodeManager.getAnnotations(userId, nodeId);
		// Now add all of the annotations from the dataset
		NodeTranslationUtils.updateAnnoationsFromObject(newEntity, annos);
		// Now update the annotations
		try {
			nodeManager.updateAnnotations(userId, annos);
		} catch (ConflictingUpdateException e) {
			// This should never happen
			throw new DatastoreException("A confilct occured on a node create", e);
		}
		// Now fetch the dataset we just created
		return nodeId;
	}

	@Transactional(readOnly = true)
	@Override
	public <T extends Base> T getEntity(String userId, String entityId, Class<? extends T> entityClass) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Dataset ID cannot be null");
		// Return the new object from the database
		T newEntity = createNewEntity(entityClass);
		// Get the annotations for this entity
		Annotations annos = nodeManager.getAnnotations(userId, entityId);
		NodeTranslationUtils.updateObjectFromAnnotations(newEntity, annos);
		// Fetch the current node from the server
		Node node = nodeManager.get(userId, entityId);
		NodeTranslationUtils.updateObjectFromNode(newEntity, node);
		// return the results
		return newEntity;
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
	public void deleteEntity(String userId, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Dataset ID cannot be null");
		nodeManager.delete(userId, entityId);
	}

	@Transactional(readOnly = true)
	@Override
	public Annotations getAnnoations(String userId, String entityId) throws NotFoundException, DatastoreException, UnauthorizedException {
		if(entityId == null) throw new IllegalArgumentException("Dataset ID cannot be null");
		// This is a simple pass through
		return nodeManager.getAnnotations(userId, entityId);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public void updateAnnotations(String userId, Annotations updated) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException {
		if(updated == null) throw new IllegalArgumentException("Annoations cannot be null");
		nodeManager.updateAnnotations(userId, updated);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	@Override
	public <T extends Base> void updateEntity(String userId, T updated) throws NotFoundException, DatastoreException, UnauthorizedException, ConflictingUpdateException {
		if(updated == null) throw new IllegalArgumentException("Dataset cannot be null");
		if(updated.getId() == null) throw new IllegalArgumentException("The updated dataset cannot have a null ID");
		// Now get the annotations for this node
		Annotations annos = nodeManager.getAnnotations(userId, updated.getId());
		// Now add all of the annotations from the entity
		NodeTranslationUtils.updateAnnoationsFromObject(updated, annos);
		Node node = nodeManager.get(userId, updated.getId());
		NodeTranslationUtils.updateNodeFromObject(updated, node);
		// NOw update both at the same time
		nodeManager.update(userId, node, annos);
	}

	@Override
	public void overrideAuthDaoForTest(AuthorizationDAO mockAuth) {
		nodeManager.setAuthorizationDAO(mockAuth);
	}

}
