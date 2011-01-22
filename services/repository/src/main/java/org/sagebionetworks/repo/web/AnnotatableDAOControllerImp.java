package org.sagebionetworks.repo.web;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AnnotatableDAO;
import org.sagebionetworks.repo.model.AnnotationDAO;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.Base;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.sagebionetworks.repo.web.controller.AbstractAnnotatableEntityController;
import org.sagebionetworks.repo.web.controller.AbstractEntityController;

/**
 * Implementation for REST controller for CRUD operations on Annotation DTOs and DAOs<p>
 * 
 * This class performs the basic CRUD operations for all our DAO-backed annotatable model 
 * objects.  See controllers specific to particular models for any special handling.<p>
 * 
 * TODO this patient is still lying open on the operating table, don't CR it yet
 * 
 * @author deflaux
 * @param <T> 
 */
public class AnnotatableDAOControllerImp<T extends Base> implements AbstractAnnotatableEntityController<T> {

    private static final Logger log = Logger.getLogger(AnnotatableDAOControllerImp.class.getName());
        
    protected Class<T> theModelClass;
    protected BaseDAO<T> dao;
    protected AnnotatableDAO<T> annotatableDao;

    /**
     * @param theModelClass
     */
    @SuppressWarnings("unchecked")
    public AnnotatableDAOControllerImp(Class<T> theModelClass) {
      this.theModelClass = theModelClass;
      // TODO @Autowired, no GAE references allowed in this class
      DAOFactory daoFactory = new GAEJDODAOFactoryImpl();
      this.dao = daoFactory.getDAO(theModelClass);
      this.annotatableDao = (AnnotatableDAO<T>) daoFactory.getDAO(theModelClass);
    }
    

    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractAnnotatableEntityController#getEntity(java.lang.String)
     */
    public Annotations getEntityAnnotations(String id, HttpServletRequest request) throws NotFoundException, DatastoreException {

        String entityId = getEntityIdFromUriId(id);

        Annotations annotations = annotatableDao.getAnnotations(entityId);
        if(null == annotations) {
            throw new NotFoundException("no entity with id " + entityId + " exists");
        }

        annotations.setUri(makeEntityUri(entityId, request));
        annotations.setEtag(makeEntityEtag(annotations));

        return annotations;
    }

    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractAnnotatableEntityController#updateEntity(java.lang.String, java.lang.Integer, Annotations)
     */
    public Annotations updateEntityAnnotations(String id, 
            Integer etag, 
            Annotations updatedAnnotations,
            HttpServletRequest request) throws NotFoundException, ConflictingUpdateException, DatastoreException {

        String entityId = getEntityIdFromUriId(id);
//
//        Annotations entity = dao.get(entityId);
//        if(null == entity) {
//            throw new NotFoundException("no entity with id " + entityId + " exists");
//        }
//        if(etag != entity.hashCode()) {
//            throw new ConflictingUpdateException("entity with id " + entityId 
//                    + "was updated since you last fetched it, retrieve it again and reapply the update");
//        }
//        dao.update(updatedAnnotations);
//

        // TODO this isn't how we want to do this for real
        // TODO is this additive or overwriting?

        Map<String, Collection<String>> stringAnnotations = updatedAnnotations.getStringAnnotations();
        AnnotationDAO<T, String> foo = annotatableDao.getStringAnnotationDAO();
        for(Map.Entry<String, Collection<String>> annotation : stringAnnotations.entrySet()) {
            for(String value : annotation.getValue()) {
                foo.addAnnotation(entityId, annotation.getKey(), value);
            }
        }

        Map<String, Collection<Float>> floatAnnotations = updatedAnnotations.getFloatAnnotations();
        AnnotationDAO<T, Float> bar = annotatableDao.getFloatAnnotationDAO();
        for(Map.Entry<String, Collection<Float>> annotation : floatAnnotations.entrySet()) {
            for(Float value : annotation.getValue()) {
                bar.addAnnotation(entityId, annotation.getKey(), value);
            }
        }
        
        Map<String, Collection<Date>> dateAnnotations = updatedAnnotations.getDateAnnotations();
        AnnotationDAO<T, Date> baz = annotatableDao.getDateAnnotationDAO();
        for(Map.Entry<String, Collection<Date>> annotation : dateAnnotations.entrySet()) {
            for(Date value : annotation.getValue()) {
                baz.addAnnotation(entityId, annotation.getKey(), value);
            }
        }
        
        updatedAnnotations.setUri(makeEntityUri(entityId, request));
        updatedAnnotations.setEtag(makeEntityEtag(updatedAnnotations));
        
        return updatedAnnotations;
    }
    
    /**
     * Helper function to translate ids found in URLs to ids used by the system<p>
     * 
     * Specifically we currently use the serialized system id url-encoded for use in URLs
     * @param id
     * @return
     */
    protected String getEntityIdFromUriId(String id) {
        String entityId = null;
        try {
            entityId = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, "Something is really messed up if we don't support UTF-8", e);
        }
        return entityId;
    }
    
    /**
     * Helper function to create a relative URL for an entity<p>
     * 
     * This includes not only the entity id but also the controller and servlet
     * portions of the path
     * 
     * TODO this code is duplicated
     * 
     * @param entity
     * @param request
     * @return
     */
    protected String makeEntityUri(String entityId, HttpServletRequest request) {
        String uri = null;
        try {
            uri = request.getServletPath() 
            + UrlPrefixes.getUrlForModel(theModelClass)
            + "/"
            + URLEncoder.encode(entityId, "UTF-8")
            + "/annotations";
        } 
        catch (UnsupportedEncodingException e) {
            log.log(Level.SEVERE, "Something is really messed up if we don't support UTF-8", e);
        }
        return uri;
    }
    
    /**
     * Helper function to create values for using in etags for an entity<p>
     * 
     * The current implementation uses hash code since different versions of our model
     * objects will have different hash code values
     * 
     * @param entity
     * @return
     */
    protected String makeEntityEtag(Annotations annotations) {
        Integer hashCode = annotations.hashCode();
        return hashCode.toString();
    }

}
