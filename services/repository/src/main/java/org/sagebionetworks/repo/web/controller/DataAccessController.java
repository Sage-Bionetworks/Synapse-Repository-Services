package org.sagebionetworks.repo.web.controller;

import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictionInformation;
import org.sagebionetworks.repo.model.dataaccess.DataAccessRequestInterface;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmission;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.DataAccessSubmissionPageRequest;
import org.sagebionetworks.repo.model.dataaccess.OpenSubmissionPage;
import org.sagebionetworks.repo.model.dataaccess.ACTAccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.AccessRequirementStatus;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalRequest;
import org.sagebionetworks.repo.model.dataaccess.BatchAccessApprovalResult;
import org.sagebionetworks.repo.model.dataaccess.ResearchProject;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
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
	 * Create a new ResearchProject or update an existing ResearchProject.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param toCreateOrUpdate - The object that contains information needed to create/update a ResearchProject.
	 * @return
	 * @throws DatastoreException
	 * @throws NotFoundException
	 */
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
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_RESEARCH_PROJECT, method = RequestMethod.GET)
	public @ResponseBody ResearchProject getUserOwnResearchProjectForUpdate(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getUserOwnResearchProjectForUpdate(userId, requirementId);
	}

	/**
	 * Create a new DataAccessRequest or update an existing DataAccessRequest.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param toCreate - The object that contains information needed to create/update a DataAccessRequest.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST, method = RequestMethod.POST)
	public @ResponseBody DataAccessRequestInterface createOrUpdate(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody DataAccessRequestInterface toCreate) throws NotFoundException {
		return serviceProvider.getDataAccessService().createOrUpdate(userId, toCreate);
	}

	/**
	 * Retrieve the DataAccessRequest for update.
	 * If one does not exist, an DataAccessRequest with some re-filled information is returned.
	 * If a submission associated with the request is approved, and the requirement
	 * requires renewal, a refilled DataAccessRenewal is returned.
	 * Only the owner of the request can perform this action.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param accessRequirementId - The accessRequirementId that is used to look for the request.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_DATA_ACCESS_REQUEST_FOR_UPDATE, method = RequestMethod.GET)
	public @ResponseBody DataAccessRequestInterface getRequestForUpdate(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getRequestForUpdate(userId, requirementId);
	}

	/**
	 * Submit a DataAccessSubmission using information from a DataAccessRequest.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param requestId - The ID of the DataAccessRequest that is used to create the submission.
	 * @param etag - The etag og the DataAccessRequest. Etag must match the current etag of the DataAccessRequest.
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST_ID_SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody ACTAccessRequirementStatus submit(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requestId,
			@RequestParam(value = AuthorizationConstants.ETAG_PARAM) String etag)
					throws NotFoundException {
		return serviceProvider.getDataAccessService().submit(userId, requestId, etag);
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
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_ID_CANCEL, method = RequestMethod.PUT)
	public @ResponseBody ACTAccessRequirementStatus cancel(
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
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_ID, method = RequestMethod.PUT)
	public @ResponseBody DataAccessSubmission updateState(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SubmissionStateChangeRequest request) throws NotFoundException {
		return serviceProvider.getDataAccessService().updateState(userId, request);
	}

	/**
	 * Retrieve a list of submissions for a given access requirement ID.
	 * Only ACT member can perform this action.
	 * 
	 * @param userId
	 * @param dataAccessSubmissionPageRequest
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_LIST_SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody DataAccessSubmissionPage listSubmissions(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody DataAccessSubmissionPageRequest dataAccessSubmissionPageRequest) throws NotFoundException {
		return serviceProvider.getDataAccessService().listSubmissions(userId, dataAccessSubmissionPageRequest);
	}

	/**
	 * Retrieve an access requirement status for a given access requirement ID.
	 * 
	 * @param userId
	 * @param requirementId
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_STATUS, method = RequestMethod.GET)
	public @ResponseBody AccessRequirementStatus getAccessRequirementStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getAccessRequirementStatus(userId, requirementId);
	}

	/**
	 * Retrieve restriction information on an entity
	 * 
	 * @param userId
	 * @param id
	 * @return
	 * @throws NotFoundException
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ENTITY_ID_RESTRICTION_INFORMATION, method = RequestMethod.GET)
	public @ResponseBody RestrictionInformation getRestrictionInformation(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String id) throws NotFoundException {
		return serviceProvider.getDataAccessService().getRestrictionInformation(userId, id);
	}

	/**
	 * Retrieve information about submitted DataAccessSubmissions.
	 * Only ACT member can perform this action.
	 * 
	 * @param userId
	 * @param nextPageToken
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_OPEN_SUBMISSIONS, method = RequestMethod.GET)
	public @ResponseBody OpenSubmissionPage getOpenSubmissions(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestParam(value = "nextPageToken", required = false) String nextPageToken) {
		return serviceProvider.getDataAccessService().getOpenSubmissions(userId, nextPageToken);
	}

	/**
	 * Retrieve access approval information for a batch of users.
	 * Only ACT member can perform this action.
	 * 
	 * @param userId
	 * @param batchRequest
	 * @return
	 */
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_APPROVAL_BATCH, method = RequestMethod.POST)
	public @ResponseBody BatchAccessApprovalResult getAccessApprovalInfo(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody BatchAccessApprovalRequest batchRequest) {
		return serviceProvider.getDataAccessService().getAccessApprovalInfo(userId, batchRequest);
	}
}
