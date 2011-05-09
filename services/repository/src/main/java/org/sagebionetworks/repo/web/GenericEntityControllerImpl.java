package org.sagebionetworks.repo.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.repo.manager.EntityManager;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseChild;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
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
public class GenericEntityControllerImpl implements GenericEntityController {


	@Autowired
	EntitiesAccessor2 entitiesAccessor;
	@Autowired
	EntityManager entityManager;


	@Override
	public <T extends Base> PaginatedResults<T> getEntities(String userId, Integer offset,
			Integer limit, String sort, Boolean ascending,
			HttpServletRequest request, Class<? extends T> clazz) throws DatastoreException, NotFoundException, UnauthorizedException {

		ServiceConstants.validatePaginationParams(offset, limit);
		
		PaginatedResults<T> results = entitiesAccessor.getInRangeSortedBy(userId, offset, limit,
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

		T entity = entityManager.getEntity(userId, entityId, clazz);
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

		String id = entityManager.createEntity(userId, newEntity);
		newEntity = (T) entityManager.getEntity(userId, id, newEntity.getClass());

		addServiceSpecificMetadata(newEntity, request);

		return newEntity;
	}

	@Override
	public <T extends Base> T updateEntity(String userId, String id,
			T updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);
		entityManager.updateEntity(userId, updatedEntity);
		updatedEntity = (T) entityManager.getEntity(userId, entityId, updatedEntity.getClass());

		addServiceSpecificMetadata(updatedEntity, request);

		return updatedEntity;
	}

	@Override
	public void deleteEntity(String userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		entityManager.deleteEntity(userId, entityId);

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
//		entity.setEtag(UrlHelpers.makeEntityEtag(entity));
	}

	private void addServiceSpecificMetadata(String id, Annotations annotations,
			HttpServletRequest request) {
		annotations.setId(id); // the NON url-encoded id
		annotations.setUri(UrlHelpers.makeEntityPropertyUri(request));
//		annotations.setEtag(UrlHelpers.makeEntityEtag(annotations));
	}

	@Override
	public Annotations getEntityAnnotations(String userId, String id,
			HttpServletRequest request) throws NotFoundException, DatastoreException, UnauthorizedException {
		// TODO Auto-generated method stub
		Annotations annoations = entityManager.getAnnoations(userId, id);
		addServiceSpecificMetadata(id, annoations, request);
		return annoations;
	}


	@Override
	public Annotations updateEntityAnnotations(String userId, String entityId,
			Annotations updatedAnnotations, HttpServletRequest request) throws ConflictingUpdateException, NotFoundException, DatastoreException, UnauthorizedException, InvalidModelException {
		if(updatedAnnotations.getId() == null) throw new IllegalArgumentException("Annotations must have a non-null id");
		entityManager.updateAnnotations(userId,entityId, updatedAnnotations);
		Annotations annos = entityManager.getAnnoations(userId, updatedAnnotations.getId());
		addServiceSpecificMetadata(updatedAnnotations.getId(), annos, request);
		return annos;
	}

	@Override
	public JsonSchema getEntityAnnotationsSchema() throws DatastoreException {
		return SchemaHelper.getSchema(Annotations.class);
	}

	@Override
	public <T extends BaseChild> List<T> getEntityChildrenOfType(String userId,
			String parentId, Class<? extends T> clazz) throws DatastoreException, NotFoundException, UnauthorizedException {
		// TODO Auto-generated method stub
		return entitiesAccessor.getChildrenOfType(userId, parentId, clazz);
	}

	@Override
	public <T extends BaseChild> Collection<T> aggregateEntityUpdate(String userId, String parentId, Collection<T> update,	HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException {
		// First try the updated
		List<String> updatedIds = entityManager.aggregateEntityUpdate(userId, parentId, update);
		// Now create the update object
		List<T> newList = new ArrayList<T>();
		update.iterator().next();
		for(int i=0; i<updatedIds.size(); i++){
			T updated = update.iterator().next();
			newList.add((T) entityManager.getEntity(userId, updatedIds.get(i), updated.getClass()));
		}
		return newList;
	}


}
