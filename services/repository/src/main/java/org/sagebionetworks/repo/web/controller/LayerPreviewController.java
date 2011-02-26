package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.DependentPropertyDAO;
import org.sagebionetworks.repo.model.InputDataLayer;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.LayerPreview;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.DependentEntityController;
import org.sagebionetworks.repo.web.DependentEntityControllerImp;
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
 * REST controller for RU operations on LayerPreview objects
 * <p>
 * 
 * Note that any controller logic common to all dependent objects belongs in the
 * implementation of {@link DependentEntityController} that this wraps. Only
 * functionality specific to LayerPreview objects belongs in this controller.
 * 
 * @author deflaux
 */
@Controller
public class LayerPreviewController extends BaseController implements
		DependentEntityController<LayerPreview, InputDataLayer> {

	private DependentEntityController<LayerPreview, InputDataLayer> controller;

	LayerPreviewController() {
		controller = new DependentEntityControllerImp<LayerPreview, InputDataLayer>();
	}

	private void checkAuthorization(String userId, Boolean readOnly) {
		DependentPropertyDAO<LayerPreview, InputDataLayer> dao = getDaoFactory()
				.getLayerPreviewDAO(userId);
		setDao(dao);
	}

	@Override
	public void setDao(DependentPropertyDAO<LayerPreview, InputDataLayer> dao) {
		controller.setDao(dao);

	}

	/*******************************************************************************
	 * Layer Preview handlers
	 */

	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.PREVIEW, method = RequestMethod.GET)
	public @ResponseBody
	LayerPreview getDependentEntity(
			@RequestParam(value = ServiceConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		checkAuthorization(userId, true);
		return controller.getDependentEntity(userId, id, request);
	}

	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATASET + "/{parentId}"
			+ UrlHelpers.LAYER + "/{id}" + UrlHelpers.PREVIEW, method = RequestMethod.PUT)
	public @ResponseBody
	LayerPreview updateDependentEntity(
			@RequestParam(value = ServiceConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody LayerPreview updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {

		checkAuthorization(userId, false);
		return controller.updateDependentEntity(userId, id, etag,
				updatedEntity, request);
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
			+ UrlHelpers.LAYER + UrlHelpers.PREVIEW + "/test", method = RequestMethod.GET)
	public String sanityCheck(ModelMap modelMap) {
		modelMap.put("hello", "REST for Dataset Layer Previews rocks");
		return ""; // use the default view
	}

}
