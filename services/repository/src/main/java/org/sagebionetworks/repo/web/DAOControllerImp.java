package org.sagebionetworks.repo.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.logging.Level;
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
public class DAOControllerImp<T extends Base> implements AbstractEntityController<T> {

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
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public T getEntity(String id, HttpServletRequest request) throws NotFoundException, DatastoreException {
        BaseDAO<T> dao = daoFactory.getDAO(theModelClass);
        if(null == dao) {
            throw new DatastoreException("The datastore is not correctly configured to store objects of type " 
                    + theModelClass);
        }
        T entity = dao.get(id);
        if(null == entity) {
            throw new NotFoundException("no entity with id " + id + " exists");
        }
        entity.setUri(makeEntityUri(entity, request));
        entity.setEtag(makeEntityEtag(entity));
        return entity;
    }

    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#createEntity(T)
     */
    @SuppressWarnings("unchecked")
    public T createEntity(T newEntity, HttpServletRequest request) throws DatastoreException, InvalidModelException {
        BaseDAO<T> dao = daoFactory.getDAO(theModelClass);
        if(null == dao) {
            throw new DatastoreException("The datastore is not correctly configured to store objects of type " 
                    + newEntity.getClass());
        }
        dao.create(newEntity);
        newEntity.setUri(makeEntityUri(newEntity, request));
        newEntity.setEtag(makeEntityEtag(newEntity));
        return newEntity;
    }

    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#updateEntity(java.lang.String, java.lang.Integer, T)
     */
    @SuppressWarnings("unchecked")
    public T updateEntity(String id, 
            Integer etag, 
            T updatedEntity,
            HttpServletRequest request) throws NotFoundException, ConflictingUpdateException, DatastoreException {
        BaseDAO<T> dao = daoFactory.getDAO(theModelClass);
        if(null == dao) {
            throw new DatastoreException("The datastore is not correctly configured to store objects of type " 
                    + updatedEntity.getClass());
        }
        String entityId = null;
        try {
            entityId = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        T entity = dao.get(entityId);
        if(null == entity) {
            throw new NotFoundException("no entity with id " + entityId + " exists");
        }
        if(etag != entity.hashCode()) {
            throw new ConflictingUpdateException("entity with id " + entityId 
                    + "was updated since you last fetched it, retrieve it again and reapply the update");
        }
        dao.update(updatedEntity);
        updatedEntity.setUri(makeEntityUri(updatedEntity, request));
        updatedEntity.setEtag(makeEntityEtag(updatedEntity));
        return updatedEntity;
    }
    
    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#deleteEntity(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    public void deleteEntity(String id) throws NotFoundException, DatastoreException {
        BaseDAO<T> dao = daoFactory.getDAO(theModelClass);
        if(null == dao) {
            throw new DatastoreException("The datastore is not correctly configured to store objects of type " 
                    + theModelClass);
        }
        String entityId = null;
        try {
            entityId = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        dao.delete(entityId);
        return;
    }

    private String makeEntityUri(T entity, HttpServletRequest request) {
        String uri = null;
        try {
            uri = request.getServletPath() 
            + UrlPrefixes.getUrlForModel(theModelClass)
            + "/"
            + URLEncoder.encode(entity.getId(), "UTF-8");
        } 
        catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, "Something is really messed up if we don't support UTF-8", e);
        }
        return uri;
    }
    
    private String makeEntityEtag(T entity) {
        Integer hashCode = entity.hashCode();
        return hashCode.toString();
    }
}
