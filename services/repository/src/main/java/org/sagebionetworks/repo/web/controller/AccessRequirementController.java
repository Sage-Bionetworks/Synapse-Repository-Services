package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.web.UrlHelpers.ID_PATH_VARIABLE;

import javax.servlet.http.HttpServletRequest;

import org.sagebionetworks.repo.model.AccessRequirement;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.PaginatedResults;
import org.sagebionetworks.repo.model.UnauthorizedException;
import org.sagebionetworks.repo.web.ForbiddenException;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class AccessRequirementController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	@ResponseStatus(HttpStatus.CREATED)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT, method = RequestMethod.POST)
	public @ResponseBody
	AccessRequirement createAccessRequirement(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@RequestHeader HttpHeaders header,
			HttpServletRequest request
			) throws Exception {
		return serviceProvider.getAccessRequirementService().createAccessRequirement(userId, header, request);
	}
	


	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_UNFULFILLED_WITH_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessRequirement>
	 getUnfulfilledAccessRequirement(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable(value = ID_PATH_VARIABLE) String entityId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, ForbiddenException {

		return serviceProvider.getAccessRequirementService().getUnfulfilledAccessRequirement(userId, entityId, request);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WITH_ENTITY_ID, method = RequestMethod.GET)
	public @ResponseBody
	PaginatedResults<AccessRequirement>
	 getAccessRequirements(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
				@PathVariable(value = ID_PATH_VARIABLE) String entityId,
			HttpServletRequest request
			) throws DatastoreException, UnauthorizedException, NotFoundException, ForbiddenException {
		return serviceProvider.getAccessRequirementService().getAccessRequirements(userId, entityId, request);
	}

	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_WITH_REQUIREMENT_ID, method = RequestMethod.DELETE)
	public void deleteAccessRequirements(
				@RequestParam(value = AuthorizationConstants.USER_ID_PARAM, required = false) String userId,
			@PathVariable String requirementId) throws DatastoreException, UnauthorizedException, NotFoundException, ForbiddenException {
		serviceProvider.getAccessRequirementService().deleteAccessRequirements(userId, requirementId);
	}
}
