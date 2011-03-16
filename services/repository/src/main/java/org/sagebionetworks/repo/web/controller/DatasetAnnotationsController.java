package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatastoreException;
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
 * REST controller for RU operations on Dataset Annotation objects
 * <p>
 * 
 * Note that any controller logic common to all objects belongs in the
 * implementation of {@link AnnotationsController} that this wraps. Only
 * functionality specific to Dataset Annotations objects belongs in this
 * controller.
 * 
 * @author deflaux
 */
@Controller
public class DatasetAnnotationsController extends BaseController implements
		AnnotationsController<Dataset> {

	private AnnotationsController<Dataset> datasetAnnotationsController;

	DatasetAnnotationsController() {
		datasetAnnotationsController = new AnnotationsControllerImp<Dataset>();
	}

	private void checkAuthorization(String userId, Boolean readOnly) {
		BaseDAO<Dataset> dao = getDaoFactory().getDatasetDAO(userId);
		setDao(dao);
	}

	@Override
	public void setDao(BaseDAO<Dataset> dao) {
		datasetAnnotationsController.setDao(dao);
	}

	/*******************************************************************************
	 * Dataset Annotation RUD handlers
	 * 
	 */

	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{id}"
			+ UrlHelpers.ANNOTATIONS, method = RequestMethod.GET)
	public @ResponseBody
	Annotations getEntityAnnotations(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		checkAuthorization(userId, true);
		return datasetAnnotationsController.getEntityAnnotations(userId, id,
				request);
	}

	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{id}"
			+ UrlHelpers.ANNOTATIONS, method = RequestMethod.PUT)
	public @ResponseBody
	Annotations updateEntityAnnotations(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody Annotations updatedAnnotations,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException,
			UnauthorizedException {

		checkAuthorization(userId, false);
		return datasetAnnotationsController.updateEntityAnnotations(userId, id,
				etag, updatedAnnotations, request);
	}

	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{id}"
			+ UrlHelpers.ANNOTATIONS + UrlHelpers.SCHEMA, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getEntityAnnotationsSchema() throws DatastoreException {
		return datasetAnnotationsController.getEntityAnnotationsSchema();
	}

}
