package org.sagebionetworks.repo.web.controller;

import java.util.Collection;
import java.util.HashSet;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayerDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerLocation;
import org.sagebionetworks.repo.model.LayerLocations;
import org.sagebionetworks.repo.model.LayerLocationsDAO;
import org.sagebionetworks.repo.model.LayerPreview;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.web.AnnotatableEntitiesAccessorImpl;
import org.sagebionetworks.repo.web.AnnotationsControllerImp;
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
 * REST controller for CRUD operations on Dataset Layer objects
 * <p>
 * 
 * Note that any controller logic common to all objects belongs in the
 * implementation of {@link EntityController} and of
 * {@link AnnotationsController} that this wraps. Only functionality specific to
 * Dataset objects belongs in this controller.
 * 
 * @author deflaux
 */
@Controller
public class LayerController extends BaseController {

	private EntitiesAccessor<InputDataLayer> layerAccessor;
	private EntityController<InputDataLayer> layerController;
	private AnnotationsController<InputDataLayer> layerAnnotationsController;

	// TODO @Autowired, no GAE references allowed in this class
	private static final DAOFactory DAO_FACTORY = new GAEJDODAOFactoryImpl();
	private DatasetDAO datasetDao = null; //DAO_FACTORY.getDatasetDAO();
	private LayerLocationsDAO locationsDao = null;
	
	private void setDao(String userId) {
		datasetDao=DAO_FACTORY.getDatasetDAO(userId);
		locationsDao = DAO_FACTORY.getLayerLocationsDAO(userId);
	}

	LayerController() {
		layerAccessor = new AnnotatableEntitiesAccessorImpl<InputDataLayer>();

		layerController = new EntityControllerImp<InputDataLayer>(
				InputDataLayer.class, layerAccessor);

		layerAnnotationsController = new AnnotationsControllerImp<InputDataLayer>();

	}

	/*******************************************************************************
	 * Layers CRUD handlers
	 * 
	 * note that I can't put these in a different file because the Spring
	 * dispatcher can't handle multiple controllers with the same url prefix
	 * 
	 */

	/**
	 * @param parentId
	 * @param newEntity
	 * @param request
	 * @return the newly created layer
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER, method = RequestMethod.POST)
	public @ResponseBody
	InputDataLayer createChildEntity(@PathVariable String parentId,
			@RequestParam(value="userId", required=false) String userId,
			@RequestBody InputDataLayer newEntity, HttpServletRequest request)
			throws DatastoreException, InvalidModelException, UnauthorizedException {
		
		setDao(userId);

		String datasetId = UrlHelpers.getEntityIdFromUriId(parentId);

		InputDataLayerDAO layerDao = datasetDao.getInputDataLayerDAO(datasetId);

		layerController.setDao(layerDao);
		InputDataLayer datasetLayer = layerController.createEntity(newEntity, userId,
				request);

		addServiceSpecificMetadata(datasetLayer, request);

		return datasetLayer;
	}

	/**
	 * @param parentId
	 * @param id
	 * @param request
	 * @return the requested layer
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}", method = RequestMethod.GET)
	public @ResponseBody
	InputDataLayer getChildEntity(@PathVariable String parentId,
			@RequestParam(value="userId", required=false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		setDao(userId);

		String datasetId = UrlHelpers.getEntityIdFromUriId(parentId);

		InputDataLayerDAO layerDao = datasetDao.getInputDataLayerDAO(datasetId);

		layerController.setDao(layerDao);
		InputDataLayer datasetLayer = layerController.getEntity(id, userId, request);

		addServiceSpecificMetadata(datasetLayer, request);

		return datasetLayer;
	}

	/**
	 * @param parentId
	 * @param id
	 * @param etag
	 * @param updatedEntity
	 * @param request
	 * @return the updated layer
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws InvalidModelException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}", method = RequestMethod.PUT)
	public @ResponseBody
	InputDataLayer updateChildEntity(@PathVariable String parentId,
			@RequestParam(value="userId", required=false) String userId,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody InputDataLayer updatedEntity,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			InvalidModelException, UnauthorizedException {

		setDao(userId);

		String datasetId = UrlHelpers.getEntityIdFromUriId(parentId);

		InputDataLayerDAO layerDao = datasetDao.getInputDataLayerDAO(datasetId);

		layerController.setDao(layerDao);
		InputDataLayer datasetLayer = layerController.updateEntity(id, userId, etag,
				updatedEntity, request);

		addServiceSpecificMetadata(datasetLayer, request);

		return datasetLayer;
	}

	/**
	 * @param parentId
	 * @param id
	 * @throws NotFoundException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}", method = RequestMethod.DELETE)
	public void deleteChildEntity(@PathVariable String parentId,
			@RequestParam(value="userId", required=false) String userId,
			@PathVariable String id) throws NotFoundException,
			DatastoreException, UnauthorizedException {

		setDao(userId);

		String datasetId = UrlHelpers.getEntityIdFromUriId(parentId);

		InputDataLayerDAO layerDao = datasetDao.getInputDataLayerDAO(datasetId);

		layerController.setDao(layerDao);
		layerController.deleteEntity(id, userId);
		return;
	}

	/**
	 * @param parentId
	 * @param offset
	 * @param limit
	 * @param sort
	 * @param ascending
	 * @param request
	 * @return the layers
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<InputDataLayer> getChildEntities(
			@PathVariable String parentId,
			@RequestParam(value="userId", required=false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException {

		setDao(userId);

		String datasetId = UrlHelpers.getEntityIdFromUriId(parentId);

		InputDataLayerDAO layerDao = datasetDao.getInputDataLayerDAO(datasetId);

		layerController.setDao(layerDao);
		layerAccessor.setDao(layerDao);
		PaginatedResults<InputDataLayer> results = layerController.getEntities(userId,
				offset, limit, sort, ascending, request);

		for (InputDataLayer layer : results.getResults()) {
			addServiceSpecificMetadata(layer, request);
		}

		return results;
	}

	/*******************************************************************************
	 * Layer Annotation RUD handlers
	 */

