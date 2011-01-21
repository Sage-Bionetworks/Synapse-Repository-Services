package org.sagebionetworks.repo.web.controller;

import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.DAOControllerImp;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlPrefixes;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;


/**
 * REST controller for CRUD operations on Dataset objects<p>
 * 
 * Note that any controller logic common to all objects belongs in the implementation
 * of AbstractEntityController that this wraps.  Only functionality specific to Dataset
 * objects belongs in this controller.
 * 
 * @author deflaux
 */
@Controller
@RequestMapping(UrlPrefixes.DATASET)
public class DatasetController extends BaseController implements AbstractEntityController<Dataset> {

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(DatasetController.class.getName());
        
    private AbstractEntityController<Dataset> entityController = new DAOControllerImp<Dataset>(Dataset.class);
    
    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#getEntities(java.lang.Integer, java.lang.Integer, javax.servlet.http.HttpServletRequest)
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "", method = RequestMethod.GET)
    public @ResponseBody PaginatedResults<Dataset> getEntities(
            @RequestParam(value=ServiceConstants.PAGINATION_OFFSET_PARAM, 
                    required=false, 
                    defaultValue=ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
            @RequestParam(value=ServiceConstants.PAGINATION_LIMIT_PARAM, 
                    required=false, 
                    defaultValue=ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
                    HttpServletRequest request) throws DatastoreException {
        return entityController.getEntities(offset, limit, request);
    }

    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#getEntity(java.lang.String)
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
        public @ResponseBody Dataset getEntity(@PathVariable String id, HttpServletRequest request) throws NotFoundException, DatastoreException {
        return entityController.getEntity(id, request);
    }

    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#createEntity(T)
     */
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(value = "", method = RequestMethod.POST)
    public @ResponseBody Dataset createEntity(@RequestBody Dataset newEntity, HttpServletRequest request) throws DatastoreException, InvalidModelException {
        return entityController.createEntity(newEntity, request);
    }

    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#updateEntity(java.lang.String, java.lang.Integer, T)
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public @ResponseBody Dataset updateEntity(@PathVariable String id, 
            @RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag, 
            @RequestBody Dataset updatedEntity,
            HttpServletRequest request) throws NotFoundException, ConflictingUpdateException, DatastoreException {
        return entityController.updateEntity(id, etag, updatedEntity, request);
    }
    
    /* (non-Javadoc)
     * @see org.sagebionetworks.repo.web.controller.AbstractEntityController#deleteEntity(java.lang.String)
     */
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public void deleteEntity(@PathVariable String id) throws NotFoundException, DatastoreException {
        entityController.deleteEntity(id);
        return;
    }

    /**
     * Simple sanity check test request, using the default view<p> 
     * 
     * @param modelMap the parameter into which output data is to be stored
     * @return a dummy hard-coded response
     */
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(value = "test", method = RequestMethod.GET)
        public String sanityCheck(ModelMap modelMap) {
        modelMap.put("hello","REST for Datasets rocks");
        return ""; // use the default view
    }
        
}
