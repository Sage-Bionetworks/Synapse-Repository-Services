package org.sagebionetworks.repo.web.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayerDAO;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerMetadata;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
import org.sagebionetworks.repo.view.PaginatedResults;
import org.sagebionetworks.repo.web.AnnotatableDAOControllerImp;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.DAOControllerImp;
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
 * 
 * Note that any controller logic common to all objects belongs in the
 * implementation of {@link AbstractEntityController} and of
 * {@link AbstractAnnotatableEntityController} that this wraps. Only
 * functionality specific to Dataset objects belongs in this controller.
 * 
 * @author deflaux
 */
@Controller
@RequestMapping(UrlHelpers.DATASET)
public class DatasetController extends BaseController implements
		AbstractEntityController<Dataset>,
		AbstractAnnotatableEntityController<Dataset> {

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(DatasetController.class
			.getName());

	private AbstractEntityController<Dataset> entityController = new DAOControllerImp<Dataset>(
			Dataset.class);
	private AbstractAnnotatableEntityController<Dataset> annotationsController = new AnnotatableDAOControllerImp<Dataset>(
			Dataset.class);
	private InputDataLayerDAO layerDao;

	DatasetController() {
		// TODO @Autowired, no GAE references allowed in this class
		DAOFactory daoFactory = new GAEJDODAOFactoryImpl();
		this.layerDao = daoFactory.getInputDataLayerDAO();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#getEntities
	 * (java.lang.Integer, java.lang.Integer,
	 * javax.servlet.http.HttpServletRequest)
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "", method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<Dataset> getEntities(
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			HttpServletRequest request) throws DatastoreException {
		
		PaginatedResults<Dataset> results = entityController.getEntities(offset, limit, request);
		
		for(Dataset dataset : results.getResults()) {
			addServiceSpecificMetadata(dataset, request);
		}
		
		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#getEntity
	 * (java.lang.String)
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public @ResponseBody
	Dataset getEntity(@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException {

		Dataset dataset = entityController.getEntity(id, request);

		addServiceSpecificMetadata(dataset, request);

		return dataset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#createEntity
	 * (T)
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = "", method = RequestMethod.POST)
	public @ResponseBody
	Dataset createEntity(@RequestBody Dataset newEntity,
			HttpServletRequest request) throws DatastoreException,
			InvalidModelException {

		Dataset dataset = entityController.createEntity(newEntity, request);

		addServiceSpecificMetadata(dataset, request);

		return dataset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#updateEntity
	 * (java.lang.String, java.lang.Integer, T)
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public @ResponseBody
	Dataset updateEntity(@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody Dataset updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException {

		Dataset dataset = entityController.updateEntity(id, etag,
				updatedEntity, request);

		addServiceSpecificMetadata(dataset, request);

		return dataset;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.sagebionetworks.repo.web.controller.AbstractEntityController#deleteEntity
	 * (java.lang.String)
	 */
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public void deleteEntity(@PathVariable String id) throws NotFoundException,
			DatastoreException {
		entityController.deleteEntity(id);
		return;
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
	@RequestMapping(value = "test", method = RequestMethod.GET)
	public String sanityCheck(ModelMap modelMap) {
		modelMap.put("hello", "REST for Datasets rocks");
		return ""; // use the default view
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}/annotations", method = RequestMethod.GET)
	public @ResponseBody
	Annotations getEntityAnnotations(@PathVariable String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException {
		return annotationsController.getEntityAnnotations(id, request);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = "/{id}/annotations", method = RequestMethod.PUT)
	public @ResponseBody
	Annotations updateEntityAnnotations(@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody Annotations updatedAnnotations,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException {
		return annotationsController.updateEntityAnnotations(id, etag,
				updatedAnnotations, request);
	}

	private void addServiceSpecificMetadata(Dataset dataset,
			HttpServletRequest request) {

		dataset.setAnnotations(UrlHelpers.makeEntityAnnotationsUri(dataset,
				request));

		// Layers have not yet been implemented
		// Collection<String> layers = dataset.getLayers();
		// for(String layer : layers) {
		// LayerDTO layerDto = layerDao.getLayer(layer);
		// layerUris.put(layerDto.getType,
		// UrlHelpers.makeEntityUri(layerDto, request));
		// }

		// TODO inserting fake data for now, fix me!
		if (0 == dataset.getLayers().size()) {
			Collection<LayerMetadata> layers = new ArrayList<LayerMetadata>();

			layers.add(new LayerMetadata("agxkZWZsYXV4LXRlc3RyEwsSDUdBRUpET0RhdGFzZXQYAQw",
					"C",
					"/datalayer/agxkZWZsYXV4LXRlc3RyEwsSDUdBRUpET0RhdGFzZXQYAQw"));
			layers.add(new LayerMetadata("agxkZWZsYXV4LXRlc3RyFQsSDUdBRUpET0RhdGFzZXQYiaECDA",
					"E",
					"agxkZWZsYXV4LXRlc3RyFQsSDUdBRUpET0RhdGFzZXQYiaECDA"));
			layers.add(new LayerMetadata("agxkZWZsYXV4LXRlc3RyFQsSDUdBRUpET0RhdGFzZXQYmfIBDA",
					"G",
					"/datalayer/agxkZWZsYXV4LXRlc3RyFQsSDUdBRUpET0RhdGFzZXQYmfIBDA"));
			dataset.setLayers(layers);
		}

		return;
	}
}
