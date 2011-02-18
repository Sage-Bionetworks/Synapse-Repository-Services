package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.Annotations;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DAOFactory;
import org.sagebionetworks.repo.model.Dataset;
import org.sagebionetworks.repo.model.DatasetDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.gaejdo.GAEJDODAOFactoryImpl;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * REST controller for CRUD operations on Dataset Annotation objects
 * <p>
 * 
 * Note that any controller logic common to all objects belongs in the
 * implementation of {@link EntityController} and of
 * {@link AnnotationsController} that this wraps. Only functionality specific to
 * Dataset Annotations objects belongs in this controller.
 * 
 * @author deflaux
 */
@Controller
public class DatasetAnnotationsController extends BaseController implements
		AnnotationsController<Dataset> {

	private AnnotationsController<Dataset> datasetAnnotationsController;

	// TODO @Autowired, no GAE references allowed in this class
	private static final DAOFactory DAO_FACTORY = new GAEJDODAOFactoryImpl();
	private DatasetDAO datasetDao = DAO_FACTORY.getDatasetDAO();

	DatasetAnnotationsController() {

		datasetAnnotationsController = new AnnotationsControllerImp<Dataset>();

		setDao(datasetDao); // TODO remove this when @Autowired
	}

	@Override
	public void setDao(BaseDAO<Dataset> dao) {
		datasetDao = (DatasetDAO) dao;
		datasetAnnotationsController.setDao(datasetDao);
	}

	/*******************************************************************************
	 * Dataset Annotation RUD handlers
	 * 
	 */

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{id}"
			+ UrlHelpers.ANNOTATIONS, method = RequestMethod.GET)
	public @ResponseBody
	Annotations getEntityAnnotations(@PathVariable String id,
			HttpServletRequest request) throws NotFoundException,
			DatastoreException {
		return datasetAnnotationsController.getEntityAnnotations(id, request);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{id}"
			+ UrlHelpers.ANNOTATIONS, method = RequestMethod.PUT)
	public @ResponseBody
	Annotations updateEntityAnnotations(@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody Annotations updatedAnnotations,
			HttpServletRequest request) throws NotFoundException,
			ConflictingUpdateException, DatastoreException {
		return datasetAnnotationsController.updateEntityAnnotations(id, etag,
				updatedAnnotations, request);
	}

}