	/**
	 * @param parentId
	 * @param id
	 * @param request
	 * @return the annotations for this layer
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.ANNOTATIONS, method = RequestMethod.GET)
	public @ResponseBody
	Annotations getChildEntityAnnotations(@PathVariable String parentId,
			@RequestParam(value="userId", required=false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		setDao(userId);

		String datasetId = UrlHelpers.getEntityIdFromUriId(parentId);

		InputDataLayerDAO layerDao = datasetDao.getInputDataLayerDAO(datasetId);

		layerAnnotationsController.setDao(layerDao);

		return layerAnnotationsController.getEntityAnnotations(id, userId, request);
	}

	/**
	 * @param parentId
	 * @param id
	 * @param etag
	 * @param updatedAnnotations
	 * @param request
	 * @return the updated annotations for this layer
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.ANNOTATIONS, method = RequestMethod.PUT)
	public @ResponseBody
	Annotations updateChildEntityAnnotations(@PathVariable String parentId,
			@RequestParam(value="userId", required=false) String userId,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody Annotations updatedAnnotations,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException, UnauthorizedException {

		setDao(userId);

		String datasetId = UrlHelpers.getEntityIdFromUriId(parentId);

		InputDataLayerDAO layerDao = datasetDao.getInputDataLayerDAO(datasetId);

		layerAnnotationsController.setDao(layerDao);

		return layerAnnotationsController.updateEntityAnnotations(id, userId, etag,
				updatedAnnotations, request);
	}

	/*******************************************************************************
	 * Helpers
	 */

	/**
	 * Simple sanity check test request, using the default view
	 * <p>
	 * 
	 * @param modelMap
	 *            the parameter into which output data is to be stored
	 * @return a dummy hard-coded response
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/test", method = RequestMethod.GET)
	public String sanityCheckChild(ModelMap modelMap) {
		modelMap.put("hello", "REST for Dataset Layers rocks");
		return ""; // use the default view
	}

	private void addServiceSpecificMetadata(InputDataLayer layer,
			HttpServletRequest request) throws DatastoreException, UnauthorizedException {

		layer.setAnnotations(UrlHelpers.makeEntityPropertyUri(layer,
				Annotations.class, request));
		layer.setPreview(UrlHelpers.makeEntityPropertyUri(layer,
				LayerPreview.class, request));

		//
		// Make URIs to get the additional metadata about locations
		//
		Collection<String> layerLocations = new HashSet<String>();

		// TODO only if user is a curator add the RU uri for locations
		layerLocations.add(UrlHelpers.makeEntityPropertyUri(layer,
				LayerLocations.class, request));

		LayerLocations locations;
		try {
			locations = locationsDao.get(layer.getId());
		} catch (NotFoundException e) {
			// This should not happen because we were just create/get/update
			// this layer
			throw new DatastoreException(e);
		}
		for (LayerLocation location : locations.getLocations()) {
			layerLocations.add(UrlHelpers.makeLocationUri(layer.getUri(),
					location.getType()));
		}
		layer.setLocations(layerLocations);

	}

}
