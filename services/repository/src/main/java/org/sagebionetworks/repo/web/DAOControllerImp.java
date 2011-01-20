package org.sagebionetworks.repo.web;

import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.sagebionetworks.repo.web.controller.AbstractEntityController;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation for REST controller for CRUD operations on Entity objects<p>
 * 
 * TODO this patient is lying open on the surgery table, don't bother CR-ing this yet<p>
 * 
 * This class performs the basic CRUD operations for all our DAO-backed model 
 * objects.  See controllers specific to particular models for any special handling.
 * 
 * TODO still actively working on this class, only the create method has tests 
 * 
 * @author deflaux
 * @param <T> 
 */
public class DAOControllerImp<T> implements AbstractEntityController<T> {

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(DAOControllerImp.class.getName());
        
    private Class<T> theModelClass;
    
    // TODO @Autowired, no GAE references allowed in this class
    private DAOFactory daoFactory = new GAEJDODAOFactoryImpl();

    /**
     * @param theModelClass
     */
    public DAOControllerImp(Class<T> theModelClass) {
      this.theModelClass = theModelClass;
    }
    
    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#getEntities(java.lang.Integer, java.lang.Integer, javax.servlet.http.HttpServletRequest)
     */
    public PaginatedResults<T> getEntities(Integer offset,
            Integer limit,
            HttpServletRequest request) throws DatastoreException {
        
        ServiceConstants.validatePaginationParams(offset, limit);
        BaseDAO<T> dao = daoFactory.getDAO(theModelClass);
        if(null == dao) {
            throw new DatastoreException("The datastore is not correctly configured to store objects of type " 
                    + theModelClass);
        }
        
        List<T> entities = dao.getInRange(offset, offset + limit - 1);
        Integer totalNumberOfEntities = dao.getCount();
        return new PaginatedResults<T>(request.getServletPath() + UrlPrefixes.getUrlForModel(theModelClass),
                entities, totalNumberOfEntities, offset, limit);
    }

    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#getEntity(java.lang.String)
     */
    public T getEntity(String id) throws NotFoundException, DatastoreException {
        BaseDAO<T> dao = daoFactory.getDAO(theModelClass);
        if(null == dao) {
            throw new DatastoreException("The datastore is not correctly configured to store objects of type " 
                    + theModelClass);
        }
        T entity = dao.get(id);
        if(null == entity) {
            throw new NotFoundException("no entity with id " + id + " exists");
        }
        return entity;
    }

    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#createEntity(T)
     */
    @SuppressWarnings("unchecked")
    public T createEntity(T newEntity) throws DatastoreException, InvalidModelException {
        BaseDAO<T> dao = daoFactory.getDAO(newEntity.getClass());
        if(null == dao) {
            throw new DatastoreException("The datastore is not correctly configured to store objects of type " 
                    + newEntity.getClass());
        }
        dao.create(newEntity);
        // TODO set ref or location property on DTO
        return newEntity;
    }

    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#updateEntity(java.lang.String, java.lang.Integer, T)
     */
    public T updateEntity(String id, 
            Integer etag, 
            T updatedEntity) throws NotFoundException, ConflictingUpdateException, DatastoreException {
        BaseDAO<T> dao = daoFactory.getDAO(updatedEntity.getClass());
        if(null == dao) {
            throw new DatastoreException("The datastore is not correctly configured to store objects of type " 
                    + updatedEntity.getClass());
        }
        T entity = dao.get(id);
        if(null == entity) {
            throw new NotFoundException("no entity with id " + id + " exists");
        }
        if(etag != entity.hashCode()) {
            throw new ConflictingUpdateException("entity with id " + id 
                    + "was updated since you last fetched it, retrieve it again and reapply the update");
        }
        dao.update(updatedEntity);
        return updatedEntity;
    }
    
    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#deleteEntity(java.lang.String)
     */
    public void deleteEntity(String id) throws NotFoundException {
//        if(!entityRepository.deleteById(id)) {
//            throw new NotFoundException("no entity with id " + id + " exists");   
//        }
        return;
    }
}
