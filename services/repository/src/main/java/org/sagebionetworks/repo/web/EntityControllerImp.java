package org.sagebionetworks.repo.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;

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
public class EntityControllerImp<T extends Base> implements EntityController<T> {

	private Class<T> theModelClass;
	private BaseDAO<T> dao;
	private EntitiesAccessor<T> entitiesAccessor;

	/**
	 * @param theModelClass
	 * @param entitiesAccessor
	 */
	public EntityControllerImp(Class<T> theModelClass,
			EntitiesAccessor<T> entitiesAccessor) {
		this.theModelClass = theModelClass;
		this.entitiesAccessor = entitiesAccessor;
	}

	/**
	 * @return the theModelClass
	 */
	public Class<T> getTheModelClass() {
		return theModelClass;
	}

	/**
	 * @param theModelClass
	 *            the theModelClass to set
	 */
	public void setTheModelClass(Class<T> theModelClass) {
		this.theModelClass = theModelClass;
	}

	/**
	 * @return the dao
	 */
	public BaseDAO<T> getDao() {
		return dao;
	}

	/**
	 * @param dao
	 *            the dao to set
	 */
	public void setDao(BaseDAO<T> dao) {
		this.dao = dao;
	}

	/**
	 * @return the entitiesAccessor
	 */
	public EntitiesAccessor<T> getEntitiesAccessor() {
		return entitiesAccessor;
	}

	/**
	 * @param entitiesAccessor
	 *            the entitiesAccessor to set
	 */
	public void setEntitiesAccessor(EntitiesAccessor<T> entitiesAccessor) {
		this.entitiesAccessor = entitiesAccessor;
	}

	@Override
	public PaginatedResults<T> getEntities(String userId, Integer offset,
			Integer limit, String sort, Boolean ascending,
			HttpServletRequest request) throws DatastoreException {

		ServiceConstants.validatePaginationParams(offset, limit);

		List<T> entities = entitiesAccessor.getInRangeSortedBy(offset, limit,
				sort, ascending);

		for (T entity : entities) {
			addServiceSpecificMetadata(entity, request);
		}

		Integer totalNumberOfEntities = dao.getCount();

		return new PaginatedResults<T>(request.getServletPath()
				+ UrlHelpers.getUrlForModel(theModelClass), entities,
				totalNumberOfEntities, offset, limit, sort, ascending);
	}

	@Override
	public T getEntity(String userId, String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		T entity = dao.get(entityId);
		if (null == entity) {
			throw new NotFoundException("no entity with id " + entityId
					+ " exists");
		}

		addServiceSpecificMetadata(entity, request);

		return entity;
	}

	@Override
	public T createEntity(String userId, T newEntity, HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException {

		dao.create(newEntity);

		addServiceSpecificMetadata(newEntity, request);

		return newEntity;
	}

	@Override
	public T updateEntity(String userId, String id, Integer etag,
			T updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {

		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		T entity = dao.get(entityId);
		if (null == entity) {
			throw new NotFoundException("no entity with id " + entityId
					+ " exists");
		}
		if (etag != entity.hashCode()) {
			throw new ConflictingUpdateException(
					"entity with id "
							+ entityId
							+ " was updated since you last fetched it, retrieve it again and reapply the update");
		}
		dao.update(updatedEntity);

		addServiceSpecificMetadata(updatedEntity, request);

		return updatedEntity;
	}

	@Override
	public void deleteEntity(String userId, String id)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		dao.delete(entityId);

		return;
	}

	private void addServiceSpecificMetadata(T entity, HttpServletRequest request) {
		entity.setUri(UrlHelpers.makeEntityUri(entity, request));
		entity.setEtag(UrlHelpers.makeEntityEtag(entity));
	}

}
