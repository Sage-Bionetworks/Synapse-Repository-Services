package org.sagebionetworks.repo.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.authutil.AuthUtilConstants;
import org.sagebionetworks.repo.model.AuthorizationManager;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.InvalidModelException;
import org.sagebionetworks.repo.model.ResourceAccess;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.web.ConflictingUpdateException;
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
 * REST controller for CRUD operations on UserGroup objects
 * <p>
 * Note that any controller logic common to all objects belongs in the
 * implementation of {@link EntityController} that this wraps. Only
 * functionality specific to UserGroup objects belongs in this controller.
 * 
 */
@Controller
public class UserGroupController extends BaseController2  {

//	private EntitiesAccessor<UserGroup> userGroupAccessor;
//	private JDOUserGroupDAO dao = null;
//	private EntityController<UserGroup> userGroupController;

	UserGroupController() {
//		userGroupAccessor = new EntitiesAccessorImpl<UserGroup>();
//		userGroupController = new EntityControllerImp<UserGroup>(UserGroup.class,
//				userGroupAccessor);
	}

	private void checkAuthorization(String userId, Boolean readOnly) {
//		dao = getDaoFactory().getUserGroupDAO(userId);
//		setDao(dao);
	}

//	@Override
//	public void setDao(BaseDAO<UserGroup> dao) {
//		userGroupAccessor.setDao(dao);
//		userGroupController.setDao(dao);
//	}

	/*******************************************************************************
	 * UserGroup CRUD handlers
	 */

////	@Override
//	@ResponseStatus(HttpStatus.CREATED)
//	@RequestMapping(value = UrlHelpers.USERGROUP, method = RequestMethod.POST)
//	public @ResponseBody
//	UserGroup createEntity(
//			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
//			@RequestBody UserGroup newEntity, HttpServletRequest request)
//			throws DatastoreException, InvalidModelException,
//			UnauthorizedException {
//
//		checkAuthorization(userId, false);
//		// TODO: may only change specify types if 'userId' is an administrator
//		UserGroup userGroup = userGroupController.createEntity(userId, newEntity,
//				request);
//		addServiceSpecificMetadata(userGroup, request);
//		return userGroup;
//	}

	//@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}", method = RequestMethod.GET)
	public @ResponseBody
	UserGroup getEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, HttpServletRequest request)
			throws NotFoundException, DatastoreException, UnauthorizedException {

//		checkAuthorization(userId, true);
//		UserGroup userGroup = userGroupController.getEntity(userId, id, request);
//		addServiceSpecificMetadata(userGroup, request);
//		return userGroup;
		throw new RuntimeException("Not yet implemented.");
	}

	//@Override
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}", method = RequestMethod.PUT)
	public @ResponseBody
	UserGroup updateEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,
			@RequestHeader(ServiceConstants.ETAG_HEADER) Integer etag,
			@RequestBody UserGroup updatedEntity, HttpServletRequest request)
			throws NotFoundException, ConflictingUpdateException,
			DatastoreException, InvalidModelException, UnauthorizedException {

		checkAuthorization(userId, false);
		// TODO:  may only change 'creatableTypes' if userId is an administrator
//		UserGroup userGroup = userGroupController.updateEntity(userId, id, etag,
//				updatedEntity, request);
//		addServiceSpecificMetadata(userGroup, request);
//		return userGroup;
		throw new RuntimeException("Not yet implemented.");
	}

	//@Override
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}", method = RequestMethod.DELETE)
	public void deleteEntity(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id) throws NotFoundException,
			DatastoreException, UnauthorizedException {

//		checkAuthorization(userId, false);
//		userGroupController.deleteEntity(userId, id);
//		return;
		throw new RuntimeException("Not yet implemented.");
	}

//	//@Override
//	@ResponseStatus(HttpStatus.OK)
//	@RequestMapping(value = UrlHelpers.USERGROUP, method = RequestMethod.GET)
//	public @ResponseBody
//	PaginatedResults<UserGroup> getEntities(
//			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
//			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM) Integer offset,
//			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_LIMIT_PARAM) Integer limit,
//			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
//			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending,
//			HttpServletRequest request) throws DatastoreException,
//			UnauthorizedException {
//
//		checkAuthorization(userId, true);
//		PaginatedResults<UserGroup> results = userGroupController.getEntities(
//				userId, offset, limit, sort, ascending, request);
//
//		for (UserGroup userGroup : results.getResults()) {
//			addServiceSpecificMetadata(userGroup, request);
//		}
//
//		return results;
//	}

