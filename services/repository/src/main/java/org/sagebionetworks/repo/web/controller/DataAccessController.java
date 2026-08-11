package org.sagebionetworks.repo.web.controller;

import static org.sagebionetworks.repo.model.oauth.OAuthScope.modify;
import static org.sagebionetworks.repo.model.oauth.OAuthScope.view;

import org.sagebionetworks.repo.model.AccessApproval;
import org.sagebionetworks.repo.model.AuthorizationConstants;
import org.sagebionetworks.repo.model.DatastoreException;
import org.sagebionetworks.repo.model.RestrictionInformationBatchRequest;
import org.sagebionetworks.repo.model.RestrictionInformationBatchResponse;
import org.sagebionetworks.repo.model.RestrictionInformationRequest;
import org.sagebionetworks.repo.model.RestrictionInformationResponse;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestList;
import org.sagebionetworks.repo.model.dataaccess.AccessRequestListRequest;
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
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionSearchResponse;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStateChangeRequest;
import org.sagebionetworks.repo.model.dataaccess.SubmissionStatus;
import org.sagebionetworks.repo.model.dataaccess.UserSubmissionSearchRequest;
import org.sagebionetworks.repo.model.dataaccess.UserSubmissionSearchResponse;
import org.sagebionetworks.repo.model.educ.EDucFileHandleId;
import org.sagebionetworks.repo.model.educ.EDucSignatureStatus;
import org.sagebionetworks.repo.model.educ.EDucTemplateListRequest;
import org.sagebionetworks.repo.model.educ.EDucTemplatePage;
import org.sagebionetworks.repo.model.educ.EDucTemplateValidationResult;
import org.sagebionetworks.repo.model.educ.EDucSignatureQuota;
import org.sagebionetworks.repo.service.ServiceProvider;
import org.sagebionetworks.repo.web.NotFoundException;
import org.sagebionetworks.repo.web.RequiredScope;
import org.sagebionetworks.repo.web.UrlHelpers;
import org.sagebionetworks.repo.web.rest.doc.ControllerInfo;
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
	 * If none exists, a ResearchProject with some pre-filled information is returned to the user.
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
	 * List data access requests associated with the current user.
	 *
	 * @param userId  - The ID of the user who is making the request.
	 * @param request - Pagination parameters.
	 * @return A paginated list of access request summaries.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST_LIST, method = RequestMethod.POST)
	public @ResponseBody AccessRequestList listUserRequests(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody AccessRequestListRequest request) {
		return serviceProvider.getDataAccessService().listUserRequests(userId, request);
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
	 * Submit an Access Request using information from a Request.
	 * 
	 * @param userId - The ID of the user who is making the request.
	 * @param request - The object that contains information to create a submission.
	 * @param requestId - The ID of the request object.
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST_ID_SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody SubmissionStatus submit(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody CreateSubmissionRequest request,
			@PathVariable String requestId)
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
	 * @param submissionId
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view,modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_ID, method = RequestMethod.PUT)
	public @ResponseBody Submission updateState(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SubmissionStateChangeRequest request,
			@PathVariable String submissionId) throws NotFoundException {
		return serviceProvider.getDataAccessService().updateState(userId, request);
	}

	/**
	 * Retrieve a list of submissions for a given access requirement ID. 
	 * 
	 * Allows to optionally filter by submission state and/or id of an accessor setting the associated fields in the <a href="${org.sagebionetworks.repo.model.dataaccess.SubmissionPageRequest}">SubmissionPageRequest</a>.
	 * 
	 * Only an ACT member can perform this action.
	 * 
	 * @param userId
	 * @param submissionPageRequest
	 * @param requirementId
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_LIST_SUBMISSION, method = RequestMethod.POST)
	public @ResponseBody SubmissionPage listSubmissions(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SubmissionPageRequest submissionPageRequest,
			@PathVariable String requirementId) throws NotFoundException {
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
	 * ordered by submission modified-on date, descending.  Note that accessor 
	 * changes are only visible to members of the ACT.
	 * 
	 * @param userId
	 * @param submissionInfoPageRequest
	 * @param requirementId
	 * @return in order of modifiedOn, ascending
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.ACCESS_REQUIREMENT_ID_LIST_APPROVED_SUBMISISON_INFO, method = RequestMethod.POST)
	public @ResponseBody SubmissionInfoPage listInfoForApprovedSubmissions(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SubmissionInfoPageRequest submissionInfoPageRequest,
			@PathVariable String requirementId) throws NotFoundException {
		return serviceProvider.getDataAccessService().listInfoForApprovedSubmissions(userId, submissionInfoPageRequest);
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
	 * Retrieve restriction information on a batch of restrictable object, limited to a maxiumum of 50 object ids
	 * 
	 * @param request
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.RESTRICTION_INFORMATION + "/batch", method = RequestMethod.POST)
	public @ResponseBody RestrictionInformationBatchResponse getRestrictionInformationBatch(
		@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
		@RequestBody RestrictionInformationBatchRequest request) throws NotFoundException {
		return serviceProvider.getDataAccessService().getRestrictionInformationBatch(userId, request);
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

	/**
	 * List available eDUC (electronic Data Use Certificate) templates that the
	 * ACT may use when issuing access certificates. Only an ACT member can
	 * perform this action.
	 *
	 * @param userId
	 * @param request
	 * @return a page of eDUC template metadata
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EDUC_TEMPLATE, method = RequestMethod.POST)
	public @ResponseBody EDucTemplatePage listEDucTemplates(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody EDucTemplateListRequest request)
			throws Exception {
		return serviceProvider.getEDucService().listTemplates(userId, request);
	}

	/**
	 * Validate a DocuSign template for use with Synapse eDUC.
	 * Only an ACT member can perform this action.
	 *
	 * @param userId     - The ID of the user who is making the request.
	 * @param templateId - The DocuSign template ID to validate.
	 * @return The validation result indicating whether the template is valid.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.EDUC_TEMPLATE_VALIDATE, method = RequestMethod.GET)
	public @ResponseBody EDucTemplateValidationResult validateEDucTemplate(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String templateId) {
		return serviceProvider.getEDucService().validateTemplate(userId, templateId);
	}
	
	/**
	 * Performs a search through access submissions that are reviewable by the user and that match the criteria in the given request.
	 * 
	 * An ACT user can see all the submissions, while a non-ACT user can only see the submissions whose access requirement has an ACL with REVIEW_SUBMISSION for the user.
	 * 
	 * An ACT user can limit the type of submissions to only see those that have an ACL assigned (e.g. delegated submissions) or those that DO NOT have any ACL (e.g. ACT only) using the reviewerFilterType
	 * parameter in the request.
	 * 
	 * For a non-ACT user reviewerFilterType.ALL is the same as reviewerFilterType.DELEGATED_ONLY and using reviewerFilterType.ACT_ONLY will produce an empty result.
	 * 
	 * @param userId
	 * @param request
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_SEARCH, method = RequestMethod.POST)
	public @ResponseBody SubmissionSearchResponse searchSubmissions(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody SubmissionSearchRequest request) {
		return serviceProvider.getDataAccessService().searchSubmissions(userId, request);
	}
	
	/**
	 * Fetch a submission by its id. If the user is a not accessor to the submission or part of the ACT,
	 * they must be validated and assigned as reviewers of the AR submissions in order to fetch the submission.
	 * 
	 * @param userId
	 * @param submissionId
	 * @return
	 * @throws NotFoundException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_ID, method = RequestMethod.GET)
	public @ResponseBody Submission getSubmission(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String submissionId) {
		return serviceProvider.getDataAccessService().getSubmission(userId, submissionId);
	}

	/**
	 * Fetch an access approval for a submission.If the user is an accessor in the submission they can fetch their own
	 * access approval information specific to a submission.
	 *
	 * @param userId
	 * @param submissionId
	 * @return
	 * @throws NotFoundException
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.USER_ACCESS_APPROVAL_FOR_SUBMISSION, method = RequestMethod.GET)
	public @ResponseBody AccessApproval getUserAccessApproval(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String submissionId) {
		return serviceProvider.getDataAccessService().getUserAccessApproval(userId, submissionId);
	}

	/**
	 * Retrieve a list of submissions for a given access requirement ID, where the calling user is an accessor.
	 *
	 * Allows to optionally filter by accessRequirement Ids, submission state and sort by the associated fields in the <a href="${org.sagebionetworks.repo.model.dataaccess.SubmissionSearchSort}">SubmissionSearchSort</a>.
	 *
	 *
	 * @param userId
	 * @param submissionSearchRequest
	 * @return
	 * @throws NotFoundException
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_USER_REQUESTS, method = RequestMethod.POST)
	public @ResponseBody UserSubmissionSearchResponse searchUserSubmissions(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@RequestBody UserSubmissionSearchRequest submissionSearchRequest ) throws NotFoundException {
		return serviceProvider.getDataAccessService().searchUserSubmissions(userId, submissionSearchRequest);
	}

	/**
	 * Get the data access submission associated with a given discussion thread.
	 *
	 * @param userId   - The ID of the user who is making the request.
	 * @param threadId - The ID of the thread.
	 * @return
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_SUBMISSION_THREAD, method = RequestMethod.GET)
	public @ResponseBody Submission getSubmissionForThread(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String threadId) throws NotFoundException {
		return serviceProvider.getDataAccessService().getSubmissionForThread(userId, threadId);
	}

	/**
	 * Preview the eDUC document for a data access request.
	 * Creates a draft envelope if one doesn't exist and returns the PDF as a file handle.
	 *
	 * @param userId    - The ID of the user who is making the request.
	 * @param requestId - The ID of the data access request.
	 * @return The file handle ID for the preview PDF.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST_ID_PREVIEW, method = RequestMethod.GET)
	public @ResponseBody EDucFileHandleId previewEDuc(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requestId) {
		return serviceProvider.getEDucService().previewEDuc(userId, requestId);
	}

	/**
	 * Route the eDUC associated with a data access request for electronic signature.
	 *
	 * @param userId    - The ID of the user who is making the request.
	 * @param requestId - The ID of the data access request.
	 * @return The signature quota information including remaining routings.
	 */
	@RequiredScope({view, modify})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST_ID_SIGNATURE, method = RequestMethod.POST)
	public @ResponseBody EDucSignatureQuota routeForSignature(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requestId) {
		return serviceProvider.getEDucService().routeForSignature(userId, requestId);
	}

	/**
	 * Get the status of a routed eDUC envelope.
	 *
	 * @param userId    - The ID of the user who is making the request.
	 * @param requestId - The ID of the data access request.
	 * @return The signature status of the envelope.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST_ID_SIGNATURE_STATUS, method = RequestMethod.GET)
	public @ResponseBody EDucSignatureStatus getSignatureStatus(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requestId) {
		return serviceProvider.getEDucService().getSignatureStatus(userId, requestId);
	}

	/**
	 * Cancel a routed eDUC envelope.
	 *
	 * @param userId    - The ID of the user who is making the request.
	 * @param requestId - The ID of the data access request.
	 */
	@RequiredScope({view, modify})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST_ID_SIGNATURE, method = RequestMethod.DELETE)
	public void cancelSignature(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requestId) {
		serviceProvider.getEDucService().cancelSignature(userId, requestId);
	}

	/**
	 * Get the file handle ID of the signed eDUC document.
	 *
	 * @param userId    - The ID of the user who is making the request.
	 * @param requestId - The ID of the data access request.
	 * @return The file handle ID for the signed PDF.
	 */
	@RequiredScope({view})
	@ResponseStatus(HttpStatus.OK)
	@RequestMapping(value = UrlHelpers.DATA_ACCESS_REQUEST_ID_SIGNATURE_FILE_HANDLE, method = RequestMethod.GET)
	public @ResponseBody EDucFileHandleId getSignedDocumentFileHandle(
			@RequestParam(value = AuthorizationConstants.USER_ID_PARAM) Long userId,
			@PathVariable String requestId) {
		return serviceProvider.getEDucService().getSignedDocumentFileHandle(userId, requestId);
	}

}
