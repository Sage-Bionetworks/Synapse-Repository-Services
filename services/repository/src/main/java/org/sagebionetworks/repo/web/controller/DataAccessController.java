package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.dataaccess.ChangeOwnershipRequest;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
import org.sagebionetworks.repo.web.service.ServiceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * <p>Some data in Synapse are governed by an ACTAccessRequirement. To gain access
 * to these data, a user must meet all requirements specified in the ACTAccessRequirement.</p>
 * <br>
 * <p>These services provide the APIs for users to create request to gain access to 
 * controlled data, and APIs for the ACT to review and grant access to users.</p>
 */
@ControllerInfo(displayName = "Data Access Services", path = "repo/v1")
@Controller
@RequestMapping(UrlHelpers.REPO_PATH)
public class DataAccessController extends BaseController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a new ResearchProject.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param toCreate - The object that contains information needed to create a new ResearchProject.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.RESEARCH_PROJECT, method = RequestMethod.POST)
	public @ResponseBody ResearchProject create(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ResearchProject toCreate) throws NotFoundException {
		return serviceProvider.getDataAccessService().create(userId, toCreate);
	}

	/**
	 * Update an existing ResearchProject.
	 * Only the owner of the researchProject can perform this action.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param toUpdate - The object that contains information needed to update a ResearchProject.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.RESEARCH_PROJECT_ID, method = RequestMethod.PUT)
	public @ResponseBody ResearchProject update(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ResearchProject toUpdate) throws NotFoundException {
		return serviceProvider.getDataAccessService().update(userId, toUpdate);
	}

	/**
	 * Retrieve an existing ResearchProject that the user owns.
	 * Only the owner of the researchProject can perform this action.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param accessRequirementId - The accessRequirementId that is used to look for the ResearchProject.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_RESEARCH_PROJECT, method = RequestMethod.GET)
	public @ResponseBody ResearchProject getUserOwnResearchProject(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String accessRequirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getUserOwnResearchProject(userId, accessRequirementId);
	}

	/**
	 * Change the ownership of an existing ResearchProject.
	 * Only an ACT member can perform this action.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param request - The object that contains information needed to update ownership of a ResearchProject.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.RESEARCH_PROJECT_ID_UPDATE_OWNERSHIP, method = RequestMethod.PUT)
	public @ResponseBody ResearchProject changeOwnership(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ChangeOwnershipRequest request) throws NotFoundException {
		return serviceProvider.getDataAccessService().changeOwnership(userId, request);
	}
}
