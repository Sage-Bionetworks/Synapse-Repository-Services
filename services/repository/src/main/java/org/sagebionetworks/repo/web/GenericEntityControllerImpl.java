package org.sagebionetworks.repo.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.repo.manager.AuthorizationManager;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AccessControlList;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseChild;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.util.SchemaHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation for REST controller for CRUD operations on Base DTOs and Base
 * DAOs
 * <p>
 * 
 * This class performs the basic CRUD operations for all our DAO-backed model
 * objects. See controllers specific to particular models for any special
 * handling.
 * 
 * @author deflaux
 * @param <T>
 *            the particular type of entity the controller is managing
 */
@SuppressWarnings({"rawtypes","unchecked"})
public class GenericEntityControllerImpl implements GenericEntityController {


	@Autowired
	EntitiesAccessor entitiesAccessor;
	@Autowired
	EntityManager entityManager;
	
	@Autowired
	PermissionsManager permissionsManager;
	
	@Autowired
	UserManager userManager;
	
	public GenericEntityControllerImpl(){
		
	}
	

	/**
	 * Provided for tests
	 * @param entitiesAccessor
	 * @param entityManager
	 */
	GenericEntityControllerImpl(EntitiesAccessor entitiesAccessor,
			EntityManager entityManager) {
		super();
		this.entitiesAccessor = entitiesAccessor;
		this.entityManager = entityManager;
	}

	@Override
	public <T extends Base> PaginatedResults<T> getEntities(String userId, Integer offset,
			Integer limit, String sort, Boolean ascending,
			HttpServletRequest request, Class<? extends T> clazz) throws DatastoreException, NotFoundException, UnauthorizedException {

		ServiceConstants.validatePaginationParams(offset, limit);
		
		UserInfo userInfo = userManager.getUserInfo(userId);
		PaginatedResults<T> results = entitiesAccessor.getInRangeSortedBy(userInfo, offset, limit,
				sort, ascending, clazz);
		List<T> entities = results.getResults();

		for (T entity : entities) {
			addServiceSpecificMetadata(entity, request);
		}
		return new PaginatedResults<T>(request.getServletPath()
				+ UrlHelpers.getUrlForModel(clazz), entities,
				results.getTotalNumberOfResults(), offset, limit, sort, ascending);
	}

	@Override
	public <T extends Base> T getEntity(String userId, String id, HttpServletRequest request, Class<? extends T> clazz)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		UserInfo userInfo = userManager.getUserInfo(userId);
		T entity = entityManager.getEntity(userInfo, entityId, clazz);
		if (null == entity) {
			throw new NotFoundException("no entity with id " + entityId
					+ " exists");
		}

		addServiceSpecificMetadata(entity, request);

		return entity;
	}

	@Override
	public <T extends Base> T createEntity(String userId, T newEntity, HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException, NotFoundException {

		UserInfo userInfo = userManager.getUserInfo(userId);
		String id = entityManager.createEntity(userInfo, newEntity);
		newEntity = (T) entityManager.getEntity(userInfo, id, newEntity.getClass());

		addServiceSpecificMetadata(newEntity, request);

		return newEntity;
	}

	@Override
	public <T extends Base> T updateEntity(String userId, String id,
			T updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);
		UserInfo userInfo = userManager.getUserInfo(userId);
		entityManager.updateEntity(userInfo, updatedEntity);
		updatedEntity = (T) entityManager.getEntity(userInfo, entityId, updatedEntity.getClass());

		addServiceSpecificMetadata(updatedEntity, request);

		return updatedEntity;
	}

	@Override
	public void deleteEntity(String userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		UserInfo userInfo = userManager.getUserInfo(userId);
		entityManager.deleteEntity(userInfo, entityId);

		return;
	}

	@Override
	public <T extends Base> JsonSchema getEntitySchema(Class<? extends T> clazz) throws DatastoreException {
		return SchemaHelper.getSchema(clazz);
	}
	
	@Override
	public <T extends Base> JsonSchema getEntitiesSchema(Class<? extends T> clazz) throws DatastoreException {
		// TODO is there a better way to pass this class?
		PaginatedResults<T> empty = new PaginatedResults<T>();
		return SchemaHelper.getSchema(empty.getClass());
	}

	private <T extends Base> void addServiceSpecificMetadata(T entity, HttpServletRequest request) {
		entity.setUri(UrlHelpers.makeEntityUri(entity, request));
	}

	private void addServiceSpecificMetadata(String id, Annotations annotations,
			HttpServletRequest request) {
		annotations.setId(id); // the NON url-encoded id
		annotations.setUri(UrlHelpers.makeEntityPropertyUri(request));
	}

	@Override
	public Annotations getEntityAnnotations(String userId, String id,
			HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		Annotations annotations = entityManager.getAnnotations(userInfo, id);
		addServiceSpecificMetadata(id, annotations, request);
		return annotations;
	}


	@Override
	public Annotations updateEntityAnnotations(String userId, String entityId,
			Annotations updatedAnnotations, HttpServletRequest request) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updatedAnnotations.getId() == null) throw new IllegalArgumentException("Annotations must have a non-null id");
		UserInfo userInfo = userManager.getUserInfo(userId);
		entityManager.updateAnnotations(userInfo,entityId, updatedAnnotations);
		Annotations annos = entityManager.getAnnotations(userInfo, updatedAnnotations.getId());
		addServiceSpecificMetadata(updatedAnnotations.getId(), annos, request);
		return annos;
	}

	@Override
	public JsonSchema getEntityAnnotationsSchema() throws DatastoreException {
		return SchemaHelper.getSchema(Annotations.class);
	}

	@Override
	public <T extends BaseChild> List<T> getEntityChildrenOfType(String userId,
			String parentId, Class<? extends T> clazz, HttpServletRequest request) throws DatastoreException, NotFoundException, UnauthorizedException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<T> list =  entitiesAccessor.getChildrenOfType(userInfo, parentId, clazz);
		Iterator<T> it = list.iterator();
		while(it.hasNext()){
			addServiceSpecificMetadata(it.next(), request);
		}
		return list;
	}

	@Override
	public <T extends BaseChild> Collection<T> aggregateEntityUpdate(String userId, String parentId, Collection<T> update,	HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException {
		if(update == null) return null;
		if(update.isEmpty()) return update;
		// First try the updated
		UserInfo userInfo = userManager.getUserInfo(userId);
		List<String> updatedIds = entityManager.aggregateEntityUpdate(userInfo, parentId, update);
		// Now create the update object
		List<T> newList = new ArrayList<T>();
		Class tClass = update.iterator().next().getClass();
		for(int i=0; i<updatedIds.size(); i++){
			newList.add((T) entityManager.getEntity(userInfo, updatedIds.get(i), tClass));
		}
		return newList;
	}


	@Override
	public AccessControlList getEntityACL(String entityId, String userId)
			throws NotFoundException, DatastoreException {
		// First try the updated
		UserInfo userInfo = userManager.getUserInfo(userId);
		return permissionsManager.getACL(entityId, userInfo);
	}


	@Override
	public AccessControlList updateEntityACL(String userId,
			AccessControlList updated) throws DatastoreException, NotFoundException, InvalidModelException, UnauthorizedException {
		// Resolve the user
		UserInfo userInfo = userManager.getUserInfo(userId);
		return permissionsManager.updateACL(updated, userInfo);
	}


}
