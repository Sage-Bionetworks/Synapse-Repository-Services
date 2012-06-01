package org.sagebionetworks.repo.web.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.ServiceConstants;
import org.sagebionetworks.repo.manager.PermissionsManager;
import org.sagebionetworks.repo.manager.UserManager;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.model.UserGroup;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller

public class UserGroupController extends BaseController {

	@Autowired
	PermissionsManager permissionsManager;
	
	@Autowired
	UserManager userManager;


	/**
	 * Get the user-groups in the system
	 * @param userId - The user that is making the request.
	 * @param request
	 * @return The UserGroups for individuals
	 * @throws DatastoreException - Thrown when there is a server-side problem.
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USERGROUP, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<UserGroup> getUserGroups(HttpServletRequest request, 
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestParam(value = ServiceConstants.PAGINATION_OFFSET_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PAGINATION_OFFSET_PARAM_NEW) Integer offset,
			@RequestParam(value = ServiceConstants.PAGINATION_LIMIT_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_PRINCIPALS_PAGINATION_LIMIT_PARAM) Integer limit,
			@RequestParam(value = ServiceConstants.SORT_BY_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_SORT_BY_PARAM) String sort,
			@RequestParam(value = ServiceConstants.ASCENDING_PARAM, required = false, defaultValue = ServiceConstants.DEFAULT_ASCENDING_PARAM) Boolean ascending
			) throws DatastoreException, UnauthorizedException, NotFoundException {
		UserInfo userInfo = userManager.getUserInfo(userId);
		long endExcl = offset+limit;
		List<UserGroup> results = permissionsManager.getGroupsInRange(userInfo, offset, endExcl, sort, ascending);
		int totalNumberOfResults = permissionsManager.getGroups(userInfo).size();
		return new PaginatedResults<UserGroup>(
				request.getServletPath()+UrlHelpers.USERGROUP, 
				results,
				totalNumberOfResults, 
				offset, 
				limit,
				sort, 
				ascending);
	}

//	/**
//	 * Get the schema for an ACL
//	 * @param id
//	 * @param request
//	 * @return
//	 * @throws DatastoreException
//	 */
//	@ResponseStatus(HttpStatus.OK)
//	@RequestMapping(value ={UrlHelpers.USERGROUP + UrlHelpers.SCHEMA}, method = RequestMethod.GET)
//	public @ResponseBody
//	JsonSchema getGroupSchema() throws DatastoreException {
//		return SchemaHelper.getSchema(UserGroup.class);
//	}
	

	
}