//	//@Override
//	@ResponseStatus(HttpStatus.OK)
//	@RequestMapping(value = UrlHelpers.USERGROUP  + "/{id}" + UrlHelpers.SCHEMA, method = RequestMethod.GET)
//	public @ResponseBody
//	JsonSchema getEntitySchema() throws DatastoreException {
//
//		return userGroupController.getEntitySchema();
//	}
//	
//	//@Override
//	@ResponseStatus(HttpStatus.OK)
//	@RequestMapping(value = UrlHelpers.USERGROUP + UrlHelpers.SCHEMA, method = RequestMethod.GET)
//	public @ResponseBody
//	JsonSchema getEntitiesSchema() throws DatastoreException {
//
//		return userGroupController.getEntitiesSchema();
//	}
	
	/**
	 * Simple sanity check test request, using the default view
	 * <p>
	 * 
	 * @param modelMap
	 *            the parameter into which output data is to be stored
	 * @return a dummy hard-coded response
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USERGROUP + "/test", method = RequestMethod.GET)
	public String sanityCheck(ModelMap modelMap) {
		modelMap.put("hello", "REST for UserGroups rocks");
		return ""; // use the default view
	}

	private void addServiceSpecificMetadata(UserGroup userGroup,
			HttpServletRequest request) {

		// from DatasetController, not used in this class
//		userGroup.setAnnotations(UrlHelpers.makeEntityPropertyUri(userGroup,
//				Annotations.class, request));
//
//		userGroup.setLayer(UrlHelpers.makeEntityUri(userGroup, request) + "/layer");

		return;
	}
	
//	@ResponseStatus(HttpStatus.CREATED)
//	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}" + UrlHelpers.USERS+"/{uid}", method = RequestMethod.POST)
//	public void addUser(
//			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
//			@PathVariable String id, 
//			@PathVariable String uid)
//			throws NotFoundException, DatastoreException, InvalidModelException,
//			UnauthorizedException {
//
//		checkAuthorization(userId, false);
//		UserGroup userGroup = new UserGroup();
//		userGroup.setId(id);
//		User user = new User();
//		user.setId(uid);
//		dao.addUser(userGroup, user);
//	}

//	
//	@ResponseStatus(HttpStatus.NO_CONTENT)
//	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}" + UrlHelpers.USERS+"/{uid}", method = RequestMethod.DELETE)
//	public void removeUser(
//			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
//			@PathVariable String id,
//			@PathVariable String uid) throws NotFoundException,
//			DatastoreException, UnauthorizedException {
//
//		checkAuthorization(userId, false);
//		UserGroup userGroup = new UserGroup();
//		userGroup.setId(id);
//		User user = new User();
//		user.setId(uid);
//		dao.removeUser(userGroup, user);
//		return;
//	}

//	@ResponseStatus(HttpStatus.OK)
//	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}" + UrlHelpers.USERS, method = RequestMethod.GET)
//	public @ResponseBody
//	PaginatedResults<User>  getUsers(
//			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
//			@PathVariable String id,
//			HttpServletRequest request
//			) throws DatastoreException,
//			UnauthorizedException, NotFoundException {
//
//		checkAuthorization(userId, true);
//		UserGroup userGroup = new UserGroup();
//		userGroup.setId(id);
//		List<User> results = new ArrayList<User>(dao.getUsers(userGroup));
//
//		Integer totalNumberOfEntities = results.size();
//
//		PaginatedResults<User> prs = new PaginatedResults<User>(request.getServletPath()
//				+ UrlHelpers.getUrlForModel(User.class), results,
//				totalNumberOfEntities, 0, results.size(), null, false);
//		
//		for (User user : prs.getResults()) {
//			user.setUri(UrlHelpers.makeEntityUri(user, request));
//			user.setEtag(UrlHelpers.makeEntityEtag(user));
//		}
//
//		return prs;
//
//	}


//	/*
//	 * By convention the 'type' of a resource is the fully qualified class name of the DTO 
//	 * for that resource.  Therefore we can use Class.forName to create a new instance.
//	 */
//	public static Base typeToBase(String type) {
//		try {
//			return (Base)Class.forName(type).newInstance();
//		} catch (Exception e) {
//			throw new RuntimeException(type, e);
//		}
//	}

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}" + UrlHelpers.RESOURCES+"/{rid}", method = RequestMethod.POST)
	public void addResource(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id,  
			@PathVariable String rid,
			@RequestBody ResourceAccess accessTypes)
			throws NotFoundException, DatastoreException, InvalidModelException,
			UnauthorizedException {

		checkAuthorization(userId, false);
		UserGroup userGroup = new UserGroup();
		userGroup.setId(id);
		String rtype = AuthorizationManager.NODE_RESOURCE_TYPE;
//		Base resource = typeToBase(rtype);  // TODO this will break until we replace with new AuthorizationDAO
//		resource.setId(rid);
//		dao.addResource(userGroup, resource, accessTypes.getAccessType());
		// TODO: call the UserGroupManager
	}

	
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}" + UrlHelpers.RESOURCES + "/{rid}", method = RequestMethod.DELETE)
	public void removeResource(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, 
			@PathVariable String rid) throws NotFoundException,
			DatastoreException, UnauthorizedException {

		checkAuthorization(userId, false);
		UserGroup userGroup = new UserGroup();
		userGroup.setId(id);
		String rtype = AuthorizationManager.NODE_RESOURCE_TYPE;
//		Base resource = typeToBase(rtype); // TODO this will break until we replace with new AuthorizationDAO
//		resource.setId(rid);
//		dao.removeResource(userGroup, resource);
		// TODO: call the UserGroupManager
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}" + UrlHelpers.RESOURCES + "/{rid}", method = RequestMethod.GET)
	public @ResponseBody
	ResourceAccess getAccessTypes(
			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String id, 
			@PathVariable String rid
			) throws DatastoreException,
			UnauthorizedException, NotFoundException {

		checkAuthorization(userId, true);
		UserGroup userGroup = new UserGroup();
		userGroup.setId(id);
		String rtype = AuthorizationManager.NODE_RESOURCE_TYPE;
//		Base resource = typeToBase(rtype); // TODO this will break until we replace with new AuthorizationDAO
//		resource.setId(rid);
//		Collection<AuthorizationConstants.ACCESS_TYPE> results = dao.getAccessTypes(userGroup, resource);
		// TODO Call the UserGroupManager
		ResourceAccess ra = new ResourceAccess();
//		ra.setAccessType(results);
		return ra;
	}

