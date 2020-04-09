package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.UserInfo;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
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
public class DataAccessController {

	@Autowired
	ServiceProvider serviceProvider;

	/**
	 * Create a new ResearchProject or update an existing ResearchProject.
	 * 
	 * @param toCreateOrUpdate - The object that contains information needed to create/update a ResearchProject.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.RESEARCH_PROJECT, method = RequestMethod.POST)
	public @ResponseBody ResearchProject createOrUpdate(
			UserInfo userInfo,
			@RequestBody ResearchProject toCreateOrUpdate) throws NotFoundException {
		return serviceProvider.getDataAccessService().createOrUpdate(userInfo, toCreateOrUpdate);
	}

	/**
	 * Retrieve an existing ResearchProject that the user owns.
	 * If none exists, a ResearchProject with some re-filled information is returned to the user.
	 * Only the owner of the researchProject can perform this action.
	 * 
	 * @param accessRequirementId - The accessRequirementId that is used to look for the ResearchProject.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_RESEARCH_PROJECT, method = RequestMethod.GET)
	public @ResponseBody ResearchProject getUserOwnResearchProjectForUpdate(
			UserInfo userInfo,
			@PathVariable String requirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getUserOwnResearchProjectForUpdate(userInfo, requirementId);
	}

	/**
	 * Create a new Request or update an existing Request.
	 * 
	 * @param toCreate - The object that contains information needed to create/update a Request.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST, method = RequestMethod.POST)
	public @ResponseBody RequestInterface createOrUpdate(
			UserInfo userInfo,
			@RequestBody RequestInterface toCreate) throws NotFoundException {
		return serviceProvider.getDataAccessService().createOrUpdate(userInfo, toCreate);
	}

	/**
	 * Retrieve the Request for update.
	 * If one does not exist, an Request with some re-filled information is returned.
	 * If a submission associated with the request is approved, and the requirement
	 * requires renewal, a refilled Renewal is returned.
	 * Only the owner of the request can perform this action.
	 * 
	 * @param accessRequirementId - The accessRequirementId that is used to look for the request.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_DATA_ACCESS_REQUEST_FOR_UPDATE, method = RequestMethod.GET)
	public @ResponseBody RequestInterface getRequestForUpdate(
			UserInfo userInfo,
			@PathVariable String requirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getRequestForUpdate(userInfo, requirementId);
	}

	/**
	 * Submit a Submission using information from a Request.
	 * 
	 * @param requestId - The object that contains information to create a submission.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST_ID_SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody SubmissionStatus submit(
			UserInfo userInfo,
			@RequestBody CreateSubmissionRequest request)
					throws NotFoundException {
		return serviceProvider.getDataAccessService().submit(userInfo, request);
	}

	/**
	 * Cancel a submission.
	 * Only the user who created this submission can cancel it.
	 * 
	 * @param submissionId - The ID of the submission to cancel.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_ID_CANCEL, method = RequestMethod.PUT)
	public @ResponseBody SubmissionStatus cancel(
			UserInfo userInfo,
			@PathVariable String submissionId) throws NotFoundException {
		return serviceProvider.getDataAccessService().cancel(userInfo, submissionId);
	}

	/**
	 * Request to update a submission' state.
	 * Only ACT member can perform this action.
	 * 
	 * @param request
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_ID, method = RequestMethod.PUT)
	public @ResponseBody Submission updateState(
			UserInfo userInfo,
			@RequestBody SubmissionStateChangeRequest request) throws NotFoundException {
		return serviceProvider.getDataAccessService().updateState(userInfo, request);
	}

	/**
	 * Retrieve a list of submissions for a given access requirement ID.
	 * Only ACT member can perform this action.
	 * 
	 * @param SubmissionPageRequest
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_LIST_SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody SubmissionPage listSubmissions(
			UserInfo userInfo,
			@RequestBody SubmissionPageRequest SubmissionPageRequest) throws NotFoundException {
		return serviceProvider.getDataAccessService().listSubmissions(userInfo, SubmissionPageRequest);
	}

	/**
	 * Retrieve an access requirement status for a given access requirement ID.
	 * 
	 * @param requirementId
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_STATUS, method = RequestMethod.GET)
	public @ResponseBody AccessRequirementStatus getAccessRequirementStatus(
			UserInfo userInfo,
			@PathVariable String requirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getAccessRequirementStatus(userInfo, requirementId);
	}

	/**
	 * Retrieve restriction information on a restrictable object
	 * 
	 * @param request
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.RESTRICTION_INFORMATION, method = RequestMethod.POST)
	public @ResponseBody RestrictionInformationResponse getRestrictionInformation(
		UserInfo userInfo,
		@RequestBody RestrictionInformationRequest request) throws NotFoundException {
		return serviceProvider.getDataAccessService().getRestrictionInformation(userInfo, request);
	}

	/**
	 * Retrieve information about submitted Submissions.
	 * Only ACT member can perform this action.
	 * 
	 * @param nextPageToken
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_OPEN_SUBMISSIONS, method = RequestMethod.GET)
	public @ResponseBody OpenSubmissionPage getOpenSubmissions(
			UserInfo userInfo,
			@RequestParam(value = UrlHelpers.NEXT_PAGE_TOKEN_PARAM, required = false) String nextPageToken) {
		return serviceProvider.getDataAccessService().getOpenSubmissions(userInfo, nextPageToken);
	}
}
