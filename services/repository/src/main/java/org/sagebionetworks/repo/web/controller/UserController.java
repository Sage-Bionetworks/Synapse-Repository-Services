package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jackson.schema.JsonSchema;
import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.BaseDAO;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.User;
import org.sagebionetworks.repo.model.UserDAO;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
import org.sagebionetworks.repo.web.EntitiesAccessor;
import org.sagebionetworks.repo.web.EntitiesAccessorImpl;
import org.sagebionetworks.repo.web.EntityController;
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
 * REST controller for CRUD operations on User objects
 * <p>
 * Note that any controller logic common to all objects belongs in the
 * implementation of {@link EntityController} that this wraps. Only
 * functionality specific to User objects belongs in this controller.
 * 
 */
@Controller
public class UserController extends BaseController implements
		EntityController<User> {

	private EntitiesAccessor<User> userAccessor;
	private UserDAO dao = null;
	private EntityController<User> userController;

	UserController() {
		userAccessor = new EntitiesAccessorImpl<User>();
		userController = new EntityControllerImp<User>(User.class,
				userAccessor);
	}

	private void checkAuthorization(String userId, Boolean readOnly) {
		dao = getDaoFactory().getUserDAO(userId);
		setDao(dao);
	}

	@Override
	public void setDao(BaseDAO<User> dao) {
		userAccessor.setDao(dao);
		userController.setDao(dao);
	}

	/*******************************************************************************
	 * User CRUD handlers.  We omit C and D, since creation and deletion are done by
	 * the Crowd mirror service.  Here we simply Read and Update
	 */

	@Override
	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.USER, method = RequestMethod.POST)
	public @ResponseBody
	User createEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestBody User newEntity, HttpServletRequest request)
			throws DatastoreException, InvalidModelException,
			UnauthorizedException {

		throw new UnauthorizedException("User creation via REST interface not allowed.");
	}

	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER + "/{id}", method = RequestMethod.GET)
	public @ResponseBody
	User getEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

		checkAuthorization(userId, true);
		User user = userController.getEntity(userId, id, request);
		addServiceSpecificMetadata(user, request);
		return user;
	}

	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER + "/{id}", method = RequestMethod.PUT)
	public @ResponseBody
	User updateEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody User updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {

		checkAuthorization(userId, false);
		User user = userController.updateEntity(userId, id, etag,
				updatedEntity, request);
		addServiceSpecificMetadata(user, request);
		return user;
	}

	@Override
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.USER + "/{id}", method = RequestMethod.DELETE)
	public void deleteEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id) throws NotFoundException,
			DatastoreException, UnauthorizedException {

		throw new UnauthorizedException("User deletion via REST interface not allowed.");
	}

	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<User> getEntities(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending,
			HttpServletRequest request) throws DatastoreException,
			UnauthorizedException {

		checkAuthorization(userId, true);
		PaginatedResults<User> results = userController.getEntities(
				userId, offset, limit, sort, ascending, request);

		for (User user : results.getResults()) {
			addServiceSpecificMetadata(user, request);
		}

		return results;
	}

	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER  + "/{id}" + UrlHelpers.SCHEMA, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getEntitySchema() throws DatastoreException {

		return userController.getEntitySchema();
	}
	
	@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER + UrlHelpers.SCHEMA, method = RequestMethod.GET)
	public @ResponseBody
	JsonSchema getEntitiesSchema() throws DatastoreException {

		return userController.getEntitiesSchema();
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
	@RequestMapping(value = UrlHelpers.USER + "/test", method = RequestMethod.GET)
	public String sanityCheck(ModelMap modelMap) {
		modelMap.put("hello", "REST for Users rocks");
		return ""; // use the default view
	}

	private void addServiceSpecificMetadata(User user,
			HttpServletRequest request) {

		// from DatasetController, not used in this class
//		user.setAnnotations(UrlHelpers.makeEntityPropertyUri(user,
//				Annotations.class, request));
//
//		user.setLayer(UrlHelpers.makeEntityUri(user, request) + "/layer");
		
		// this might be redudant...
		user.setUri(UrlHelpers.makeEntityUri(user, request));
		user.setEtag(UrlHelpers.makeEntityEtag(user));

		return;
	}
	

}
