package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InputDataLayerDAO;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.AnnotationsController;
import org.sagebionetworks.repo.web.AnnotationsControllerImp;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.ServiceConstants;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * REST controller for RUD operations on Layer Annotations
 * <p>
 * 
 * Note that any controller logic common to all objects belongs in the
 * implementation of {@link AnnotationsController} that this wraps. Only
 * functionality specific to Layer Annotations objects belongs in this
 * controller.
 * 
 * @author deflaux
 */
@Controller
public class LayerAnnotationsController extends BaseController { // TODO
	// implements
	// AnnotationsController

	private AnnotationsController<InputDataLayer> layerAnnotationsController;

	private DatasetDAO datasetDao = null; // DAO_FACTORY.getDatasetDAO();

	private void setDao(String userId) {
		datasetDao = getDaoFactory().getDatasetDAO(userId);
	}

	LayerAnnotationsController() {
		layerAnnotationsController = new AnnotationsControllerImp<InputDataLayer>();

	}

	/*******************************************************************************
	 * Layer Annotation RUD handlers
	 */

	/**
	 * @param userId
	 * @param parentId
	 * @param id
	 * @param request
	 * @return the annotations for this layer
	 * @throws NotFoundException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 * 
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.ANNOTATIONS, method = RequestMethod.GET)
	public @ResponseBody
	Annotations getChildEntityAnnotations(
			@RequestParam(value = ServiceConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String parentId, @PathVariable String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException, UnauthorizedException {

		setDao(userId);

		String datasetId = UrlHelpers.getEntityIdFromUriId(parentId);

		InputDataLayerDAO layerDao = datasetDao.getInputDataLayerDAO(datasetId);

		layerAnnotationsController.setDao(layerDao);

		return layerAnnotationsController.getEntityAnnotations(userId, id,
				request);
	}

	/**
	 * @param userId
	 * @param parentId
	 * @param id
	 * @param etag
	 * @param updatedAnnotations
	 * @param request
	 * @return the updated annotations for this layer
	 * @throws NotFoundException
	 * @throws ConflictingUpdateException
	 * @throws DatastoreException
	 * @throws UnauthorizedException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.ANNOTATIONS, method = RequestMethod.PUT)
	public @ResponseBody
	Annotations updateChildEntityAnnotations(
			@RequestParam(value = ServiceConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String parentId, @PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody Annotations updatedAnnotations,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException {

		setDao(userId);

		String datasetId = UrlHelpers.getEntityIdFromUriId(parentId);

		InputDataLayerDAO layerDao = datasetDao.getInputDataLayerDAO(datasetId);

		layerAnnotationsController.setDao(layerDao);

		return layerAnnotationsController.updateEntityAnnotations(userId, id,
				etag, updatedAnnotations, request);
	}

}
