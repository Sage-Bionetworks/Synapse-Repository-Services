package org.sagebionetworks.repo.web;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.sagebionetworks.repo.web.controller.AbstractEntityController;

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
 */
public class DAOControllerImp<T extends Base> implements
		AbstractEntityController<T> {

	private static final Logger log = Logger.getLogger(DAOControllerImp.class
			.getName());

	protected Class<T> theModelClass;
	protected BaseDAO<T> dao;
	protected EntitiesAccessor<T> entitiesAccessor;
	
	/**
	 * @param theModelClass
	 */
	@SuppressWarnings("unchecked")
	public DAOControllerImp(Class<T> theModelClass, EntitiesAccessor<T> entitiesAccessor) {
		this.theModelClass = theModelClass;
		this.entitiesAccessor = entitiesAccessor;
		// TODO @Autowired, no GAE references allowed in this class
		DAOFactory daoFactory = new GAEJDODAOFactoryImpl();
		this.dao = daoFactory.getDAO(theModelClass);
	}
	

	/**
	 * @return the theModelClass
	 */
	public Class<T> getTheModelClass() {
		return theModelClass;
	}


	/**
	 * @param theModelClass the theModelClass to set
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
	 * @param dao the dao to set
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
	 * @param entitiesAccessor the entitiesAccessor to set
	 */
	public void setEntitiesAccessor(EntitiesAccessor<T> entitiesAccessor) {
		this.entitiesAccessor = entitiesAccessor;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#getEntities
	 */
	public PaginatedResults<T> getEntities(Integer offset, Integer limit,
			String sort, Boolean ascending, HttpServletRequest request) throws DatastoreException {

		ServiceConstants.validatePaginationParams(offset, limit);

		List<T> entities = entitiesAccessor.getInRangeSortedBy(offset, limit, sort, ascending);

		for(T entity : entities) {
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
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#getEntity
	 * (java.lang.String)
	 */
	public T getEntity(String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException {

		T entity = dao.get(id);
		if (null == entity) {
			throw new NotFoundException("no entity with id " + id + " exists");
		}

		addServiceSpecificMetadata(entity, request);

		return entity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#createEntity
	 * (T)
	 */
	public T createEntity(T newEntity, HttpServletRequest request)
			throws DatastoreException, InvalidModelException {

		dao.create(newEntity);

		addServiceSpecificMetadata(newEntity, request);

		return newEntity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#updateEntity
	 * (java.lang.String, java.lang.Integer, T)
	 */
	public T updateEntity(String id, Integer etag, T updatedEntity,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException, InvalidModelException {

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
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#deleteEntity
	 * (java.lang.String)
	 */
	public void deleteEntity(String id) throws NotFoundException,
			DatastoreException {
		String entityId = UrlHelpers.getEntityIdFromUriId(id);

		dao.delete(entityId);

		return;
	}
	
	private void addServiceSpecificMetadata(T entity, HttpServletRequest request) {
		entity.setUri(UrlHelpers.makeEntityUri(entity, request));
		entity.setEtag(UrlHelpers.makeEntityEtag(entity));
	}

}
