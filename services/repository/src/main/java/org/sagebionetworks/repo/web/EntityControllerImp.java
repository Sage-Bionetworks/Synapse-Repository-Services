package org.sagebionetworks.repo.web;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.controller.EntityController;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.web.controller.EntityController#getEntities
	 */
	public PaginatedResults<T> getEntities(String userId, Integer offset, Integer limit,
			String sort, Boolean ascending, HttpServletRequest request)
			throws DatastoreException {

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.web.controller.EntityController#getEntity
	 * (java.lang.String)
	 */
	public T getEntity(String id, String userId, HttpServletRequest request)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.EntityController#createEntity (T)
	 */
	public T createEntity(T newEntity, String userId, HttpServletRequest request)
			throws DatastoreException, InvalidModelException , UnauthorizedException {

		dao.create(newEntity);

		addServiceSpecificMetadata(newEntity, request);

		return newEntity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.EntityController#updateEntity
	 * (java.lang.String, java.lang.Integer, T)
	 */
	public T updateEntity(String id, String userId, Integer etag, T updatedEntity,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException  {

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.EntityController#deleteEntity
	 * (java.lang.String)
	 */
	public void deleteEntity(String id, String userId) throws NotFoundException,
			DatastoreException, UnauthorizedException  {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		dao.delete(entityId);

		return;
	}

	private void addServiceSpecificMetadata(T entity, HttpServletRequest request) {
		entity.setUri(UrlHelpers.makeEntityUri(entity, request));
		entity.setEtag(UrlHelpers.makeEntityEtag(entity));
	}

}
