package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.web.AnnotatableEntitiesAccessorImpl;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.EntitiesAccessor;
import org.sagebionetworks.repo.web.EntityControllerImp;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
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
 * REST controller for CRUD operations on Dataset objects
 * <p>
 * Note that any controller logic common to all objects belongs in the
 * implementation of {@link EntityController} and of
 * {@link AnnotationsController} that this wraps. Only functionality specific to
 * Dataset objects belongs in this controller.
 * 
 * @author deflaux
 */
@Controller
public class DatasetController extends BaseController implements
		EntityController<Dataset> {

	private EntitiesAccessor<Dataset> datasetAccessor;
	private EntityController<Dataset> datasetController;

	// TODO @Autowired, no GAE references allowed in this class
	private static final DAOFactory DAO_FACTORY = new GAEJDODAOFactoryImpl();
	private DatasetDAO datasetDao; // = DAO_FACTORY.getDatasetDAO();

	DatasetController() {

		datasetAccessor = new AnnotatableEntitiesAccessorImpl<Dataset>();

		datasetController = new EntityControllerImp<Dataset>(Dataset.class,
				datasetAccessor);

		//setDao(datasetDao); // TODO remove this when @Autowired
	}

	@Override
	public void setDao(BaseDAO<Dataset> dao) {
		datasetDao = (DatasetDAO) dao;
		datasetAccessor.setDao(datasetDao);
		datasetController.setDao(datasetDao);
	}

	/*******************************************************************************
	 * Dataset CRUD handlers
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.EntityController#createEntity (T)
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DATASET, method = RequestMethod.POST)
	public @ResponseBody
	Dataset createEntity(@RequestBody Dataset newEntity, @RequestParam(value="userId", required=false) String userId, 
			HttpServletRequest request) throws DatastoreException,
			InvalidModelException, UnauthorizedException {

		setDao(DAO_FACTORY.getDatasetDAO(userId));
		Dataset dataset = datasetController.createEntity(newEntity, userId, request);

		addServiceSpecificMetadata(dataset, request);

		return dataset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.web.controller.EntityController#getEntity
	 * (java.lang.String)
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{id}", method = RequestMethod.GET)
	public @ResponseBody
	Dataset getEntity(@PathVariable String id,  @RequestParam(value="userId", required=false) String userId, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {
		setDao(DAO_FACTORY.getDatasetDAO(userId));

		Dataset dataset = datasetController.getEntity(id, userId, request);

		addServiceSpecificMetadata(dataset, request);

		return dataset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.EntityController#updateEntity
	 * (java.lang.String, java.lang.Integer, T)
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{id}", method = RequestMethod.PUT)
	public @ResponseBody
	Dataset updateEntity(@PathVariable String id,
			@RequestParam(value="userId", required=false) String userId, 
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody Dataset updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {
		setDao(DAO_FACTORY.getDatasetDAO(userId));

		Dataset dataset = datasetController.updateEntity(id, userId, etag,
				updatedEntity, request);

		addServiceSpecificMetadata(dataset, request);

		return dataset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.EntityController#deleteEntity
	 * (java.lang.String)
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.DATASET + "/{id}", method = RequestMethod.DELETE)
	public void deleteEntity(@PathVariable String id, @RequestParam(value="userId", required=false) String userId) throws NotFoundException,
			DatastoreException, UnauthorizedException {
		setDao(DAO_FACTORY.getDatasetDAO(userId));
		datasetController.deleteEntity(id, userId);
		return;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sagebionetworks.repo.web.controller.EntityController#getEntities
	 * (java.lang.Integer, java.lang.Integer,
	 * javax.servlet.http.HttpServletRequest)
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Dataset> getEntities(@RequestParam(value="userId", required=false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException {

		setDao(DAO_FACTORY.getDatasetDAO(userId));
		PaginatedResults<Dataset> results = datasetController.getEntities(userId,
				offset, limit, sort, ascending, request);

		for (Dataset dataset : results.getResults()) {
			addServiceSpecificMetadata(dataset, request);
		}

		return results;
	}

	/**
	 * Simple sanity check test request, using the default view
	 * <p>
	 * 
	 * @param modelMap
	 *            the parameter into which output data is to be stored
	 * @return a dummy hard-coded response
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/test", method = RequestMethod.GET)
	public String sanityCheck(ModelMap modelMap) {
		modelMap.put("hello", "REST for Datasets rocks");
		return ""; // use the default view
	}

	private void addServiceSpecificMetadata(Dataset dataset,
			HttpServletRequest request) throws DatastoreException {

		dataset.setAnnotations(UrlHelpers.makeEntityPropertyUri(dataset,
				Annotations.class, request));

		dataset.setLayer(UrlHelpers.makeEntityUri(dataset, request) + "/layer");

		return;
	}
}
