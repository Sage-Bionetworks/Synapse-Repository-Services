package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.CreateSubmissionRequest;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.RequestInterface;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.Submission;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionInfoPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
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
	 * @param userId - The ID of the user who is making the request.
	 * @param toCreateOrUpdate - The object that contains information needed to create/update a ResearchProject.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.RESEARCH_PROJECT, method = RequestMethod.POST)
	public @ResponseBody ResearchProject createOrUpdate(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody ResearchProject toCreateOrUpdate) throws NotFoundException {
		return serviceProvider.getDataAccessService().createOrUpdate(userId, toCreateOrUpdate);
	}

	/**
	 * Retrieve an existing ResearchProject that the user owns.
	 * If none exists, a ResearchProject with some re-filled information is returned to the user.
	 * Only the owner of the researchProject can perform this action.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param accessRequirementId - The accessRequirementId that is used to look for the ResearchProject.
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_RESEARCH_PROJECT, method = RequestMethod.GET)
	public @ResponseBody ResearchProject getUserOwnResearchProjectForUpdate(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getUserOwnResearchProjectForUpdate(userId, requirementId);
	}

	/**
	 * Create a new Request or update an existing Request.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param toCreate - The object that contains information needed to create/update a Request.
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST, method = RequestMethod.POST)
	public @ResponseBody RequestInterface createOrUpdate(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody RequestInterface toCreate) throws NotFoundException {
		return serviceProvider.getDataAccessService().createOrUpdate(userId, toCreate);
	}

	/**
	 * Retrieve the Request for update.
	 * If one does not exist, an Request with some re-filled information is returned.
	 * If a submission associated with the request is approved, and the requirement
	 * requires renewal, a refilled Renewal is returned.
	 * Only the owner of the request can perform this action.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param accessRequirementId - The accessRequirementId that is used to look for the request.
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_DATA_ACCESS_REQUEST_FOR_UPDATE, method = RequestMethod.GET)
	public @ResponseBody RequestInterface getRequestForUpdate(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getRequestForUpdate(userId, requirementId);
	}

	/**
	 * Submit a Submission using information from a Request.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param requestId - The object that contains information to create a submission.
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST_ID_SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody SubmissionStatus submit(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CreateSubmissionRequest request)
					throws NotFoundException {
		return serviceProvider.getDataAccessService().submit(userId, request);
	}

	/**
	 * Cancel a submission.
	 * Only the user who created this submission can cancel it.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param submissionId - The ID of the submission to cancel.
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_ID_CANCEL, method = RequestMethod.PUT)
	public @ResponseBody SubmissionStatus cancel(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String submissionId) throws NotFoundException {
		return serviceProvider.getDataAccessService().cancel(userId, submissionId);
	}

	/**
	 * Request to update a submission' state.
	 * Only ACT member can perform this action.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_ID, method = RequestMethod.PUT)
	public @ResponseBody Submission updateState(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SubmissionStateChangeRequest request) throws NotFoundException {
		return serviceProvider.getDataAccessService().updateState(userId, request);
	}

	/**
	 * Retrieve a list of submissions for a given access requirement ID.
	 * Only an ACT member can perform this action.
	 * 
	 * @param userId
	 * @param SubmissionPageRequest
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_LIST_SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody SubmissionPage listSubmissions(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SubmissionPageRequest submissionPageRequest) throws NotFoundException {
		return serviceProvider.getDataAccessService().listSubmissions(userId, submissionPageRequest);
	}
	
	/**
	 * Delete a submission.
	 * Only an ACT member can perform this action.
	 * 
	 * @param userId
	 * @param submissionId
	 * @throws NotFoundException
	 */
	@RequiredScope({modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_ID, method = RequestMethod.DELETE)
	public void deleteSubmission(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String submissionId) throws NotFoundException {
		serviceProvider.getDataAccessService().deleteSubmission(userId, submissionId);
	}

	/**
	 * Return the research project info for approved data access submissions, 
	 * ordered by submission modified-on date, ascending
	 * 
	 * @param userId
	 * @param researchProjectPageRequest
	 * @return in order of modifiedOn, ascending
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_LIST_APPROVED_SUBMISISON_INFO, method = RequestMethod.POST)
	public @ResponseBody SubmissionInfoPage listInfoForApprovedSubmissions(
			@RequestBody SubmissionInfoPageRequest submissionInfoPageRequest) throws NotFoundException {
		return serviceProvider.getDataAccessService().listInfoForApprovedSubmissions(submissionInfoPageRequest);
	}

	/**
	 * Retrieve an access requirement status for a given access requirement ID.
	 * 
	 * @param userId
	 * @param requirementId
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_STATUS, method = RequestMethod.GET)
	public @ResponseBody AccessRequirementStatus getAccessRequirementStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getAccessRequirementStatus(userId, requirementId);
	}

	/**
	 * Retrieve restriction information on a restrictable object
	 * 
	 * @param request
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.RESTRICTION_INFORMATION, method = RequestMethod.POST)
	public @ResponseBody RestrictionInformationResponse getRestrictionInformation(
		@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
		@RequestBody RestrictionInformationRequest request) throws NotFoundException {
		return serviceProvider.getDataAccessService().getRestrictionInformation(userId, request);
	}

	/**
	 * Retrieve information about submitted Submissions.
	 * Only ACT member can perform this action.
	 * 
	 * @param userId
	 * @param nextPageToken
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_OPEN_SUBMISSIONS, method = RequestMethod.GET)
	public @ResponseBody OpenSubmissionPage getOpenSubmissions(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = UrlHelpers.NEXT_PAGE_TOKEN_PARAM, required = false) String nextPageToken) {
		return serviceProvider.getDataAccessService().getOpenSubmissions(userId, nextPageToken);
	}
}
