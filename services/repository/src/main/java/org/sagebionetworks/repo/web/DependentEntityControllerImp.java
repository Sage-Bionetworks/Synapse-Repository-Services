package org.sagebionetworks.repo.web;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DependentPropertyDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.util.SchemaHelper;

/**
 * Implementation of the controller interface for all operations common to
 * dependent entities.
 * <p>
 * 
 * This class performs the basic RU operations for all our dependent DAO-backed
 * model objects. See controllers specific to particular models for any special
 * handling.
 * 
 * @author deflaux
 * @param <T>
 *            the dependent DTO
 * @param <S>
 *            the parent DTO
 */
public class DependentEntityControllerImp<T extends Base, S> implements
		DependentEntityController<T, S> {

	private Class<T> theModelClass;
	private DependentPropertyDAO<T, S> dao;

	/**
	 * @param theModelClass
	 */
	public DependentEntityControllerImp(Class<T> theModelClass) {
		this.theModelClass = theModelClass;
	}

	@Override
	public void setDao(DependentPropertyDAO<T, S> dao) {
		this.dao = dao;

	}

	@Override
	public T getDependentEntity(String userId, String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {
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
	public T updateDependentEntity(String userId, String id, Integer etag,
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
	public JsonSchema getDependentEntitySchema() throws DatastoreException {
		return SchemaHelper.getSchema(theModelClass);
	}

	private void addServiceSpecificMetadata(T entity, HttpServletRequest request) {
		entity.setUri(UrlHelpers.makeEntityPropertyUri(request));
		entity.setEtag(UrlHelpers.makeEntityEtag(entity));
	}

}