//	@ResponseStatus(HttpStatus.CREATED)
//	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}" + UrlHelpers.USERGROUP+"/{rid}", method = RequestMethod.POST)
//	public void addGroup(
//			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
//			@PathVariable String id,  
//			@PathVariable String rid,
//			@RequestBody ResourceAccess accessTypes)
//			throws NotFoundException, DatastoreException, InvalidModelException,
//			UnauthorizedException {
//
//		checkAuthorization(userId, false);
//		UserGroup userGroup = new UserGroup();
//		userGroup.setId(id);
//		String rtype = UserGroup.class.getName();
//		Base resource = typeToBase(rtype);
//		resource.setId(rid);
//		dao.addResource(userGroup, resource, accessTypes.getAccessType());
//	}
//
//	
//	@ResponseStatus(HttpStatus.NO_CONTENT)
//	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}" + UrlHelpers.USERGROUP + "/{rid}", method = RequestMethod.DELETE)
//	public void removeGroup(
//			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
//			@PathVariable String id, 
//			@PathVariable String rid) throws NotFoundException,
//			DatastoreException, UnauthorizedException {
//
//		checkAuthorization(userId, false);
//		UserGroup userGroup = new UserGroup();
//		userGroup.setId(id);
//		String rtype = UserGroup.class.getName();
//		Base resource = typeToBase(rtype);
//		resource.setId(rid);
//		dao.removeResource(userGroup, resource);
//	}
//
//	@ResponseStatus(HttpStatus.OK)
//	@RequestMapping(value = UrlHelpers.USERGROUP + "/{id}" + UrlHelpers.USERGROUP + "/{rid}", method = RequestMethod.GET)
//	public @ResponseBody
//	ResourceAccess getGroupAccessTypes(
//			@RequestParam(value = AuthUtilConstants.USER_ID_PARAM, required = false) String userId,
//			@PathVariable String id, 
//			@PathVariable String rid
//			) throws DatastoreException,
//			UnauthorizedException, NotFoundException {
//
//		checkAuthorization(userId, true);
//		UserGroup userGroup = new UserGroup();
//		userGroup.setId(id);
//		String rtype = UserGroup.class.getName();
//		Base resource = typeToBase(rtype);
//		resource.setId(rid);
//		Collection<AuthorizationConstants.ACCESS_TYPE> results = dao.getAccessTypes(userGroup, resource);
//		ResourceAccess ra = new ResourceAccess();
//		ra.setAccessType(results);
//		return ra;
//	}

}
